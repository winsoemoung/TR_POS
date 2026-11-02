package com.example.offlinepos.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "suppliers",
    indices = [
        Index("name"),
        Index("shopName"),
        Index("phone"),
    ],
)
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val supplierId: Long = 0,
    val name: String, // Contact person or main name
    val shopName: String = "", // Shop, Company or Distribution name
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val taxNumber: String = "",
    val note: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)
