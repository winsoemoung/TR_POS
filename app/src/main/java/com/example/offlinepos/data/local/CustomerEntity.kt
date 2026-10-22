package com.example.offlinepos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index("phone"),
        Index("currentDebtMinor"),
    ]
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val customerId: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val creditLimitMinor: Long = 0L, // 0 = no credit limit set
    val currentDebtMinor: Long = 0L,
    val creditDueDateEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val notes: String = "",
)
