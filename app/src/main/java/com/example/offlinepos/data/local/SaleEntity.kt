package com.example.offlinepos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object SaleStatus {
    const val COMPLETED = "COMPLETED"
    const val VOIDED = "VOIDED"
    const val PARTIALLY_REFUNDED = "PARTIALLY_REFUNDED"
    const val REFUNDED = "REFUNDED"
}

object PickupStatus {
    const val PICKED_UP = "PICKED_UP"
    const val PENDING_PICKUP = "PENDING_PICKUP"
}

@Entity(
    tableName = "sales",
    indices = [
        Index("createdAtEpochMs"),
        Index("businessDate"),
        Index("customerId"),
        Index("status"),
        Index("pickupStatus"),
    ]
)
data class SaleEntity(
    @PrimaryKey val saleId: String,
    val businessDate: String,
    val subtotalMinor: Long,
    val discountMinor: Long,
    val taxMinor: Long,
    val totalMinor: Long,
    val status: String,
    val cashierId: String,
    val deviceId: String,
    val createdAtEpochMs: Long,
    val customerId: Long? = null,
    val customerName: String? = null,
    val paymentMethod: String = "CASH",
    val splitCashMinor: Long = 0L,
    val splitDigitalMinor: Long = 0L,
    val deliveryFeeMinor: Long = 0L,
    val pickupStatus: String = PickupStatus.PICKED_UP,
    val expectedPickupDate: String? = null,
    val pickupNote: String? = null,
)
