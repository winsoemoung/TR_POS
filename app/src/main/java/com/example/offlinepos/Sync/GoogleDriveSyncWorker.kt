package com.example.offlinepos.Sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.example.offlinepos.ui.StoreConfig

class GoogleDriveSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val account = StoreConfig(applicationContext).getGoogleAccount()?.trim().orEmpty()
        // 1. Two-way Conflict Resolution Auto-merge
        val recordResult = RecordLevelSyncManager(applicationContext).sync(account)
        // 2. Snapshot database backup with merged dataset
        val snapshotResult = GoogleDriveSyncManager(applicationContext).uploadLatestBackup(account)
        return if (recordResult.isSuccess && snapshotResult.isSuccess) {
            StoreConfig(applicationContext).setLastGoogleBackupTime(System.currentTimeMillis())
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        fun constraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
