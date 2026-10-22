package com.example.offlinepos.managers

import android.content.Context
import com.example.offlinepos.Sync.RecordLevelSyncManager
import com.example.offlinepos.data.local.AppDatabase
import com.example.offlinepos.data.local.OperationsDao
import com.example.offlinepos.data.local.ProductDao
import com.example.offlinepos.data.local.ProductEntity
import com.example.offlinepos.data.local.PurchaseEntity
import com.example.offlinepos.data.local.StockMovementEntity
import com.example.offlinepos.domain.ProductDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class InventoryManager(
    private val productDao: ProductDao,
    private val operationsDao: OperationsDao,
    private val db: AppDatabase,
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onTriggerSync: (String) -> Unit,
    private val onBannerMessage: (String) -> Unit,
) {
    fun addNewProduct(
        draft: ProductDraft,
        sku: String,
        onResult: (Boolean, String) -> Unit,
    ) {
        val errors = draft.validate()
        if (errors.isNotEmpty()) {
            onResult(false, errors.first())
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val assignedSku = sku.ifBlank { "PROD-${now % 10000}" }
                val newEntity = ProductEntity(
                    name = draft.name,
                    sku = assignedSku,
                    barcode = draft.barcode,
                    costPriceMinor = draft.costPriceMinor,
                    sellPriceMinor = draft.sellPriceMinor,
                    stockQuantity = draft.stockQuantity,
                    category = draft.category.ifBlank { "အထွေထွေ" },
                    isRoofingSheet = draft.isRoofingSheet,
                    lowStockThreshold = draft.lowStockThreshold,
                    imageUri = draft.imageUri,
                    unitName = draft.unitName.ifBlank { "ခု" },
                    wholesalePriceMinor = draft.wholesalePriceMinor,
                    wholesaleMinQty = draft.wholesaleMinQty,
                    wholesaleUnitName = draft.wholesaleUnitName.ifBlank { "ဖာ" },
                    wholesaleUnitMultiplier = maxOf(draft.wholesaleUnitMultiplier, 1L),
                    updatedAtEpochMs = now,
                )
                productDao.insert(newEntity)
                withContext(Dispatchers.Main) {
                    onTriggerSync("Add Product (${draft.name})")
                    onResult(true, "ကုန်ပစ္စည်း အသစ်ကို အောင်မြင်စွာ ထည့်သွင်းပြီးပါပြီ")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "ကုန်ပစ္စည်းထည့်သွင်းခြင်း မအောင်မြင်ပါ")
                }
            }
        }
    }

    fun adjustStock(productId: Long, adjustment: Long) {
        coroutineScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val before = productDao.findById(productId)?.stockQuantity ?: return@launch
            if (productDao.adjustStock(productId, adjustment, now) == 1) {
                operationsDao.insertStockMovement(
                    StockMovementEntity(
                        movementId = UUID.randomUUID().toString(),
                        productId = productId,
                        movementType = "ADJUSTMENT",
                        quantityChange = adjustment,
                        quantityBefore = before,
                        quantityAfter = before + adjustment,
                        reason = "Manual inventory adjustment",
                        createdAtEpochMs = now,
                    )
                )
                withContext(Dispatchers.Main) {
                    onTriggerSync("Stock Adjustment")
                    onBannerMessage("လက်ကျန်စာရင်း ပြင်ဆင်ပြီးပါပြီ")
                }
            }
        }
    }

    fun archiveProduct(productId: Long) {
        coroutineScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existing = productDao.findById(productId) ?: return@launch
            productDao.update(existing.copy(isActive = false, updatedAtEpochMs = now))
            RecordLevelSyncManager(context.applicationContext).recordTombstone(db, "products", productId.toString())
            withContext(Dispatchers.Main) {
                onTriggerSync("Archive Product")
                onBannerMessage("ကုန်ပစ္စည်းကို စာရင်းမှ ဖယ်ရှားပြီးပါပြီ")
            }
        }
    }

    fun activateProduct(productId: Long) {
        coroutineScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existing = productDao.findById(productId) ?: return@launch
            productDao.update(existing.copy(isActive = true, updatedAtEpochMs = now))
            withContext(Dispatchers.Main) {
                onTriggerSync("Activate Product")
                onBannerMessage("ကုန်ပစ္စည်းကို ပြန်လည်အသုံးပြုနိုင်ပါပြီ")
            }
        }
    }

    fun recordStockInPurchase(
        productId: Long,
        productName: String,
        supplierName: String,
        quantity: Long,
        unitCost: Long,
        invoiceRef: String,
        note: String,
        updateMasterCost: Boolean = true,
        onResult: (Boolean, String) -> Unit,
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val totalCost = quantity * unitCost
                val purchase = PurchaseEntity(
                    productId = productId,
                    productName = productName,
                    supplierName = supplierName,
                    quantity = quantity,
                    unitCostMinor = unitCost,
                    totalCostMinor = totalCost,
                    invoiceRef = invoiceRef,
                    note = note,
                    createdAtEpochMs = System.currentTimeMillis()
                )
                operationsDao.insertPurchase(purchase)
                val now = System.currentTimeMillis()
                productDao.adjustStock(productId, quantity, now)
                if (updateMasterCost && unitCost > 0) {
                    productDao.updateCostPrice(productId, unitCost, now)
                }

                withContext(Dispatchers.Main) {
                    onTriggerSync("Purchase/Stock In ($productName)")
                    onResult(true, "'$productName' အဝင် (+$quantity) စာရင်းသွင်းပြီးပါပြီ")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "ပစ္စည်းအဝင်သွင်းခြင်း မအောင်မြင်ပါ: ${e.message}")
                }
            }
        }
    }
}
