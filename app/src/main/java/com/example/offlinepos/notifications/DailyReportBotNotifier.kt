package com.example.offlinepos.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.offlinepos.MainActivity
import com.example.offlinepos.R
import com.example.offlinepos.ui.StoreConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DailyReportData(
    val storeName: String,
    val cashierName: String,
    val dateStr: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    val timeStr: String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
    val totalSalesMinor: Long,
    val totalProfitMinor: Long,
    val totalBillsCount: Int,
    val cashSalesMinor: Long,
    val digitalSalesMinor: Long,
    val creditSalesMinor: Long,
    val expensesMinor: Long,
    val expectedCashMinor: Long,
    val actualCashMinor: Long,
    val differenceMinor: Long,
    val lowStockItems: List<Pair<String, Long>> = emptyList(), // Name to Stock Qty
)

object DailyReportBotNotifier {

    private const val CHANNEL_ID = "pos_daily_reports_channel"
    private const val NOTIFICATION_ID = 8877

    fun formatReportHtml(report: DailyReportData): String {
        val lowStockSection = if (report.lowStockItems.isNotEmpty()) {
            val itemsList = report.lowStockItems.take(10).joinToString("\n") { (name, qty) ->
                "• ${name}: <b>${qty} ခု</b>"
            }
            "\n⚠️ <b>လက်ကျန်နည်းနေသော ပစ္စည်းများ (Low Stock):</b>\n$itemsList\n"
        } else {
            "\n✅ <i>ပစ္စည်းလက်ကျန် အားလုံး လုံလောက်မှုရှိပါသည်</i>\n"
        }

        val diffText = when {
            report.differenceMinor > 0 -> "ငွေပို: +%,d Ks".format(report.differenceMinor)
            report.differenceMinor < 0 -> "ငွေလို: -%,d Ks".format(-report.differenceMinor)
            else -> "ငွေကိုက်ပါသည် (±0 Ks)"
        }

        return """
📊 <b>[${report.storeName}] နေ့စဉ် အရောင်းနှင့် အမြတ်ငွေ အစီရင်ခံစာ</b>
📅 ရက်စွဲ: ${report.dateStr} (${report.timeStr})
👤 တာဝန်ကျ: ${report.cashierName}
━━━━━━━━━━━━━━━━━━━━━━
💰 <b>စုစုပေါင်း အရောင်း: %,d Ks</b>
📈 <b>စုစုပေါင်း အမြတ်ငွေ: %,d Ks</b>
🧾 <b>ဘေလ်စောင်ရေ: %d စောင်</b>
──────────────────────
💵 ငွေသားအရောင်း: %,d Ks
💳 ဒစ်ဂျစ်တယ် (KPay/Wave): %,d Ks
📝 အကြွေးအရောင်း: %,d Ks
💸 ဆိုင်တွင်း အသုံးစရိတ်: %,d Ks
──────────────────────
📥 အံဆွဲတွင်ရှိရမည့်ငွေ: %,d Ks
💵 လက်ဝယ်ရေတွက်ငွေ: %,d Ks
⚖️ အခြေအနေ: $diffText
──────────────────────$lowStockSection━━━━━━━━━━━━━━━━━━━━━━
✨ <i>TR-POS စနစ်မှ အလိုအလျောက် သတင်းပို့ပေးပါသည်။</i>
        """.trimIndent().format(
            report.totalSalesMinor,
            report.totalProfitMinor,
            report.totalBillsCount,
            report.cashSalesMinor,
            report.digitalSalesMinor,
            report.creditSalesMinor,
            report.expensesMinor,
            report.expectedCashMinor,
            report.actualCashMinor
        )
    }

    fun formatReportPlainText(report: DailyReportData): String {
        return formatReportHtml(report)
            .replace("<b>", "")
            .replace("</b>", "")
            .replace("<i>", "")
            .replace("</i>", "")
    }

    suspend fun sendTelegramMessage(botToken: String, chatId: String, htmlText: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanToken = botToken.trim()
        val cleanChatId = chatId.trim()
        if (cleanToken.isBlank() || cleanChatId.isBlank()) {
            return@withContext Pair(false, "Telegram Bot Token နှင့် Chat ID ထည့်သွင်းထားခြင်း မရှိပါ")
        }

        try {
            val url = URL("https://api.telegram.org/bot$cleanToken/sendMessage")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val payload = JSONObject().apply {
                put("chat_id", cleanChatId)
                put("text", htmlText)
                put("parse_mode", "HTML")
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseBody = try {
                if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                }
            } finally {
                conn.disconnect()
            }

            if (responseCode in 200..299) {
                Pair(true, "Telegram သို့ အောင်မြင်စွာ ပို့ပြီးပါပြီ")
            } else {
                val errorMsg = try { JSONObject(responseBody).optString("description", responseBody) } catch (_: Exception) { responseBody }
                Pair(false, "Telegram ပို့မရပါ (HTTP $responseCode): $errorMsg")
            }
        } catch (e: Exception) {
            Pair(false, "Telegram ချိတ်ဆက်မှု မအောင်မြင်ပါ: ${e.message}")
        }
    }

    suspend fun sendViberMessage(viberToken: String, receiverId: String, text: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanToken = viberToken.trim()
        val cleanReceiver = receiverId.trim()
        if (cleanToken.isBlank() || cleanReceiver.isBlank()) {
            return@withContext Pair(false, "Viber Bot Token နှင့် Receiver ID ထည့်သွင်းထားခြင်း မရှိပါ")
        }

        try {
            val url = URL("https://chatapi.viber.com/pa/send_message")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("X-Viber-Auth-Token", cleanToken)
                setRequestProperty("Accept", "application/json")
            }

            val payload = JSONObject().apply {
                put("receiver", cleanReceiver)
                put("min_api_version", 1)
                put("type", "text")
                put("text", text)
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            val responseBody = try {
                if (responseCode in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                }
            } finally {
                conn.disconnect()
            }

            if (responseCode in 200..299) {
                val json = try { JSONObject(responseBody) } catch (_: Exception) { null }
                val status = json?.optInt("status", -1) ?: 0
                if (status == 0) {
                    Pair(true, "Viber သို့ အောင်မြင်စွာ ပို့ပြီးပါပြီ")
                } else {
                    val statusMsg = json?.optString("status_message", "Error $status") ?: "Status $status"
                    Pair(false, "Viber အမှား: $statusMsg")
                }
            } else {
                Pair(false, "Viber ပို့မရပါ (HTTP $responseCode): $responseBody")
            }
        } catch (e: Exception) {
            Pair(false, "Viber ချိတ်ဆက်မှု မအောင်မြင်ပါ: ${e.message}")
        }
    }

    fun showSystemPushNotification(context: Context, report: DailyReportData) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Closing Reports",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily sales, profits, and low stock summaries"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val plainSummary = "အရောင်း: %,d Ks | အမြတ်: %,d Ks | ဘေလ်: %d စောင်".format(
            report.totalSalesMinor, report.totalProfitMinor, report.totalBillsCount
        )

        val bigText = formatReportPlainText(report)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("📊 နေ့စဉ်အရောင်း အစီရင်ခံစာ (${report.storeName})")
            .setContentText(plainSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    suspend fun dispatchDailyClosingReport(context: Context, report: DailyReportData): String = withContext(Dispatchers.IO) {
        val config = StoreConfig(context)
        val results = mutableListOf<String>()

        // 1. Always Trigger Android Push Notification
        try {
            showSystemPushNotification(context, report)
            results.add("📱 ဖုန်း Push Notification ပို့ပြီး")
        } catch (_: Exception) {}

        // 2. Telegram Bot
        if (config.isTelegramEnabled()) {
            val token = config.getTelegramBotToken().orEmpty()
            val chatId = config.getTelegramChatId().orEmpty()
            if (token.isNotBlank() && chatId.isNotBlank()) {
                val res = sendTelegramMessage(token, chatId, formatReportHtml(report))
                if (res.first) {
                    results.add("✓ Telegram Bot သို့ ပို့ဆောင်ပြီး")
                } else {
                    results.add("✗ ${res.second}")
                }
            }
        }

        // 3. Viber Bot
        if (config.isViberEnabled()) {
            val token = config.getViberBotToken().orEmpty()
            val receiver = config.getViberReceiverId().orEmpty()
            if (token.isNotBlank() && receiver.isNotBlank()) {
                val res = sendViberMessage(token, receiver, formatReportPlainText(report))
                if (res.first) {
                    results.add("✓ Viber သို့ ပို့ဆောင်ပြီး")
                } else {
                    results.add("✗ ${res.second}")
                }
            }
        }

        val summary = results.joinToString(" • ")
        val now = System.currentTimeMillis()
        config.setLastBotReportTime(now)
        config.setLastBotStatus(summary)
        summary
    }
}
