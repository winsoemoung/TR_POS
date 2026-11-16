package com.example.offlinepos.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationsDao {
    // Shifts / Z-Report
    @Query("SELECT * FROM shifts ORDER BY openTimeEpochMs DESC")
    fun observeAllShifts(): Flow<List<ShiftReportEntity>>

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openTimeEpochMs DESC LIMIT 1")
    suspend fun getActiveShift(): ShiftReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftReportEntity): Long

    @Query("SELECT * FROM shifts")
    suspend fun getAllShifts(): List<ShiftReportEntity>

    @Query("DELETE FROM shifts")
    suspend fun clearAllShifts()

    @Update
    suspend fun updateShift(shift: ShiftReportEntity): Int

    // Purchases / Stock-In
    @Query("SELECT * FROM purchases ORDER BY createdAtEpochMs DESC")
    fun observeAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases")
    suspend fun getAllPurchases(): List<PurchaseEntity>

    @Query("DELETE FROM purchases")
    suspend fun clearAllPurchases()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Query("SELECT * FROM stock_movements ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun observeStockMovements(limit: Int = 200): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    // Parked Orders (Hold Orders)
    @Query("SELECT * FROM parked_orders ORDER BY createdAtEpochMs DESC")
    fun observeParkedOrders(): Flow<List<ParkedOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParkedOrder(order: ParkedOrderEntity)

    @Query("DELETE FROM parked_orders WHERE parkId = :parkId")
    suspend fun deleteParkedOrder(parkId: String)

    @Query("DELETE FROM parked_orders")
    suspend fun clearAllParkedOrders()
}
