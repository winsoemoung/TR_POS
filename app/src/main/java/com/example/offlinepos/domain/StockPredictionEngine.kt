package com.example.offlinepos.domain

import com.example.offlinepos.data.local.ProductEntity
import com.example.offlinepos.data.local.SaleEntity
import com.example.offlinepos.data.local.SaleItemEntity
import kotlin.math.ceil

enum class StockRiskLevel(val labelBurmese: String, val colorHex: Long) {
    OUT_OF_STOCK("ပစ္စည်းပြတ်နေသည် (0 ခု)", 0xFFDC2626),
    CRITICAL("အရေးပေါ် (၃ ရက်အတွင်း ပြတ်မည်)", 0xFFEA580C),
    WARNING("သတိပေး (၇ ရက်အတွင်း ပြတ်မည်)", 0xFFD97706),
    HEALTHY("လက်ကျန်ပုံမှန်ရှိသည်", 0xFF16A34A),
}

data class StockPredictionItem(
    val product: ProductEntity,
    val currentStock: Long,
    val unitsSoldLast7Days: Long,
    val unitsSoldLast30Days: Long,
    val dailyRunRate: Double,
    val daysRemaining: Int,
    val riskLevel: StockRiskLevel,
    val recommendedRestockQty: Long,
    val recommendedRestockUnit: String,
    val recommendedPackCount: Long,
    val estimatedRestockCostMinor: Long,
    val urgencyScore: Double,
)

object StockPredictionEngine {
    fun calculatePredictions(
        products: List<ProductEntity>,
        sales: List<SaleEntity>,
        saleItems: List<SaleItemEntity>,
    ): List<StockPredictionItem> {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000L
        val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000L

        val saleTimeMap = sales.associate { it.saleId to it.createdAtEpochMs }
        val itemsByProduct = saleItems.groupBy { it.productId }

        return products.filter { it.isActive }.map { product ->
            val pItems = itemsByProduct[product.productId] ?: emptyList()

            var sold7d = 0L
            var sold30d = 0L

            for (item in pItems) {
                val saleTime = saleTimeMap[item.saleId] ?: 0L
                if (saleTime >= sevenDaysAgo) {
                    sold7d += item.quantity
                }
                if (saleTime >= thirtyDaysAgo) {
                    sold30d += item.quantity
                }
            }

            val dailyRunRate = when {
                sold7d > 0 -> sold7d / 7.0
                sold30d > 0 -> sold30d / 30.0
                else -> 0.0
            }

            val currentStock = product.stockQuantity
            val daysRemaining = when {
                currentStock <= 0 -> 0
                dailyRunRate > 0.0 -> (currentStock / dailyRunRate).toInt()
                else -> 999
            }

            val riskLevel = when {
                currentStock <= 0 -> StockRiskLevel.OUT_OF_STOCK
                dailyRunRate > 0.0 && daysRemaining <= 3 -> StockRiskLevel.CRITICAL
                dailyRunRate > 0.0 && daysRemaining <= 7 -> StockRiskLevel.WARNING
                currentStock <= product.lowStockThreshold -> StockRiskLevel.WARNING
                else -> StockRiskLevel.HEALTHY
            }

            val bufferDays = 14
            val targetStock = maxOf((dailyRunRate * bufferDays).toLong(), product.lowStockThreshold * 2, 10L)
            val neededUnits = maxOf(targetStock - currentStock, product.lowStockThreshold, 5L)

            val multiplier = maxOf(product.wholesaleUnitMultiplier, 1L)
            val hasPack = product.wholesaleUnitMultiplier > 1 && product.wholesaleUnitName.isNotBlank()
            val recommendedPackCount = if (hasPack) ceil(neededUnits.toDouble() / multiplier).toLong() else neededUnits
            val recommendedRestockQty = if (hasPack) recommendedPackCount * multiplier else neededUnits
            val recommendedUnitName = if (hasPack) product.wholesaleUnitName else product.unitName
            val unitCost = product.costPriceMinor
            val estimatedCost = recommendedRestockQty * unitCost

            val urgencyScore = when (riskLevel) {
                StockRiskLevel.OUT_OF_STOCK -> 1000.0 + dailyRunRate * 10
                StockRiskLevel.CRITICAL -> 500.0 + (3 - daysRemaining).coerceAtLeast(0) * 50 + dailyRunRate * 5
                StockRiskLevel.WARNING -> 200.0 + (7 - daysRemaining).coerceAtLeast(0) * 20 + dailyRunRate
                StockRiskLevel.HEALTHY -> 10.0
            }

            StockPredictionItem(
                product = product,
                currentStock = currentStock,
                unitsSoldLast7Days = sold7d,
                unitsSoldLast30Days = sold30d,
                dailyRunRate = dailyRunRate,
                daysRemaining = daysRemaining,
                riskLevel = riskLevel,
                recommendedRestockQty = recommendedRestockQty,
                recommendedRestockUnit = recommendedUnitName,
                recommendedPackCount = recommendedPackCount,
                estimatedRestockCostMinor = estimatedCost,
                urgencyScore = urgencyScore,
            )
        }.sortedByDescending { it.urgencyScore }
    }
}
