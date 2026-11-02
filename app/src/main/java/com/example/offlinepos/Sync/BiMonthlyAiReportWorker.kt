package com.example.offlinepos.Sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.offlinepos.R
import com.example.offlinepos.ai.BusinessIntelligenceEngine
import com.example.offlinepos.data.local.AppDatabase
import com.example.offlinepos.ui.StoreConfig
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Bi-Monthly AI Report Worker (DDD)
 * Runs automated business intelligence analysis on the 1st and 15th of the month.
 * Computes:
 * 1. Top-selling products
 * 2. Slow-moving / dead stock
 * 3. Smart restock recommendations
 * 4. Demand forecast and seasonal stocking guidance
 */
class BiMonthlyAiReportWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val calendar = Calendar.getInstance()
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            // Specifically trigger notification on 1st, 15th or if test run
            val isReportDay = dayOfMonth == 1 || dayOfMonth == 15

            val db = AppDatabase.get(applicationContext)
            val products = db.productDao().observeActive().first()
            val sales = db.checkoutDao().observeSales().first()
            val saleItems = db.checkoutDao().observeAllSaleItems().first()
            val purchases = db.operationsDao().observeAllPurchases().first()
            val returns = db.checkoutDao().observeAllReturnTransactions().first()
            val suppliers = db.supplierDao().getAllSuppliers().first()
            val profile = StoreConfig(applicationContext).getProfile()

            val report = BusinessIntelligenceEngine.generateBiMonthlyReport(
                products = products,
                sales = sales,
                saleItems = saleItems,
                purchases = purchases,
                returns = returns,
                suppliers = suppliers,
                calendar = calendar
            )

            if (isReportDay) {
                showNotification(
                    storeName = profile.storeName,
                    period = report.reportPeriod,
                    summary = "ထိပ်တန်းအရောင်း: ${report.topSellingItems.take(2).joinToString { it.productName }} | မှာယူရန်: ${report.restockRecommendations.size} မျိုး"
                )
            }

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(storeName: String, period: String, summary: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "bimonthly_bi_insights"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BI Insights & Seasonal Reports",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Automated 1st and 15th Business Intelligence reports"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📊 $storeName — $period သုံးသပ်ချက်")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("📊 $period စီးပွားရေးဆန်းစစ်ချက် အစီရင်ခံစာ ထွက်ရှိပါပြီ။\n$summary\nGemini Copilot တွင် အသေးစိတ် ကြည့်ရှုနိုင်ပါသည်။"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(7878, notification)
    }

    companion object {
        private const val WORK_NAME = "bi_monthly_ai_report_work"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val request = PeriodicWorkRequestBuilder<BiMonthlyAiReportWorker>(12, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
