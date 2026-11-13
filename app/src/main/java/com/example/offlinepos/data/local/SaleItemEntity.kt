package com.example.offlinepos.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sale_items",
    foreignKeys = [ForeignKey(
        entity = SaleEntity::class,
        parentColumns = ["saleId"],
        childColumns = ["saleId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("saleId"), Index("productId")],
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val lineId: Long = 0,
    val saleId: String,
    val productId: Long,
    val productNameSnapshot: String,
    val unitPriceMinor: Long,
    val quantity: Long,
    val lineDiscountMinor: Long,
    val lineTaxMinor: Long,
    val lineTotalMinor: Long,
    val unitName: String = "ခု",
    val returnedQuantity: Long = 0L,
    val quantityDecimal: Double = 0.0,
    val returnedQuantityDecimal: Double = 0.0,
)

fun SaleItemEntity.effectiveQuantity(): Double =
    if (quantityDecimal > 0.0) quantityDecimal else quantity.toDouble()

fun SaleItemEntity.effectiveReturnedQuantity(): Double =
    if (returnedQuantityDecimal > 0.0) returnedQuantityDecimal else returnedQuantity.toDouble()
