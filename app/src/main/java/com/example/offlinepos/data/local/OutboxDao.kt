package com.example.offlinepos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun enqueue(operation: OutboxEntity)

    @Query("SELECT * FROM sync_outbox WHERE state IN ('PENDING','RETRYABLE_FAILURE') AND nextAttemptAtEpochMs <= :now ORDER BY createdAtEpochMs LIMIT :limit")
    suspend fun loadReady(now: Long, limit: Int = 50): List<OutboxEntity>

    @Query("UPDATE sync_outbox SET state = :state, errorClass = :errorClass, lastError = :error, retryCount = :retryCount, nextAttemptAtEpochMs = :nextAttemptAt, updatedAtEpochMs = :now WHERE operationId = :operationId")
    suspend fun markResult(operationId: String, state: String, errorClass: String?, error: String?, retryCount: Int, nextAttemptAt: Long, now: Long): Int

    @Query("SELECT COUNT(*) FROM sync_outbox WHERE state IN ('PENDING','RETRYABLE_FAILURE','CONFLICT','QUARANTINED','PERMANENT_FAILURE')")
    fun observePendingCount(): Flow<Int>

    @Query("UPDATE sync_outbox SET state = 'ACKED', errorClass = NULL, lastError = NULL, updatedAtEpochMs = :now WHERE state IN ('PENDING','IN_FLIGHT','RETRYABLE_FAILURE','CONFLICT','QUARANTINED','PERMANENT_FAILURE')")
    suspend fun markPendingAsSynced(now: Long): Int

    @Query("UPDATE sync_outbox SET state = 'ACKED', errorClass = NULL, lastError = NULL, updatedAtEpochMs = :now WHERE aggregateId = :aggregateId AND state != 'ACKED'")
    suspend fun markAggregateAsSynced(aggregateId: String, now: Long): Int
}
