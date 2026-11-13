package com.example.offlinepos.managers

import com.example.offlinepos.data.local.ProductEntity
import com.example.offlinepos.domain.CartItem
import com.example.offlinepos.domain.CartTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToLong

class CartManager {
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _cartTotals = MutableStateFlow(
        CartTotals(subtotalMinor = 0, discountMinor = 0, taxMinor = 0, totalMinor = 0, deliveryFeeMinor = 0)
    )
    val cartTotals: StateFlow<CartTotals> = _cartTotals.asStateFlow()

    private val _promoCode = MutableStateFlow<String?>(null)
    val promoCode: StateFlow<String?> = _promoCode.asStateFlow()

    private val _isVipMember = MutableStateFlow(false)
    val isVipMember: StateFlow<Boolean> = _isVipMember.asStateFlow()

    private val _vipCustomerName = MutableStateFlow<String?>(null)
    val vipCustomerName: StateFlow<String?> = _vipCustomerName.asStateFlow()

    fun setCart(items: List<CartItem>, taxRatePercent: Double = 0.0, deliveryFee: Long = 0L) {
        _cart.value = items
        recalculateTotals(items, taxRatePercent, deliveryFee)
    }

    fun addToCart(product: ProductEntity, taxRatePercent: Double, deliveryFee: Long) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == product.productId }
        val price = product.sellPriceMinor
        val singleTax = ((price * taxRatePercent) / 100.0).roundToLong()

        if (index >= 0) {
            val existing = currentList[index]
            val newEffQty = existing.effectiveQuantity + 1.0
            val newQtyInt = kotlin.math.ceil(newEffQty).toLong().coerceAtLeast(1L)
            val decQty = if (kotlin.math.abs(newEffQty - kotlin.math.round(newEffQty)) < 1e-6) null else newEffQty
            currentList[index] = existing.copy(
                quantity = newQtyInt,
                quantityDecimal = decQty,
                lineTaxMinor = ((price.toDouble() * newEffQty * taxRatePercent) / 100.0).roundToLong(),
            )
        } else {
            currentList.add(
                CartItem(
                    productId = product.productId,
                    name = product.name,
                    unitPriceMinor = price,
                    quantity = 1,
                    lineDiscountMinor = 0,
                    lineTaxMinor = singleTax,
                    unitName = product.unitName,
                    isWholesale = false,
                    costPriceMinor = product.costPriceMinor,
                    unitMultiplier = 1L,
                )
            )
        }
        _cart.value = currentList
        recalculateTotals(currentList, taxRatePercent, deliveryFee)
    }

    fun addWholesalePackToCart(product: ProductEntity, packs: Long, taxRatePercent: Double, deliveryFee: Long) {
        val currentList = _cart.value.toMutableList()
        val multiplier = if (product.wholesaleUnitMultiplier > 1L) product.wholesaleUnitMultiplier else 1L
        val wholesalePrice = if (product.wholesalePriceMinor > 0) product.wholesalePriceMinor else product.sellPriceMinor * multiplier
        val singleTax = ((wholesalePrice * taxRatePercent) / 100.0).roundToLong()

        val index = currentList.indexOfFirst { it.productId == product.productId && it.isWholesale }
        if (index >= 0) {
            val existing = currentList[index]
            val newPacks = existing.quantity + packs
            currentList[index] = existing.copy(
                quantity = newPacks,
                lineTaxMinor = singleTax * newPacks,
            )
        } else {
            currentList.add(
                CartItem(
                    productId = product.productId,
                    name = "${product.name} (လက်ကား)",
                    unitPriceMinor = wholesalePrice,
                    quantity = packs,
                    lineDiscountMinor = 0,
                    lineTaxMinor = singleTax * packs,
                    unitName = product.wholesaleUnitName,
                    isWholesale = true,
                    costPriceMinor = product.costPriceMinor * multiplier,
                    unitMultiplier = multiplier,
                )
            )
        }
        _cart.value = currentList
        recalculateTotals(currentList, taxRatePercent, deliveryFee)
    }

    fun toggleCartItemWholesale(productId: Long, allProducts: List<ProductEntity>, taxRatePercent: Double, deliveryFee: Long) {
        val product = allProducts.find { it.productId == productId } ?: return
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == productId }
        if (index < 0) return
        val item = currentList[index]

        if (item.isWholesale) {
            val retailPrice = product.sellPriceMinor
            val singleTax = ((retailPrice * taxRatePercent) / 100.0).roundToLong()
            val retailQty = maxOf(1L, item.quantity * item.unitMultiplier)
            currentList[index] = item.copy(
                name = product.name,
                unitPriceMinor = retailPrice,
                quantity = retailQty,
                unitName = product.unitName,
                isWholesale = false,
                costPriceMinor = product.costPriceMinor,
                unitMultiplier = 1L,
                lineTaxMinor = singleTax * retailQty,
            )
        } else {
            val multiplier = if (product.wholesaleUnitMultiplier > 1L) product.wholesaleUnitMultiplier else 1L
            val wholesalePrice = if (product.wholesalePriceMinor > 0) product.wholesalePriceMinor else product.sellPriceMinor * multiplier
            val packs = maxOf(1L, item.quantity / multiplier)
            val singleTax = ((wholesalePrice * taxRatePercent) / 100.0).roundToLong()
            currentList[index] = item.copy(
                name = "${product.name} (လက်ကား)",
                unitPriceMinor = wholesalePrice,
                quantity = packs,
                unitName = product.wholesaleUnitName,
                isWholesale = true,
                costPriceMinor = product.costPriceMinor * multiplier,
                unitMultiplier = multiplier,
                lineTaxMinor = singleTax * packs,
            )
        }
        _cart.value = currentList
        recalculateTotals(currentList, taxRatePercent, deliveryFee)
    }

    fun addRoofingSheetToCart(product: ProductEntity, lengthFeet: Long, sheetQty: Long, taxRatePercent: Double, deliveryFee: Long) {
        val currentList = _cart.value.toMutableList()
        val pricePerSheet = product.sellPriceMinor * lengthFeet
        val singleTax = ((pricePerSheet * taxRatePercent) / 100.0).roundToLong()
        val lineTax = singleTax * sheetQty
        val lineName = "${product.name} (${lengthFeet} ပေ x ${sheetQty} ချပ်)"

        currentList.add(
            CartItem(
                productId = product.productId,
                name = lineName,
                unitPriceMinor = pricePerSheet,
                quantity = sheetQty,
                lineDiscountMinor = 0,
                lineTaxMinor = lineTax,
                unitName = "ချပ်",
                costPriceMinor = product.costPriceMinor * lengthFeet,
            )
        )

        _cart.value = currentList
        recalculateTotals(currentList, taxRatePercent, deliveryFee)
    }

    fun updateQuantity(productId: Long, quantity: Long, allProducts: List<ProductEntity>, taxRatePercent: Double, deliveryFee: Long) {
        updateQuantity(productId, quantity.toDouble(), allProducts, taxRatePercent, deliveryFee)
    }

    fun updateQuantity(productId: Long, quantity: Double, allProducts: List<ProductEntity>, taxRatePercent: Double, deliveryFee: Long) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == productId }
        if (index >= 0) {
            if (!quantity.isFinite() || quantity <= 0.0) {
                currentList.removeAt(index)
            } else {
                val existing = currentList[index]
                val product = allProducts.find { it.productId == productId }
                val isWholesale = product != null && product.wholesaleMinQty > 0 && quantity >= product.wholesaleMinQty && product.wholesalePriceMinor > 0
                val effectivePrice = if (isWholesale) product!!.wholesalePriceMinor else (product?.sellPriceMinor ?: existing.unitPriceMinor)
                val newTax = ((effectivePrice.toDouble() * quantity * taxRatePercent) / 100.0).roundToLong()
                val intQty = kotlin.math.ceil(quantity).toLong().coerceAtLeast(1L)
                val decQty = if (kotlin.math.abs(quantity - kotlin.math.round(quantity)) < 1e-6) null else quantity
                val newGross = (effectivePrice.toDouble() * quantity).roundToLong()
                currentList[index] = existing.copy(
                    quantity = intQty,
                    quantityDecimal = decQty,
                    unitPriceMinor = effectivePrice,
                    lineTaxMinor = newTax,
                    lineDiscountMinor = existing.lineDiscountMinor.coerceIn(0L, newGross),
                    isWholesale = isWholesale,
                )
            }
            _cart.value = currentList
            recalculateTotals(currentList, taxRatePercent, deliveryFee)
        }
    }

    fun removeFromCart(productId: Long, taxRatePercent: Double, deliveryFee: Long) {
        val currentList = _cart.value.filter { it.productId != productId }
        _cart.value = currentList
        recalculateTotals(currentList, taxRatePercent, deliveryFee)
    }

    fun clearCart() {
        _cart.value = emptyList()
        _promoCode.value = null
        _isVipMember.value = false
        _vipCustomerName.value = null
        _cartTotals.value = CartTotals(0, 0, 0, 0, 0)
    }

    fun applyLineDiscount(productId: Long, discountMinor: Long, taxRatePercent: Double, deliveryFee: Long) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == productId }
        if (index >= 0) {
            val existing = currentList[index]
            val maxDiscount = existing.grossAmountMinor
            val safeDiscount = discountMinor.coerceIn(0L, maxDiscount)
            currentList[index] = existing.copy(lineDiscountMinor = safeDiscount)
            _cart.value = currentList
            recalculateTotals(currentList, taxRatePercent, deliveryFee)
        }
    }

    fun setPromoCode(code: String?, isVip: Boolean, vipName: String?, taxRatePercent: Double, deliveryFee: Long) {
        _promoCode.value = code
        _isVipMember.value = isVip
        _vipCustomerName.value = vipName
        recalculateTotals(_cart.value, taxRatePercent, deliveryFee)
    }

    fun recalculateTotals(items: List<CartItem>, taxRatePercent: Double, deliveryFeeMinor: Long) {
        var subtotal = 0L
        var totalLineDiscount = 0L
        var totalTax = 0L

        for (item in items) {
            val gross = item.grossAmountMinor
            val net = (gross - item.lineDiscountMinor).coerceAtLeast(0L)
            subtotal += net
            totalLineDiscount += item.lineDiscountMinor
            totalTax += item.lineTaxMinor
        }

        // Apply VIP / Promo 5% discount on remaining subtotal if active
        val promoDiscount = if (_isVipMember.value || _promoCode.value?.uppercase() == "787VIP") {
            (subtotal * 0.05).toLong()
        } else 0L

        val netSubtotal = (subtotal - promoDiscount).coerceAtLeast(0L)
        val grandTotal = (netSubtotal + totalTax + deliveryFeeMinor).coerceAtLeast(0L)

        _cartTotals.value = CartTotals(
            subtotalMinor = subtotal,
            discountMinor = totalLineDiscount + promoDiscount,
            taxMinor = totalTax,
            totalMinor = grandTotal,
            deliveryFeeMinor = deliveryFeeMinor,
        )
    }
}
