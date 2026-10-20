package com.example.offlinepos.Sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.offlinepos.ui.StoreConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real-time Google Drive Auto-Sync Coordinator.
 *
 * Requirements fulfilled:
 * 1. Background Coroutine: Triggered immediately whenever products are added, sales checked out,
 *    or financial records (debt, repayment, refunds, shifts) are altered.
 * 2. Offline-first Resilience: Enqueues a persistent WorkManager task with CONNECTED network
 *    constraint so that if offline or in bad network conditions, changes are queued and automatically
 *    pushed as soon as connection is restored.
 * 3. Two-Way Record Merge + Database Snapshot upload.
 */
object RealtimeDriveSyncCoordinator {

    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val hasPendingSync = AtomicBoolean(false)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private val _authRecoveryIntent = MutableStateFlow<android.content.Intent?>(null)
    val authRecoveryIntent: StateFlow<android.content.Intent?> = _authRecoveryIntent.asStateFlow()

    fun setAuthRecoveryIntent(intent: android.content.Intent?) {
        _authRecoveryIntent.value = intent
        _syncStatus.value = "⚠️ Google Drive ခွင့်ပြုချက် အတည်ပြုပေးရန် လိုအပ်ပါသည်"
    }

    fun clearAuthRecoveryIntent() {
        _authRecoveryIntent.value = null
    }

    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Trigger immediate Real-time Auto-Sync to Google Drive.
     * @param context Android context
     * @param reason Description of change (e.g. "Add Product", "Sale/Checkout", "Debt Repayment")
     */
    fun triggerSync(context: Context, reason: String = "Data Change") {
        val appContext = context.applicationContext
        val account = StoreConfig(appContext).getGoogleAccount()?.trim().orEmpty()
        if (account.isBlank()) {
            return
        }

        // 1. Offline-First Resilience: Always queue WorkManager with CONNECTED network constraints.
        // If offline or interrupted, WorkManager ensures reliable execution once network connects.
        runCatching {
            val workRequest = OneTimeWorkRequestBuilder<GoogleDriveSyncWorker>()
                .setConstraints(GoogleDriveSyncWorker.constraints())
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .addTag("google-drive-realtime-sync")
                .build()

            WorkManager.getInstance(appContext).enqueueUniqueWork(
                "google-drive-sync-queue",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        // 2. Immediate Background Coroutine Execution if online
        coordinatorScope.launch {
            hasPendingSync.set(true)

            if (!isNetworkAvailable(appContext)) {
                _syncStatus.value = "⏳ အော့ဖ်လိုင်းဖြစ်နေပါသည် (အင်တာနက်ရပါက Drive သို့ အလိုအလျောက် ပို့ပါမည်)"
                return@launch
            }

            if (syncMutex.isLocked) {
                // Another sync is currently in progress; hasPendingSync flag will trigger another pass
                return@launch
            }

            syncMutex.withLock {
                while (hasPendingSync.getAndSet(false)) {
                    _isSyncing.value = true
                    _syncStatus.value = "🔄 Google Drive သို့ Auto-sync လုပ်နေပါသည် ($reason)..."
                    val driveManager = GoogleDriveSyncManager(appContext)
                    val recordManager = RecordLevelSyncManager(appContext)

                    try {
                        // 1. Two-Way Conflict Resolution Auto-Merge (pulls remote changes, resolves conflicts, updates SQLite, pushes merged state)
                        val recordResult = recordManager.sync(account)
                        // 2. Upload SQLite snapshot containing the fully merged dataset
                        val snapshotResult = driveManager.uploadLatestBackup(account)

                        if (recordResult.isSuccess && snapshotResult.isSuccess) {
                            val now = System.currentTimeMillis()
                            StoreConfig(appContext).setLastGoogleBackupTime(now)
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now))
                            val prefs = appContext.getSharedPreferences("pos_sync_status", Context.MODE_PRIVATE)
                            val resolved = prefs.getInt("last_conflicts_resolved", 0)
                            if (resolved > 0) {
                                _syncStatus.value = "✓ Two-way Auto-Merge & Sync အောင်မြင်ပါသည် (Conflict $resolved ခု ညှိနှိုင်းပြီး - $timeStr)"
                            } else {
                                _syncStatus.value = "✓ Google Drive Auto-sync အောင်မြင်ပါသည် ($timeStr)"
                            }
                        } else {
                            val ex = recordResult.exceptionOrNull() ?: snapshotResult.exceptionOrNull()
                            when {
                                ex is com.google.android.gms.auth.UserRecoverableAuthException -> {
                                    setAuthRecoveryIntent(ex.intent)
                                    _syncStatus.value = "⚠️ Google Drive ခွင့်ပြုချက် အတည်ပြုပေးရန် လိုအပ်ပါသည် (နှိပ်၍ အတည်ပြုပါ)"
                                }
                                ex is java.net.UnknownHostException || ex is java.net.SocketTimeoutException -> {
                                    _syncStatus.value = "⏳ အင်တာနက် လိုင်းမငြိမ်ပါ (Queue တွင် သိမ်းဆည်းထားပါသည်)"
                                }
                                ex != null && ex.message?.contains("401") == true -> {
                                    _syncStatus.value = "⚠️ Google OAuth Token သက်တမ်းကုန်နေပါသည် (Settings တွင် ပြန်လည် ချိတ်ဆက်ပါ)"
                                }
                                ex != null && (ex.message?.contains("403") == true || ex.message?.contains("Drive API") == true) -> {
                                    _syncStatus.value = "⚠️ Google Cloud တွင် Google Drive API ဖွင့်ထားရန် လိုအပ်ပါသည် (HTTP 403)"
                                }
                                else -> {
                                    val err = ex?.localizedMessage ?: "Sync မအောင်မြင်ပါ"
                                    _syncStatus.value = "⚠️ $err (Queue တွင် သိမ်းဆည်းထားပါသည်)"
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (e is com.google.android.gms.auth.UserRecoverableAuthException) {
                            setAuthRecoveryIntent(e.intent)
                            _syncStatus.value = "⚠️ Google Drive ခွင့်ပြုချက် အတည်ပြုပေးရန် လိုအပ်ပါသည်"
                        } else if (e is java.net.UnknownHostException || e is java.net.SocketTimeoutException) {
                            _syncStatus.value = "⏳ အင်တာနက် လိုင်းမငြိမ်ပါ (Queue တွင် သိမ်းဆည်းထားပါသည်)"
                        } else {
                            val msg = e.localizedMessage ?: e.message ?: "စစ်ဆေးရန်လိုပါသည်"
                            _syncStatus.value = "⚠️ Drive Sync: $msg (Queue တွင် သိမ်းဆည်းထားပါသည်)"
                        }
                    } finally {
                        _isSyncing.value = false
                    }
                }
            }
        }
    }
}
