package com.example.offlinepos

import com.example.offlinepos.data.local.DebtTransactionEntity
import com.example.offlinepos.data.local.ReceiptTemplateEntity
import com.example.offlinepos.data.local.SaleEntity
import com.example.offlinepos.data.local.SaleItemEntity
import com.example.offlinepos.domain.CartItem
import com.example.offlinepos.domain.CartTotals
import com.example.offlinepos.hardware.CustomReceiptEscPosGenerator
import com.example.offlinepos.ui.ReceiptData
import com.example.offlinepos.ui.StoreProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptItemsResolutionTest {

    @Test
    fun receiptItemsFallback_resolvesFromAllSaleItemsWhenReceiptItemsEmpty() {
        val saleId = "sale-credit-001"
        val emptyReceipt = ReceiptData(
            saleId = saleId,
            businessDate = "2026-11-16",
            items = emptyList(),
            totals = CartTotals(subtotalMinor = 25000L, discountMinor = 0L, taxMinor = 0L, totalMinor = 25000L),
            paymentMethod = "CREDIT",
            tenderedMinor = 0L,
            changeMinor = 0L,
            timestampEpochMs = 1794700000000L,
            cashierId = "Admin",
            deviceId = "POS-01",
            customerName = "U Kyaw",
        )
        val allSaleItems = listOf(
            SaleItemEntity(
                lineId = 1L,
                saleId = saleId,
                productId = 10L,
                productNameSnapshot = "ဘိလပ်မြေ (Crown)",
                unitPriceMinor = 12500L,
                quantity = 2L,
                lineDiscountMinor = 0L,
                lineTaxMinor = 0L,
                lineTotalMinor = 25000L,
                unitName = "အိတ်",
            ),
            SaleItemEntity(
                lineId = 2L,
                saleId = "other-sale",
                productId = 11L,
                productNameSnapshot = "သံချောင်း",
                unitPriceMinor = 5000L,
                quantity = 1L,
                lineDiscountMinor = 0L,
                lineTaxMinor = 0L,
                lineTotalMinor = 5000L,
                unitName = "ပိဿာ",
            ),
        )

        val resolved = if (emptyReceipt.items.isNotEmpty()) {
            emptyReceipt.items
        } else {
            allSaleItems.filter { it.saleId == emptyReceipt.saleId }
        }
        assertEquals(1, resolved.size)
        assertEquals("ဘိလပ်မြေ (Crown)", resolved.first().productNameSnapshot)
        assertEquals(25000L, resolved.first().lineTotalMinor)
    }

    @Test
    fun creditLedgerTransaction_matchesPurchasedItemsBySaleIdAndFallback() {
        val now = 1794700000000L
        val creditSale = SaleEntity(
            saleId = "credit-sale-abc",
            businessDate = "2026-11-16",
            subtotalMinor = 18000L,
            discountMinor = 0L,
            taxMinor = 0L,
            totalMinor = 18000L,
            status = "COMPLETED",
            cashierId = "Admin",
            deviceId = "POS-01",
            createdAtEpochMs = now,
            paymentMethod = "CREDIT",
            customerId = 42L,
            customerName = "Daw Mya",
        )
        val itemsBySaleId = listOf(
            SaleItemEntity(
                lineId = 101L,
                saleId = "credit-sale-abc",
                productId = 1L,
                productNameSnapshot = "PVC ပိုက် ၁ လက်မ",
                unitPriceMinor = 4500L,
                quantity = 4L,
                lineDiscountMinor = 0L,
                lineTaxMinor = 0L,
                lineTotalMinor = 18000L,
                unitName = "ချောင်း",
            )
        ).groupBy { it.saleId }

        val txWithSaleId = DebtTransactionEntity(
            txId = 1L,
            customerId = 42L,
            saleId = "credit-sale-abc",
            type = "CREDIT_SALE",
            amountMinor = 18000L,
            remainingBalanceMinor = 18000L,
            note = "အကြွေးအရောင်း",
            cashierId = "Admin",
            createdAtEpochMs = now + 50L,
        )
        val txLegacyNoSaleId = txWithSaleId.copy(txId = 2L, saleId = null)

        val sales = listOf(creditSale)
        val salesById = sales.associateBy { it.saleId }

        fun resolveItems(tx: DebtTransactionEntity): List<SaleItemEntity> {
            val matchedSale = tx.saleId?.let { salesById[it] } ?: sales.firstOrNull { sale ->
                sale.customerId == 42L &&
                    sale.paymentMethod.equals("CREDIT", ignoreCase = true) &&
                    sale.totalMinor == tx.amountMinor &&
                    kotlin.math.abs(sale.createdAtEpochMs - tx.createdAtEpochMs) < 120_000L
            }
            val resolvedId = matchedSale?.saleId ?: tx.saleId
            return if (resolvedId != null) itemsBySaleId[resolvedId].orEmpty() else emptyList()
        }

        assertEquals(1, resolveItems(txWithSaleId).size)
        assertEquals(1, resolveItems(txLegacyNoSaleId).size)
        assertEquals("PVC ပိုက် ၁ လက်မ", resolveItems(txLegacyNoSaleId).first().productNameSnapshot)
    }

    @Test
    fun prePrintCartConversion_preservesDecimalQuantitiesAndTotals() {
        val cart = listOf(
            CartItem(
                productId = 7L,
                name = "သံချောင်း ၁၀ မူး",
                unitPriceMinor = 4000L,
                quantity = 1L,
                quantityDecimal = 0.5,
                lineDiscountMinor = 0L,
                lineTaxMinor = 0L,
                unitName = "ပိဿာ",
            )
        )
        val mapped = cart.map { item ->
            val effQty = item.effectiveQuantity
            SaleItemEntity(
                saleId = "TR-PREVIEW",
                productId = item.productId,
                productNameSnapshot = item.name,
                unitPriceMinor = item.unitPriceMinor,
                quantity = kotlin.math.ceil(effQty).toLong().coerceAtLeast(1L),
                lineDiscountMinor = item.lineDiscountMinor,
                lineTaxMinor = item.lineTaxMinor,
                lineTotalMinor = item.grossAmountMinor - item.lineDiscountMinor + item.lineTaxMinor,
                unitName = item.unitName,
                quantityDecimal = effQty,
            )
        }
        assertEquals(1, mapped.size)
        assertEquals(2000L, mapped.first().lineTotalMinor)
        assertEquals(0.5, mapped.first().quantityDecimal ?: 0.0, 0.0001)
    }

    @Test
    fun customReceiptPhysicalHeight_accountsForEmptyItemsFallback() {
        val profile = StoreProfile(
            storeName = "Test",
            address = "YGN",
            phone = "09123",
            headerNote = "Hardware",
            footerMessage = "ကျေးဇူးတင်ပါသည်",
            autoBackupSchedule = "DAILY",
        )
        val emptyReceipt = ReceiptData(
            saleId = "empty-1",
            businessDate = "2026-11-16",
            items = emptyList(),
            totals = CartTotals(0L, 0L, 0L, 0L),
            paymentMethod = "CREDIT",
            tenderedMinor = 0L,
            changeMinor = 0L,
            timestampEpochMs = 1794700000000L,
            cashierId = "Admin",
            deviceId = "POS-1",
        )
        val tmpl2Line = ReceiptTemplateEntity(templateId = 1, name = "T1", itemLayoutStyle = "STANDARD_2LINE")
        val tmpl4Cols = ReceiptTemplateEntity(templateId = 2, name = "T2", itemLayoutStyle = "COLUMNS_4")

        val h1 = CustomReceiptEscPosGenerator.calculatePhysicalHeightDots(tmpl2Line, emptyReceipt, profile)
        val h2 = CustomReceiptEscPosGenerator.calculatePhysicalHeightDots(tmpl4Cols, emptyReceipt, profile)
        assertTrue(h1 >= 100)
        assertTrue(h2 >= 100)
    }
}
