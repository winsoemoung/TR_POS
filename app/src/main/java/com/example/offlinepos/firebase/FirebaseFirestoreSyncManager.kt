package com.example.offlinepos.firebase

import android.content.Context
import android.util.Log
import com.example.offlinepos.R
import com.example.offlinepos.data.local.AppDatabase
import com.example.offlinepos.data.local.CustomerEntity
import com.example.offlinepos.data.local.ProductEntity
import com.example.offlinepos.data.local.SaleEntity
import com.example.offlinepos.domain.VoucherHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FirebaseSyncResult(
    val success: Boolean,
    val message: String,
    val productsSynced: Int = 0,
    val salesSynced: Int = 0,
    val customersSynced: Int = 0,
)

object FirebaseFirestoreSyncManager {
    private const val TAG = "FirestoreSyncManager"

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimeEpochMs = MutableStateFlow(0L)
    val lastSyncTimeEpochMs: StateFlow<Long> = _lastSyncTimeEpochMs.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>("Cloud Sync အသင့်ရှိပါသည်")
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    private fun getFirestore(context: Context): FirebaseFirestore {
        val databaseId = try {
            context.getString(R.string.firestore_database_id)
        } catch (e: Exception) {
            Log.e(TAG, "firestore_database_id resource missing", e)
            ""
        }
        return if (databaseId.isNotBlank()) {
            FirebaseFirestore.getInstance(databaseId)
        } else {
            FirebaseFirestore.getInstance()
        }
    }

    suspend fun syncAll(
        context: Context,
        db: AppDatabase,
        storeName: String = "TR-POS"
    ): FirebaseSyncResult {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            val msg = "Google Account Login မဝင်ရသေးပါ (Please sign in with Google)"
            _syncStatus.value = msg
            return FirebaseSyncResult(false, msg)
        }

        _isSyncing.value = true
        _syncStatus.value = "Cloud Firestore သို့ ဒေတာများ Sync ပြုလုပ်နေပါသည်..."

        return try {
            val firestore = getFirestore(context)
            val uid = user.uid

            // 1. Update user store profile document
            val userDocRef = firestore.collection("users").document(uid)
            val userProfile = hashMapOf(
                "userId" to uid,
                "email" to (user.email ?: ""),
                "displayName" to (user.displayName ?: "Store Owner"),
                "storeName" to storeName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userDocRef.set(userProfile, SetOptions.merge()).await()

            // 2. Up-sync local Products to Cloud
            val localProducts = db.productDao().getAll()
            var prodCount = 0
            for (p in localProducts) {
                val pId = p.productId.toString()
                val prodRef = firestore.collection("users").document(uid).collection("products").document(pId)
                val pData = hashMapOf<String, Any>(
                    "id" to pId,
                    "userId" to uid,
                    "name" to p.name,
                    "sku" to p.sku,
                    "category" to p.category,
                    "costPriceMinor" to p.costPriceMinor,
                    "sellPriceMinor" to p.sellPriceMinor,
                    "stockQuantity" to p.stockQuantity,
                    "lowStockThreshold" to p.lowStockThreshold,
                    "isActive" to p.isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!p.barcode.isNullOrBlank()) {
                    pData["barcode"] = p.barcode
                }
                prodRef.set(pData, SetOptions.merge()).await()
                prodCount++
            }

            // Down-sync remote Products
            val remoteProdsSnapshot = firestore.collection("users").document(uid).collection("products").get().await()
            val localProdIds = localProducts.map { it.productId.toString() }.toSet()
            for (doc in remoteProdsSnapshot.documents) {
                val idStr = doc.getString("id") ?: doc.id
                if (!localProdIds.contains(idStr)) {
                    val idLong = idStr.toLongOrNull() ?: System.currentTimeMillis()
                    val newProd = ProductEntity(
                        productId = idLong,
                        name = doc.getString("name") ?: "Unnamed",
                        sku = doc.getString("sku") ?: "SKU-$idLong",
                        barcode = doc.getString("barcode"),
                        category = doc.getString("category") ?: "အထွေထွေ",
                        costPriceMinor = doc.getLong("costPriceMinor") ?: 0L,
                        sellPriceMinor = doc.getLong("sellPriceMinor") ?: 0L,
                        stockQuantity = doc.getLong("stockQuantity") ?: 0L,
                        lowStockThreshold = doc.getLong("lowStockThreshold") ?: 3L,
                        isActive = doc.getBoolean("isActive") ?: true,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                    db.productDao().upsert(newProd)
                }
            }

            // 3. Up-sync local Sales to Cloud
            val localSales = db.checkoutDao().getAllSales()
            var saleCount = 0
            for (s in localSales) {
                val sRef = firestore.collection("users").document(uid).collection("sales").document(s.saleId)
                val sData = hashMapOf<String, Any>(
                    "id" to s.saleId,
                    "userId" to uid,
                    "totalMinor" to s.totalMinor,
                    "subtotalMinor" to s.subtotalMinor,
                    "discountMinor" to s.discountMinor,
                    "taxMinor" to s.taxMinor,
                    "paymentMethod" to s.paymentMethod,
                    "businessDate" to s.businessDate,
                    "status" to s.status,
                    "voucherNo" to VoucherHelper.formatVoucherNumberFallback(s.saleId, s.createdAtEpochMs),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!s.customerName.isNullOrBlank()) {
                    sData["customerName"] = s.customerName
                }
                sRef.set(sData, SetOptions.merge()).await()
                saleCount++
            }

            // 4. Up-sync local Customers to Cloud
            val localCustomers = db.customerDao().getAllCustomers()
            var custCount = 0
            for (c in localCustomers) {
                val cId = c.customerId.toString()
                val cRef = firestore.collection("users").document(uid).collection("customers").document(cId)
                val cData = hashMapOf<String, Any>(
                    "id" to cId,
                    "userId" to uid,
                    "name" to c.name,
                    "phone" to c.phone,
                    "address" to c.address,
                    "currentDebtMinor" to c.currentDebtMinor,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                cRef.set(cData, SetOptions.merge()).await()
                custCount++
            }

            val now = System.currentTimeMillis()
            _lastSyncTimeEpochMs.value = now
            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
            val msg = "Cloud Firestore နှင့် အောင်မြင်စွာ Sync လုပ်ပြီးပါပြီ ($timeStr)"
            _syncStatus.value = msg
            _isSyncing.value = false

            FirebaseSyncResult(
                success = true,
                message = msg,
                productsSynced = prodCount,
                salesSynced = saleCount,
                customersSynced = custCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _isSyncing.value = false
            val errorMsg = "Firestore Sync မအောင်မြင်ပါ: ${e.localizedMessage ?: e.message}"
            _syncStatus.value = errorMsg
            FirebaseSyncResult(false, errorMsg)
        }
    }

    fun syncProductAsync(context: Context, product: ProductEntity, scope: CoroutineScope) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = getFirestore(context)
                val uid = user.uid
                val pId = product.productId.toString()
                val prodRef = firestore.collection("users").document(uid).collection("products").document(pId)
                val pData = hashMapOf<String, Any>(
                    "id" to pId,
                    "userId" to uid,
                    "name" to product.name,
                    "sku" to product.sku,
                    "category" to product.category,
                    "costPriceMinor" to product.costPriceMinor,
                    "sellPriceMinor" to product.sellPriceMinor,
                    "stockQuantity" to product.stockQuantity,
                    "lowStockThreshold" to product.lowStockThreshold,
                    "isActive" to product.isActive,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!product.barcode.isNullOrBlank()) {
                    pData["barcode"] = product.barcode
                }
                prodRef.set(pData, SetOptions.merge()).await()
                Log.d(TAG, "Auto-synced product ${product.name} to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to auto-sync product ${product.name}", e)
            }
        }
    }

    fun syncSaleAsync(context: Context, sale: SaleEntity, scope: CoroutineScope) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = getFirestore(context)
                val uid = user.uid
                val sRef = firestore.collection("users").document(uid).collection("sales").document(sale.saleId)
                val sData = hashMapOf<String, Any>(
                    "id" to sale.saleId,
                    "userId" to uid,
                    "totalMinor" to sale.totalMinor,
                    "subtotalMinor" to sale.subtotalMinor,
                    "discountMinor" to sale.discountMinor,
                    "taxMinor" to sale.taxMinor,
                    "paymentMethod" to sale.paymentMethod,
                    "businessDate" to sale.businessDate,
                    "status" to sale.status,
                    "voucherNo" to VoucherHelper.formatVoucherNumberFallback(sale.saleId, sale.createdAtEpochMs),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!sale.customerName.isNullOrBlank()) {
                    sData["customerName"] = sale.customerName
                }
                sRef.set(sData, SetOptions.merge()).await()
                Log.d(TAG, "Auto-synced sale ${sale.saleId} to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to auto-sync sale ${sale.saleId}", e)
            }
        }
    }

    fun syncCustomerAsync(context: Context, customer: CustomerEntity, scope: CoroutineScope) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val firestore = getFirestore(context)
                val uid = user.uid
                val cId = customer.customerId.toString()
                val cRef = firestore.collection("users").document(uid).collection("customers").document(cId)
                val cData = hashMapOf<String, Any>(
                    "id" to cId,
                    "userId" to uid,
                    "name" to customer.name,
                    "phone" to customer.phone,
                    "address" to customer.address,
                    "currentDebtMinor" to customer.currentDebtMinor,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                cRef.set(cData, SetOptions.merge()).await()
                Log.d(TAG, "Auto-synced customer ${customer.name} to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to auto-sync customer ${customer.name}", e)
            }
        }
    }
}
