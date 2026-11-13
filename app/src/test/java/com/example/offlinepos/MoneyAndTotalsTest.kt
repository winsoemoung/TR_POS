package com.example.offlinepos

import com.example.offlinepos.domain.CartItem
import com.example.offlinepos.domain.Money
import com.example.offlinepos.domain.MoneyResult
import com.example.offlinepos.domain.TotalsResult
import com.example.offlinepos.domain.calculateTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyAndTotalsTest {
    @Test
    fun multiply_overflows_becomes_overflow_not_crash() {
        val result = Money.multiply(Long.MAX_VALUE, 2)
        assertTrue(result is MoneyResult.Overflow)
    }

    @Test
    fun totals_are_snapshot_based_and_exact() {
        val items = listOf(
            CartItem(productId = 1, name = "Tea", unitPriceMinor = 1_250, quantity = 3),
            CartItem(productId = 2, name = "Bread", unitPriceMinor = 2_000, quantity = 2, lineDiscountMinor = 500),
        )
        val totals = (calculateTotals(items) as TotalsResult.Ok).totals
        assertEquals(7_250, totals.subtotalMinor)
        assertEquals(500, totals.discountMinor)
        assertEquals(7_250, totals.totalMinor)
        assertEquals("7,250", Money.format(totals.totalMinor))
    }

    @Test
    fun zero_quantity_is_rejected() {
        val items = listOf(
            CartItem(productId = 1, name = "Tea", unitPriceMinor = 1_000, quantity = 0),
        )
        assertTrue(calculateTotals(items) is TotalsResult.Invalid)
    }

    @Test
    fun decimal_quantities_calculate_exact_rounded_totals() {
        val items = listOf(
            CartItem(productId = 10, name = "သံချောင်း (ပိဿာ)", unitPriceMinor = 10_000, quantity = 1, quantityDecimal = 0.25, unitName = "ပိဿာ"),
            CartItem(productId = 11, name = "သံကြိုး (ပိဿာ)", unitPriceMinor = 8_000, quantity = 1, quantityDecimal = 0.5, unitName = "ပိဿာ"),
            CartItem(productId = 12, name = "သွပ်နန်း", unitPriceMinor = 5_000, quantity = 2, quantityDecimal = 1.5, unitName = "ပိဿာ"),
        )
        val totals = (calculateTotals(items) as TotalsResult.Ok).totals
        assertEquals(2_500 + 4_000 + 7_500, totals.subtotalMinor)
        assertEquals(14_000, totals.totalMinor)
    }

    @Test
    fun format_qty_strips_trailing_zero_decimals() {
        assertEquals("1", Money.formatQty(1.0))
        assertEquals("0.25", Money.formatQty(0.25))
        assertEquals("0.5", Money.formatQty(0.5))
        assertEquals("1.75", Money.formatQty(1.75))
        assertEquals("100", Money.formatQty(100.0))
    }
}
