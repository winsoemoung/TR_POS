package com.example.offlinepos.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

sealed interface MoneyResult {
    data class Ok(val value: Long) : MoneyResult
    data class Overflow(val detail: String) : MoneyResult
}

object Money {
    fun add(a: Long, b: Long): MoneyResult = guarded("add($a, $b)") { Math.addExact(a, b) }
    fun subtract(a: Long, b: Long): MoneyResult = guarded("subtract($a, $b)") { Math.subtractExact(a, b) }
    fun multiply(a: Long, b: Long): MoneyResult = guarded("multiply($a, $b)") { Math.multiplyExact(a, b) }
    fun format(minor: Long): String = "%,d".format(minor)

    fun formatQty(qty: Double): String {
        if (!qty.isFinite()) return "0"
        val rounded = qty.roundToLong()
        if (abs(qty - rounded.toDouble()) < 1e-6) {
            return rounded.toString()
        }
        val formatted = String.format(Locale.US, "%.3f", qty)
            .trimEnd('0')
            .trimEnd('.')
        return if (formatted == "-0") "0" else formatted
    }

    fun calculateLineGross(
        unitPriceMinor: Long,
        quantity: Long,
        quantityDecimal: Double? = null,
    ): MoneyResult {
        if (quantityDecimal == null) {
            return multiply(unitPriceMinor, quantity)
        }
        val raw = unitPriceMinor.toDouble() * quantityDecimal
        if (!raw.isFinite() || raw > Long.MAX_VALUE.toDouble() || raw < Long.MIN_VALUE.toDouble()) {
            return MoneyResult.Overflow("multiply($unitPriceMinor, $quantityDecimal)")
        }
        return MoneyResult.Ok(raw.roundToLong())
    }

    private inline fun guarded(detail: String, block: () -> Long): MoneyResult = try {
        MoneyResult.Ok(block())
    } catch (e: ArithmeticException) {
        MoneyResult.Overflow("$detail -> ${e.message}")
    }
}

data class CartItem(
    val productId: Long,
    val name: String,
    val unitPriceMinor: Long,
    val quantity: Long = 1L,
    val lineDiscountMinor: Long = 0,
    val lineTaxMinor: Long = 0,
    val unitName: String = "ခု",
    val isWholesale: Boolean = false,
    val costPriceMinor: Long = 0L,
    val unitMultiplier: Long = 1L,
    val quantityDecimal: Double? = null,
) {
    val effectiveQuantity: Double
        get() = quantityDecimal ?: quantity.toDouble()

    val grossAmountMinor: Long
        get() = when (val res = Money.calculateLineGross(unitPriceMinor, quantity, quantityDecimal)) {
            is MoneyResult.Ok -> res.value
            is MoneyResult.Overflow -> 0L
        }
}

data class CartTotals(
    val subtotalMinor: Long,
    val discountMinor: Long,
    val taxMinor: Long,
    val totalMinor: Long,
    val deliveryFeeMinor: Long = 0L,
)

sealed interface TotalsResult {
    data class Ok(val totals: CartTotals) : TotalsResult
    data class Invalid(val reason: String, val productId: Long?) : TotalsResult
}

fun calculateTotals(items: List<CartItem>): TotalsResult {
    if (items.isEmpty()) return TotalsResult.Invalid("Cart is empty", null)
    var subtotal = 0L
    var discount = 0L
    var tax = 0L
    for (item in items) {
        val effQty = item.effectiveQuantity
        if (!effQty.isFinite() || effQty <= 0.0) {
            return TotalsResult.Invalid("Quantity must be positive", item.productId)
        }
        val gross = (Money.calculateLineGross(item.unitPriceMinor, item.quantity, item.quantityDecimal) as? MoneyResult.Ok)?.value
            ?: return TotalsResult.Invalid("Line amount overflow", item.productId)
        val net = (Money.subtract(gross, item.lineDiscountMinor) as? MoneyResult.Ok)?.value
            ?: return TotalsResult.Invalid("Line discount overflow", item.productId)
        if (net < 0) return TotalsResult.Invalid("Discount exceeds line amount", item.productId)
        subtotal = (Money.add(subtotal, net) as? MoneyResult.Ok)?.value
            ?: return TotalsResult.Invalid("Subtotal overflow", item.productId)
        discount = (Money.add(discount, item.lineDiscountMinor) as? MoneyResult.Ok)?.value
            ?: return TotalsResult.Invalid("Discount overflow", item.productId)
        tax = (Money.add(tax, item.lineTaxMinor) as? MoneyResult.Ok)?.value
            ?: return TotalsResult.Invalid("Tax overflow", item.productId)
    }
    val total = (Money.add(subtotal, tax) as? MoneyResult.Ok)?.value
        ?: return TotalsResult.Invalid("Total overflow", null)
    return TotalsResult.Ok(CartTotals(subtotal, discount, tax, total))
}
