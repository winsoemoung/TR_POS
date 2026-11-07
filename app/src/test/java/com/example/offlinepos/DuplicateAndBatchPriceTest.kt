package com.example.offlinepos

import com.example.offlinepos.ai.AiActionType
import com.example.offlinepos.data.local.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.round

class DuplicateAndBatchPriceTest {

    @Test
    fun testDuplicateDetectionByNameAndSku() {
        val existing = listOf(
            ProductEntity(
                productId = 1,
                name = "Crown ဘိလပ်မြေ",
                sku = "CEMENT-001",
                barcode = null,
                costPriceMinor = 8000,
                sellPriceMinor = 9500,
                stockQuantity = 50,
                isActive = true,
                updatedAtEpochMs = 1000L
            ),
            ProductEntity(
                productId = 2,
                name = "PVC ပိုက် 4 လက်မ",
                sku = "PVC-004",
                barcode = null,
                costPriceMinor = 12000,
                sellPriceMinor = 15000,
                stockQuantity = 30,
                isActive = true,
                updatedAtEpochMs = 1000L
            )
        )

        // Test name matching case-insensitively and trimmed
        val inputName = "  crown ဘိလပ်မြေ  "
        val matchedByName = existing.firstOrNull { it.isActive && it.name.trim().equals(inputName.trim(), ignoreCase = true) }
        assertNotNull(matchedByName)
        assertEquals(1L, matchedByName?.productId)

        // Test SKU matching
        val inputSku = "pvc-004"
        val matchedBySku = existing.firstOrNull { it.isActive && it.sku.trim().equals(inputSku.trim(), ignoreCase = true) }
        assertNotNull(matchedBySku)
        assertEquals(2L, matchedBySku?.productId)
    }

    @Test
    fun testBatchPriceCalculationByPercent() {
        val originalPrice = 20000L
        val percent = 5.0
        val multiplier = 1.0 + (percent / 100.0)
        val newPrice = round(originalPrice * multiplier).toLong()
        assertEquals(21000L, newPrice)

        val price2 = 15000L
        val percent2 = 10.0
        val newPrice2 = round(price2 * (1.0 + percent2 / 100.0)).toLong()
        assertEquals(16500L, newPrice2)
    }

    @Test
    fun testBatchAdjustPriceActionModel() {
        val actionType = AiActionType.BatchAdjustPriceByPercent(
            targetKeywordOrCategory = "pvc",
            percentIncrease = 5.0,
            matchedProductCount = 3,
            affectedProductIds = listOf(1L, 2L, 3L),
            sampleAdjustments = listOf("PVC 1' -> 21,000 Ks")
        )

        assertEquals("pvc", actionType.targetKeywordOrCategory)
        assertEquals(5.0, actionType.percentIncrease, 0.001)
        assertEquals(3, actionType.matchedProductCount)
        assertEquals(1, actionType.sampleAdjustments.size)
    }
}
