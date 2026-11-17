package com.example.offlinepos.firebase

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import com.example.offlinepos.data.local.AppDatabase
import com.example.offlinepos.ui.StoreConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class FirebaseRealtimeSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (Firebase.auth.currentUser == null) {
            return Result.success()
        }
        val db = AppDatabase.get(applicationContext)
        val storeConfig = StoreConfig(applicationContext)
        val storeName = storeConfig.getProfile().storeName
        val res = FirebaseFirestoreSyncManager.syncAll(
            context = applicationContext,
            db = db,
            storeName = storeName,
            storeConfig = storeConfig,
        )
        return if (res.success) Result.success() else Result.retry()
    }

    companion object {
        fun constraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
