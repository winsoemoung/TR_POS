package com.example.offlinepos.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.offlinepos.R
import com.example.offlinepos.data.local.AppDatabase
import com.example.offlinepos.data.local.CustomerEntity
import com.example.offlinepos.data.local.DebtTransactionEntity
import com.example.offlinepos.data.local.PaymentEntity
import com.example.offlinepos.data.local.ProductEntity
import com.example.offlinepos.data.local.PurchaseEntity
import com.example.offlinepos.data.local.ReceiptTemplateEntity
import com.example.offlinepos.data.local.ReturnTransactionEntity
import com.example.offlinepos.data.local.SaleEntity
import com.example.offlinepos.data.local.SaleItemEntity
import com.example.offlinepos.data.local.ShiftReportEntity
import com.example.offlinepos.data.local.StockMovementEntity
import com.example.offlinepos.data.local.SupplierEntity
import com.example.offlinepos.domain.VoucherHelper
import com.example.offlinepos.ui.StoreConfig
import com.example.offlinepos.ui.StoreProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class FirebaseSyncResult(
    val success: Boolean,
    val message: String,
    val productsSynced: Int = 0,
    val salesSynced: Int = 0,
    val saleItemsSynced: Int = 0,
    val customersSynced: Int = 0,
    val otherRecordsSynced: Int = 0,
)

internal enum class FirestoreOperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

/**
 * Testable repository handling full 13-collection Firebase Firestore backup & restore.
 * Accepts injected [FirebaseFirestore] and [FirebaseAuth] so production uses R.string.firestore_database_id
 * and Robolectric rule tests can inject local emulator instances.
 */
class FirebaseBackupRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        firestore = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth,
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Google Account Login မဝင်ရသေးပါ")

    internal fun handleFirestoreError(
        exception: Throwable,
        operationType: FirestoreOperationType,
        path: String?,
    ): String {
        val causes = generateSequence(exception) { it.cause }
        val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
            ?: causes.firstOrNull { it !is CancellationException }
            ?: exception

        val currentUser = auth.currentUser
        val providerInfoList = currentUser?.providerData?.map { provider ->
            JSONObject().apply {
                put("providerId", provider.providerId)
                put("email", provider.email)
            }
        } ?: emptyList()

        val authInfoJson = JSONObject().apply {
            put("userId", currentUser?.uid)
            put("email", currentUser?.email)
            put("emailVerified", currentUser?.isEmailVerified)
            put("tenantId", currentUser?.tenantId)
            put("providerInfo", JSONArray(providerInfoList))
        }

        val errorInfoJson = JSONObject().apply {
            put("error", rootError.message ?: rootError.toString())
            put("operationType", operationType.value)
            put("path", path)
            put("authInfo", authInfoJson)
        }

        val jsonString = errorInfoJson.toString()
        Log.e("FirestoreError", "Firestore Error: $jsonString")
        return jsonString
    }

    private fun userRoot(uid: String): DocumentReference =
        firestore.collection("users").document(uid)

    private suspend fun commitBatched(writes: List<Pair<DocumentReference, Map<String, Any>>>) {
        if (writes.isEmpty()) return
        for (chunk in writes.chunked(250)) {
            val batch = firestore.batch()
            for ((ref, data) in chunk) {
                batch.set(ref, data, SetOptions.merge())
            }
            withTimeout(25_000L) {
                batch.commit().await()
            }
        }
    }

    suspend fun backupAll(
        db: AppDatabase,
        storeConfig: StoreConfig? = null,
        storeNameFallback: String = "san thit",
    ): FirebaseSyncResult {
        val user = auth.currentUser
            ?: return FirebaseSyncResult(false, "Google Account Login မဝင်ရသေးပါ (Please sign in with Google)")
        val uid = user.uid
        val root = userRoot(uid)
        val profile = storeConfig?.getProfile()

        return try {
            val nowMs = System.currentTimeMillis()
            val userPayload = buildUserProfileMap(
                uid = uid,
                email = user.email ?: "",
                displayName = user.displayName ?: "Store Owner",
                profile = profile,
                storeNameFallback = storeNameFallback,
                currencySymbol = storeConfig?.getCurrencySymbol() ?: "Ks",
                promoCodesJson = storeConfig?.getPromoCodesRawJson(),
                lastBackupAtEpochMs = nowMs,
            )
            withTimeout(25_000L) {
                root.set(userPayload, SetOptions.merge()).await()
            }

            val products = db.productDao().getAll()
            val sales = db.checkoutDao().getAllSales()
            val saleItems = db.checkoutDao().getAllSaleItems()
            val payments = db.checkoutDao().getAllPayments()
            val customers = db.customerDao().getAllCustomers()
            val debtTxs = db.customerDao().getAllDebtTransactions()
            val suppliers = db.supplierDao().getAllSuppliersSync()
            val purchases = db.operationsDao().getAllPurchases()
            val returns = db.checkoutDao().getAllReturnTransactions()
            val shifts = db.operationsDao().getAllShifts()
            val movements = db.checkoutDao().getAllStockMovements()
            val templates = db.receiptTemplateDao().getAllTemplatesSync()

            val allWrites = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()

            products.forEach { p ->
                allWrites += root.collection("products").document(p.productId.toString()) to productToMap(uid, p)
            }
            sales.forEach { s ->
                allWrites += root.collection("sales").document(s.saleId) to saleToMap(uid, s)
            }
            saleItems.forEach { item ->
                val docId = "${sanitizeId(item.saleId)}_${item.lineId}"
                allWrites += root.collection("sale_items").document(docId) to saleItemToMap(uid, docId, item)
            }
            payments.forEach { pay ->
                val docId = sanitizeId(pay.paymentId)
                allWrites += root.collection("payments").document(docId) to paymentToMap(uid, docId, pay)
            }
            customers.forEach { c ->
                allWrites += root.collection("customers").document(c.customerId.toString()) to customerToMap(uid, c)
            }
            debtTxs.forEach { tx ->
                allWrites += root.collection("debt_transactions").document(tx.txId.toString()) to debtTxToMap(uid, tx)
            }
            suppliers.forEach { sup ->
                allWrites += root.collection("suppliers").document(sup.supplierId.toString()) to supplierToMap(uid, sup)
            }
            purchases.forEach { pur ->
                allWrites += root.collection("purchases").document(pur.purchaseId.toString()) to purchaseToMap(uid, pur)
            }
            returns.forEach { ret ->
                val docId = sanitizeId(ret.returnId)
                allWrites += root.collection("return_transactions").document(docId) to returnToMap(uid, docId, ret)
            }
            shifts.forEach { sh ->
                allWrites += root.collection("shifts").document(sh.shiftId.toString()) to shiftToMap(uid, sh)
            }
            movements.forEach { mov ->
                val docId = sanitizeId(mov.movementId)
                allWrites += root.collection("stock_movements").document(docId) to stockMovementToMap(uid, docId, mov)
            }
            templates.forEach { tmpl ->
                allWrites += root.collection("receipt_templates").document(tmpl.templateId.toString()) to templateToMap(uid, tmpl)
            }

            commitBatched(allWrites)

            val otherCount = payments.size + debtTxs.size + suppliers.size + purchases.size +
                returns.size + shifts.size + movements.size + templates.size

            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMs))
            val msg = "Firebase Cloud Backup အောင်မြင်ပါသည် ($timeStr) — " +
                "ပစ္စည်း ${products.size}, ဘောင်ချာ ${sales.size} (${saleItems.size} items), " +
                "Customer ${customers.size}, အခြား $otherCount"

            FirebaseSyncResult(
                success = true,
                message = msg,
                productsSynced = products.size,
                salesSynced = sales.size,
                saleItemsSynced = saleItems.size,
                customersSynced = customers.size,
                otherRecordsSynced = otherCount,
            )
        } catch (e: TimeoutCancellationException) {
            FirebaseSyncResult(
                success = false,
                message = "Firebase ချိတ်ဆက်မှု အချိန်ကျော်လွန်နေပါသည် (Timeout) — အင်တာနက်လိုင်း စစ်ဆေးပြီး ထပ်မံကြိုးစားပါ",
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.WRITE, "users/$uid")
            FirebaseSyncResult(
                success = false,
                message = "Firebase Backup မအောင်မြင်ပါ: ${e.localizedMessage ?: e.message}",
            )
        }
    }

    suspend fun syncSaleBundle(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        payment: PaymentEntity?,
        updatedProducts: List<ProductEntity>,
        updatedCustomer: CustomerEntity? = null,
    ): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        val root = userRoot(uid)
        return try {
            val writes = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
            writes += root.collection("sales").document(sanitizeId(sale.saleId)) to saleToMap(uid, sale)
            items.forEachIndexed { idx, item ->
                val lineKey = if (item.lineId > 0L) item.lineId.toString() else (idx + 1).toString()
                val docId = "${sanitizeId(sale.saleId)}_$lineKey"
                writes += root.collection("sale_items").document(docId) to saleItemToMap(uid, docId, item)
            }
            if (payment != null) {
                val pId = sanitizeId(payment.paymentId)
                writes += root.collection("payments").document(pId) to paymentToMap(uid, pId, payment)
            }
            updatedProducts.forEach { p ->
                writes += root.collection("products").document(p.productId.toString()) to productToMap(uid, p)
            }
            if (updatedCustomer != null) {
                writes += root.collection("customers").document(updatedCustomer.customerId.toString()) to customerToMap(uid, updatedCustomer)
            }
            commitBatched(writes)
            true
        } catch (e: TimeoutCancellationException) {
            Log.w("FirebaseBackupRepo", "syncSaleBundle timed out for ${sale.saleId}")
            false
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.WRITE, "users/$uid/sales/${sale.saleId}")
            false
        }
    }

    suspend fun restoreAll(
        db: AppDatabase,
        storeConfig: StoreConfig? = null,
        replaceLocal: Boolean = true,
    ): FirebaseSyncResult {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return FirebaseSyncResult(false, "Google Account Login မဝင်ရသေးပါ (Please sign in with Google)")
        }

        val root = userRoot(uid)
        return try {
            val (profileDoc, prodSnap, salesSnap, itemsSnap, paySnap, custSnap, debtSnap, supSnap, purSnap, retSnap, shiftSnap, movSnap, tmplSnap) = withTimeout(45_000L) {
                val p0 = root.get().await()
                val p1 = root.collection("products").get().await()
                val p2 = root.collection("sales").get().await()
                val p3 = root.collection("sale_items").get().await()
                val p4 = root.collection("payments").get().await()
                val p5 = root.collection("customers").get().await()
                val p6 = root.collection("debt_transactions").get().await()
                val p7 = root.collection("suppliers").get().await()
                val p8 = root.collection("purchases").get().await()
                val p9 = root.collection("return_transactions").get().await()
                val p10 = root.collection("shifts").get().await()
                val p11 = root.collection("stock_movements").get().await()
                val p12 = root.collection("receipt_templates").get().await()
                RestoreSnapshots(p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12)
            }

            val products = prodSnap.documents.mapNotNull { docToProduct(it) }
            val sales = salesSnap.documents.mapNotNull { docToSale(it) }
            val saleItems = itemsSnap.documents.mapNotNull { docToSaleItem(it) }
            val payments = paySnap.documents.mapNotNull { docToPayment(it) }
            val customers = custSnap.documents.mapNotNull { docToCustomer(it) }
            val debtTxs = debtSnap.documents.mapNotNull { docToDebtTx(it) }
            val suppliers = supSnap.documents.mapNotNull { docToSupplier(it) }
            val purchases = purSnap.documents.mapNotNull { docToPurchase(it) }
            val returns = retSnap.documents.mapNotNull { docToReturn(it) }
            val shifts = shiftSnap.documents.mapNotNull { docToShift(it) }
            val movements = movSnap.documents.mapNotNull { docToStockMovement(it) }
            val templates = tmplSnap.documents.mapNotNull { docToTemplate(it) }

            val totalRemoteCount = products.size + sales.size + saleItems.size + payments.size +
                customers.size + debtTxs.size + suppliers.size + purchases.size +
                returns.size + shifts.size + movements.size + templates.size

            if (totalRemoteCount == 0 && !profileDoc.exists()) {
                return FirebaseSyncResult(
                    success = false,
                    message = "Firebase Cloud ထဲတွင် Restore ပြုလုပ်ရန် Backup ဒေတာ မတွေ့သေးပါ",
                )
            }

            // Ensure unique non-null barcodes before Room insert to satisfy unique index
            val seenBarcodes = mutableSetOf<String>()
            val sanitizedProducts = products.map { p ->
                val bc = p.barcode?.takeIf { it.isNotBlank() }
                if (bc != null && !seenBarcodes.add(bc)) {
                    p.copy(barcode = null)
                } else {
                    p.copy(barcode = bc)
                }
            }

            db.withTransaction {
                if (replaceLocal) {
                    // Delete children before parents to respect ForeignKey.RESTRICT
                    db.checkoutDao().clearAllSaleItems()
                    db.checkoutDao().clearAllPayments()
                    db.checkoutDao().clearAllReturnTransactions()
                    db.checkoutDao().clearAllStockMovements()
                    db.customerDao().clearAllDebtTransactions()
                    db.checkoutDao().clearAllSales()
                    db.customerDao().clearAllCustomers()
                    db.operationsDao().clearAllPurchases()
                    db.supplierDao().clearAllSuppliers()
                    db.operationsDao().clearAllShifts()
                    db.receiptTemplateDao().clearAllTemplates()
                    db.productDao().clearAll()

                    sanitizedProducts.forEach { db.productDao().upsert(it) }
                    customers.forEach { db.customerDao().insertCustomer(it) }
                    if (suppliers.isNotEmpty()) db.supplierDao().insertSuppliers(suppliers)
                    if (templates.isNotEmpty()) db.receiptTemplateDao().insertTemplates(templates)
                    if (sales.isNotEmpty()) db.checkoutDao().upsertSales(sales)

                    val validSaleIds = sales.map { it.saleId }.toSet()
                    val validItems = saleItems.filter { validSaleIds.contains(it.saleId) }
                    if (validItems.isNotEmpty()) db.checkoutDao().upsertSaleItems(validItems)
                    val validPayments = payments.filter { validSaleIds.contains(it.saleId) }
                    if (validPayments.isNotEmpty()) db.checkoutDao().upsertPayments(validPayments)

                    if (debtTxs.isNotEmpty()) db.customerDao().upsertDebtTransactions(debtTxs)
                    purchases.forEach { db.operationsDao().insertPurchase(it) }
                    if (returns.isNotEmpty()) db.checkoutDao().upsertReturnTransactions(returns)
                    shifts.forEach { db.operationsDao().insertShift(it) }
                    if (movements.isNotEmpty()) db.checkoutDao().upsertStockMovements(movements)
                } else {
                    // Merge mode: preserve existing local data and add missing/updated cloud records
                    val existingLocalProducts = db.productDao().getAll()
                    val localProdById = existingLocalProducts.associateBy { it.productId }
                    val localBarcodes = existingLocalProducts.mapNotNull { it.barcode?.takeIf { b -> b.isNotBlank() } }.toMutableSet()
                    sanitizedProducts.forEach { p ->
                        val existing = localProdById[p.productId]
                        if (existing == null || p.updatedAtEpochMs > existing.updatedAtEpochMs) {
                            val safeBarcode = p.barcode?.takeIf { b ->
                                b == existing?.barcode || localBarcodes.add(b)
                            }
                            db.productDao().upsert(p.copy(barcode = safeBarcode))
                        }
                    }

                    val existingCustIds = db.customerDao().getAllCustomers().map { it.customerId }.toSet()
                    customers.filter { !existingCustIds.contains(it.customerId) }
                        .forEach { db.customerDao().insertCustomer(it) }

                    val existingSupIds = db.supplierDao().getAllSuppliersSync().map { it.supplierId }.toSet()
                    val newSups = suppliers.filter { !existingSupIds.contains(it.supplierId) }
                    if (newSups.isNotEmpty()) db.supplierDao().insertSuppliers(newSups)

                    if (templates.isNotEmpty()) {
                        val existingTmplIds = db.receiptTemplateDao().getAllTemplatesSync().map { it.templateId }.toSet()
                        val newTmpls = templates.filter { !existingTmplIds.contains(it.templateId) }
                        if (newTmpls.isNotEmpty()) db.receiptTemplateDao().insertTemplates(newTmpls)
                    }

                    val existingSales = db.checkoutDao().getAllSales().map { it.saleId }.toSet()
                    val newSales = sales.filter { !existingSales.contains(it.saleId) }
                    if (newSales.isNotEmpty()) db.checkoutDao().upsertSales(newSales)

                    val allLocalSaleIds = db.checkoutDao().getAllSales().map { it.saleId }.toSet()
                    val salesWithLocalItems = db.checkoutDao().getAllSaleItems().map { it.saleId }.toSet()
                    val newItems = saleItems.filter {
                        allLocalSaleIds.contains(it.saleId) && !salesWithLocalItems.contains(it.saleId)
                    }
                    if (newItems.isNotEmpty()) {
                        // Reset lineId to 0 if merging to avoid colliding with existing local auto-increment lineIds
                        db.checkoutDao().upsertSaleItems(newItems.map { it.copy(lineId = 0L) })
                    }

                    val existingPaymentIds = db.checkoutDao().getAllPayments().map { it.paymentId }.toSet()
                    val newPayments = payments.filter {
                        allLocalSaleIds.contains(it.saleId) && !existingPaymentIds.contains(it.paymentId)
                    }
                    if (newPayments.isNotEmpty()) db.checkoutDao().upsertPayments(newPayments)

                    val existingTxIds = db.customerDao().getAllDebtTransactions().map { it.txId }.toSet()
                    val newDebtTxs = debtTxs.filter { !existingTxIds.contains(it.txId) }
                    if (newDebtTxs.isNotEmpty()) db.customerDao().upsertDebtTransactions(newDebtTxs)

                    val existingPurchaseIds = db.operationsDao().getAllPurchases().map { it.purchaseId }.toSet()
                    purchases.filter { !existingPurchaseIds.contains(it.purchaseId) }
                        .forEach { db.operationsDao().insertPurchase(it) }

                    val existingReturnIds = db.checkoutDao().getAllReturnTransactions().map { it.returnId }.toSet()
                    val newReturns = returns.filter { !existingReturnIds.contains(it.returnId) }
                    if (newReturns.isNotEmpty()) db.checkoutDao().upsertReturnTransactions(newReturns)

                    val existingShiftIds = db.operationsDao().getAllShifts().map { it.shiftId }.toSet()
                    shifts.filter { !existingShiftIds.contains(it.shiftId) }
                        .forEach { db.operationsDao().insertShift(it) }

                    val existingMovIds = db.checkoutDao().getAllStockMovements().map { it.movementId }.toSet()
                    val newMovs = movements.filter { !existingMovIds.contains(it.movementId) }
                    if (newMovs.isNotEmpty()) db.checkoutDao().upsertStockMovements(newMovs)
                }
            }

            if (storeConfig != null && profileDoc.exists()) {
                val cur = storeConfig.getProfile()
                val restoredProfile = StoreProfile(
                    storeName = profileDoc.getString("storeName")?.takeIf { it.isNotBlank() } ?: cur.storeName,
                    address = profileDoc.getString("storeAddress") ?: cur.address,
                    phone = profileDoc.getString("storePhone") ?: cur.phone,
                    headerNote = profileDoc.getString("headerNote") ?: cur.headerNote,
                    footerMessage = profileDoc.getString("footerMessage") ?: cur.footerMessage,
                    autoBackupSchedule = cur.autoBackupSchedule,
                    taxRatePercent = profileDoc.getDouble("taxRatePercent") ?: cur.taxRatePercent,
                    customLogoPath = cur.customLogoPath,
                    receiptHeaderStyle = cur.receiptHeaderStyle,
                    receiptHeaderSize = cur.receiptHeaderSize,
                    receiptHeaderAlign = cur.receiptHeaderAlign,
                    receiptFooterStyle = cur.receiptFooterStyle,
                    receiptFooterSize = cur.receiptFooterSize,
                    receiptFooterAlign = cur.receiptFooterAlign,
                )
                if (replaceLocal) {
                    storeConfig.saveProfile(restoredProfile)
                    profileDoc.getString("currencySymbol")?.takeIf { it.isNotBlank() }?.let {
                        storeConfig.setCurrencySymbol(it)
                    }
                    profileDoc.getString("promoCodesJson")?.takeIf { it.isNotBlank() }?.let {
                        storeConfig.setPromoCodesRawJson(it)
                    }
                }
            }

            val otherCount = payments.size + debtTxs.size + suppliers.size + purchases.size +
                returns.size + shifts.size + movements.size + templates.size
            val modeLabel = if (replaceLocal) "Full Restore" else "Merge Restore"
            val msg = "Firebase မှ $modeLabel အောင်မြင်ပါသည် — " +
                "ပစ္စည်း ${products.size}, ဘောင်ချာ ${sales.size} (${saleItems.size} items), " +
                "Customer ${customers.size}, အခြား $otherCount"

            FirebaseSyncResult(
                success = true,
                message = msg,
                productsSynced = products.size,
                salesSynced = sales.size,
                saleItemsSynced = saleItems.size,
                customersSynced = customers.size,
                otherRecordsSynced = otherCount,
            )
        } catch (e: TimeoutCancellationException) {
            FirebaseSyncResult(
                success = false,
                message = "Firebase Restore အချိန်ကျော်လွန်နေပါသည် (Timeout) — အင်တာနက်လိုင်း စစ်ဆေးပြီး ထပ်မံကြိုးစားပါ",
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, FirestoreOperationType.LIST, "users/$uid")
            FirebaseSyncResult(
                success = false,
                message = "Firebase Restore မအောင်မြင်ပါ: ${e.localizedMessage ?: e.message}",
            )
        }
    }

    private data class RestoreSnapshots(
        val profileDoc: DocumentSnapshot,
        val prodSnap: com.google.firebase.firestore.QuerySnapshot,
        val salesSnap: com.google.firebase.firestore.QuerySnapshot,
        val itemsSnap: com.google.firebase.firestore.QuerySnapshot,
        val paySnap: com.google.firebase.firestore.QuerySnapshot,
        val custSnap: com.google.firebase.firestore.QuerySnapshot,
        val debtSnap: com.google.firebase.firestore.QuerySnapshot,
        val supSnap: com.google.firebase.firestore.QuerySnapshot,
        val purSnap: com.google.firebase.firestore.QuerySnapshot,
        val retSnap: com.google.firebase.firestore.QuerySnapshot,
        val shiftSnap: com.google.firebase.firestore.QuerySnapshot,
        val movSnap: com.google.firebase.firestore.QuerySnapshot,
        val tmplSnap: com.google.firebase.firestore.QuerySnapshot,
    )

    private fun sanitizeId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        return cleaned.ifBlank { "doc_${System.currentTimeMillis()}" }
    }

    private fun buildUserProfileMap(
        uid: String,
        email: String,
        displayName: String,
        profile: StoreProfile?,
        storeNameFallback: String,
        currencySymbol: String,
        promoCodesJson: String?,
        lastBackupAtEpochMs: Long,
    ): Map<String, Any> = buildMap {
        put("userId", uid)
        put("email", email.take(200))
        put("displayName", displayName.take(100))
        put("storeName", (profile?.storeName ?: storeNameFallback).take(100))
        if (profile != null) {
            put("storeAddress", profile.address.take(300))
            put("storePhone", profile.phone.take(100))
            put("headerNote", profile.headerNote.take(300))
            put("footerMessage", profile.footerMessage.take(500))
            put("taxRatePercent", profile.taxRatePercent)
        }
        put("currencySymbol", currencySymbol.take(20))
        if (!promoCodesJson.isNullOrBlank()) {
            put("promoCodesJson", promoCodesJson.take(16000))
        }
        put("lastBackupAtEpochMs", lastBackupAtEpochMs)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun productToMap(uid: String, p: ProductEntity): Map<String, Any> = buildMap {
        put("id", p.productId.toString())
        put("productId", p.productId)
        put("userId", uid)
        put("name", p.name.ifBlank { "Unnamed" }.take(200))
        put("sku", p.sku.take(100))
        p.barcode?.takeIf { it.isNotBlank() }?.let { put("barcode", it.take(100)) }
        put("category", p.category.take(100))
        put("costPriceMinor", p.costPriceMinor.coerceAtLeast(0L))
        put("sellPriceMinor", p.sellPriceMinor.coerceAtLeast(0L))
        put("stockQuantity", p.stockQuantity)
        p.stockQuantityDecimal?.let { put("stockQuantityDecimal", it) }
        put("lowStockThreshold", p.lowStockThreshold)
        put("isActive", p.isActive)
        put("isRoofingSheet", p.isRoofingSheet)
        put("wholesalePriceMinor", p.wholesalePriceMinor.coerceAtLeast(0L))
        put("wholesaleMinQty", p.wholesaleMinQty.coerceAtLeast(0L))
        put("unitName", p.unitName.take(50))
        put("wholesaleUnitName", p.wholesaleUnitName.take(50))
        put("wholesaleUnitMultiplier", p.wholesaleUnitMultiplier.coerceAtLeast(0L))
        p.imageUri?.takeIf { it.isNotBlank() }?.let { put("imageUri", it.take(500)) }
        put("updatedAtEpochMs", p.updatedAtEpochMs)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun saleToMap(uid: String, s: SaleEntity): Map<String, Any> = buildMap {
        put("id", sanitizeId(s.saleId))
        put("userId", uid)
        put("businessDate", s.businessDate.take(50))
        put("subtotalMinor", s.subtotalMinor)
        put("discountMinor", s.discountMinor)
        put("taxMinor", s.taxMinor)
        put("totalMinor", s.totalMinor.coerceAtLeast(0L))
        put("status", s.status.take(50))
        put("cashierId", s.cashierId.take(100))
        put("deviceId", s.deviceId.take(100))
        put("createdAtEpochMs", s.createdAtEpochMs)
        s.customerId?.let { put("customerId", it) }
        s.customerName?.takeIf { it.isNotBlank() }?.let { put("customerName", it.take(100)) }
        put("paymentMethod", s.paymentMethod.ifBlank { "CASH" }.take(50))
        put("splitCashMinor", s.splitCashMinor)
        put("splitDigitalMinor", s.splitDigitalMinor)
        put("deliveryFeeMinor", s.deliveryFeeMinor)
        put("pickupStatus", s.pickupStatus.take(50))
        s.expectedPickupDate?.takeIf { it.isNotBlank() }?.let { put("expectedPickupDate", it.take(50)) }
        s.pickupNote?.takeIf { it.isNotBlank() }?.let { put("pickupNote", it.take(300)) }
        put("voucherNo", VoucherHelper.formatVoucherNumberFallback(s.saleId, s.createdAtEpochMs).take(100))
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun saleItemToMap(uid: String, docId: String, item: SaleItemEntity): Map<String, Any> = buildMap {
        put("id", docId)
        put("userId", uid)
        put("lineId", item.lineId)
        put("saleId", sanitizeId(item.saleId))
        put("productId", item.productId)
        put("productNameSnapshot", item.productNameSnapshot.ifBlank { "Item" }.take(200))
        put("unitPriceMinor", item.unitPriceMinor)
        put("quantity", item.quantity)
        put("lineDiscountMinor", item.lineDiscountMinor)
        put("lineTaxMinor", item.lineTaxMinor)
        put("lineTotalMinor", item.lineTotalMinor)
        put("unitName", item.unitName.take(50))
        put("returnedQuantity", item.returnedQuantity)
        put("quantityDecimal", item.quantityDecimal)
        put("returnedQuantityDecimal", item.returnedQuantityDecimal)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun paymentToMap(uid: String, docId: String, p: PaymentEntity): Map<String, Any> = buildMap {
        put("id", docId)
        put("userId", uid)
        put("saleId", sanitizeId(p.saleId))
        put("method", p.method.ifBlank { "CASH" }.take(50))
        put("tenderedMinor", p.tenderedMinor)
        put("changeMinor", p.changeMinor)
        put("authorizedMinor", p.authorizedMinor)
        put("authorizationState", p.authorizationState.take(50))
        p.providerReference?.takeIf { it.isNotBlank() }?.let { put("providerReference", it.take(200)) }
        put("settlementState", p.settlementState.take(50))
        put("manualRecorded", p.manualRecorded)
        put("recordedByUserId", p.recordedByUserId.take(100))
        put("createdAtEpochMs", p.createdAtEpochMs)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun customerToMap(uid: String, c: CustomerEntity): Map<String, Any> = buildMap {
        put("id", c.customerId.toString())
        put("customerId", c.customerId)
        put("userId", uid)
        put("name", c.name.ifBlank { "Customer" }.take(200))
        put("phone", c.phone.take(50))
        put("address", c.address.take(300))
        put("creditLimitMinor", c.creditLimitMinor)
        put("currentDebtMinor", c.currentDebtMinor)
        c.creditDueDateEpochMs?.let { put("creditDueDateEpochMs", it) }
        put("createdAtEpochMs", c.createdAtEpochMs)
        put("notes", c.notes.take(500))
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun debtTxToMap(uid: String, tx: DebtTransactionEntity): Map<String, Any> = buildMap {
        put("id", tx.txId.toString())
        put("txId", tx.txId)
        put("userId", uid)
        put("customerId", tx.customerId)
        tx.saleId?.takeIf { it.isNotBlank() }?.let { put("saleId", sanitizeId(it)) }
        put("type", tx.type.ifBlank { "REPAYMENT" }.take(50))
        put("amountMinor", tx.amountMinor)
        put("remainingBalanceMinor", tx.remainingBalanceMinor)
        put("note", tx.note.take(500))
        put("cashierId", tx.cashierId.take(100))
        put("createdAtEpochMs", tx.createdAtEpochMs)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun supplierToMap(uid: String, s: SupplierEntity): Map<String, Any> = buildMap {
        put("id", s.supplierId.toString())
        put("supplierId", s.supplierId)
        put("userId", uid)
        put("name", s.name.ifBlank { "Supplier" }.take(200))
        put("shopName", s.shopName.take(200))
        put("phone", s.phone.take(50))
        put("email", s.email.take(200))
        put("address", s.address.take(300))
        put("taxNumber", s.taxNumber.take(100))
        put("note", s.note.take(500))
        put("createdAtEpochMs", s.createdAtEpochMs)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun purchaseToMap(uid: String, pur: PurchaseEntity): Map<String, Any> = buildMap {
        put("id", pur.purchaseId.toString())
        put("purchaseId", pur.purchaseId)
        put("userId", uid)
        put("productId", pur.productId)
        put("productName", pur.productName.ifBlank { "Product" }.take(200))
        put("supplierName", pur.supplierName.take(200))
        put("quantity", pur.quantity)
        put("unitCostMinor", pur.unitCostMinor)
        put("totalCostMinor", pur.totalCostMinor)
        put("invoiceRef", pur.invoiceRef.take(100))
        put("note", pur.note.take(500))
        put("createdAtEpochMs", pur.createdAtEpochMs)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun returnToMap(uid: String, docId: String, r: ReturnTransactionEntity): Map<String, Any> = buildMap {
        put("id", docId)
        put("userId", uid)
        put("saleId", sanitizeId(r.saleId))
        put("returnedAmountMinor", r.returnedAmountMinor)
        put("refundMethod", r.refundMethod.take(50))
        r.refundReference?.takeIf { it.isNotBlank() }?.let { put("refundReference", it.take(200)) }
        r.reason?.takeIf { it.isNotBlank() }?.let { put("reason", it.take(300)) }
        put("actorId", r.actorId.take(100))
        put("createdAtEpochMs", r.createdAtEpochMs)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun shiftToMap(uid: String, sh: ShiftReportEntity): Map<String, Any> = buildMap {
        put("id", sh.shiftId.toString())
        put("shiftId", sh.shiftId)
        put("userId", uid)
        put("cashierName", sh.cashierName.ifBlank { "Cashier" }.take(100))
        put("openTimeEpochMs", sh.openTimeEpochMs)
        put("closeTimeEpochMs", sh.closeTimeEpochMs)
        put("openingCashMinor", sh.openingCashMinor)
        put("cashSalesMinor", sh.cashSalesMinor)
        put("digitalSalesMinor", sh.digitalSalesMinor)
        put("creditSalesMinor", sh.creditSalesMinor)
        put("expenseMinor", sh.expenseMinor)
        put("expectedCashMinor", sh.expectedCashMinor)
        put("actualCashCountedMinor", sh.actualCashCountedMinor)
        put("differenceMinor", sh.differenceMinor)
        put("status", sh.status.take(50))
        put("note", sh.note.take(500))
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun stockMovementToMap(uid: String, docId: String, m: StockMovementEntity): Map<String, Any> = buildMap {
        put("id", docId)
        put("userId", uid)
        put("productId", m.productId)
        put("movementType", m.movementType.ifBlank { "ADJUST" }.take(50))
        put("quantityChange", m.quantityChange)
        put("quantityBefore", m.quantityBefore)
        put("quantityAfter", m.quantityAfter)
        m.referenceId?.takeIf { it.isNotBlank() }?.let { put("referenceId", it.take(128)) }
        m.reason?.takeIf { it.isNotBlank() }?.let { put("reason", it.take(300)) }
        put("actorId", m.actorId.take(100))
        put("createdAtEpochMs", m.createdAtEpochMs)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun templateToMap(uid: String, t: ReceiptTemplateEntity): Map<String, Any> = buildMap {
        put("id", t.templateId.toString())
        put("templateId", t.templateId)
        put("userId", uid)
        put("name", t.name.ifBlank { "Template ${t.templateId}" }.take(100))
        put("isDefault", t.isDefault)
        put("paperWidthDots", t.paperWidthDots)
        put("itemLayoutStyle", t.itemLayoutStyle.take(50))
        put("fontScalePercent", t.fontScalePercent)
        put("lineSpacingDp", t.lineSpacingDp)
        put("showLogo", t.showLogo)
        put("customStoreName", t.customStoreName.take(200))
        put("customSubtitle", t.customSubtitle.take(200))
        put("customOwnerOrContact", t.customOwnerOrContact.take(200))
        put("customPhone", t.customPhone.take(100))
        put("customAddress", t.customAddress.take(300))
        put("storeNameFontSize", t.storeNameFontSize.take(20))
        put("storeNameAlign", t.storeNameAlign.take(20))
        put("headerDetailsFontSize", t.headerDetailsFontSize.take(20))
        put("showAddress", t.showAddress)
        put("showPhone", t.showPhone)
        put("showHeaderNote", t.showHeaderNote)
        put("customHeaderNote", t.customHeaderNote.take(500))
        put("showCashierInfo", t.showCashierInfo)
        put("showVoucherNumber", t.showVoucherNumber)
        put("showCustomerInfo", t.showCustomerInfo)
        put("customerLabel", t.customerLabel.take(100))
        put("metaFontSize", t.metaFontSize.take(20))
        put("itemNameWidthWeight", t.itemNameWidthWeight.toDouble())
        put("qtyWidthWeight", t.qtyWidthWeight.toDouble())
        put("priceWidthWeight", t.priceWidthWeight.toDouble())
        put("amountWidthWeight", t.amountWidthWeight.toDouble())
        put("rowPaddingDp", t.rowPaddingDp)
        put("gridLineStyle", t.gridLineStyle.take(20))
        put("bodyFontSize", t.bodyFontSize.take(20))
        put("itemNameAlign", t.itemNameAlign.take(20))
        put("qtyAlign", t.qtyAlign.take(20))
        put("priceAlign", t.priceAlign.take(20))
        put("amountAlign", t.amountAlign.take(20))
        put("totalsFontSize", t.totalsFontSize.take(20))
        put("showTotalsBreakdown", t.showTotalsBreakdown)
        put("showPaymentMethod", t.showPaymentMethod)
        put("showBarcodeOrQr", t.showBarcodeOrQr)
        put("footerMessage", t.footerMessage.take(500))
        put("returnPolicyText", t.returnPolicyText.take(500))
        put("extraNoticeText", t.extraNoticeText.take(500))
        put("showPrintTime", t.showPrintTime)
        put("footerAlign", t.footerAlign.take(20))
        put("footerFontSize", t.footerFontSize.take(20))
        put("updatedAtEpochMs", t.updatedAtEpochMs)
        put("updatedAt", FieldValue.serverTimestamp())
    }

    private fun docToProduct(doc: DocumentSnapshot): ProductEntity? {
        val idLong = doc.getLong("productId") ?: doc.getString("id")?.toLongOrNull() ?: doc.id.toLongOrNull() ?: return null
        val name = doc.getString("name") ?: return null
        return ProductEntity(
            productId = idLong,
            name = name,
            sku = doc.getString("sku") ?: "SKU-$idLong",
            barcode = doc.getString("barcode"),
            costPriceMinor = doc.getLong("costPriceMinor") ?: 0L,
            sellPriceMinor = doc.getLong("sellPriceMinor") ?: 0L,
            stockQuantity = doc.getLong("stockQuantity") ?: 0L,
            isActive = doc.getBoolean("isActive") ?: true,
            updatedAtEpochMs = doc.getLong("updatedAtEpochMs") ?: System.currentTimeMillis(),
            category = doc.getString("category") ?: "အထွေထွေ",
            isRoofingSheet = doc.getBoolean("isRoofingSheet") ?: false,
            lowStockThreshold = doc.getLong("lowStockThreshold") ?: 3L,
            imageUri = doc.getString("imageUri"),
            wholesalePriceMinor = doc.getLong("wholesalePriceMinor") ?: 0L,
            wholesaleMinQty = doc.getLong("wholesaleMinQty") ?: 0L,
            unitName = doc.getString("unitName") ?: "ခု",
            wholesaleUnitName = doc.getString("wholesaleUnitName") ?: "ဖာ",
            wholesaleUnitMultiplier = doc.getLong("wholesaleUnitMultiplier") ?: 1L,
            stockQuantityDecimal = doc.getDouble("stockQuantityDecimal"),
        )
    }

    private fun docToSale(doc: DocumentSnapshot): SaleEntity? {
        val saleId = doc.getString("id") ?: doc.id
        if (saleId.isBlank()) return null
        return SaleEntity(
            saleId = saleId,
            businessDate = doc.getString("businessDate") ?: "2026-01-01",
            subtotalMinor = doc.getLong("subtotalMinor") ?: (doc.getLong("totalMinor") ?: 0L),
            discountMinor = doc.getLong("discountMinor") ?: 0L,
            taxMinor = doc.getLong("taxMinor") ?: 0L,
            totalMinor = doc.getLong("totalMinor") ?: 0L,
            status = doc.getString("status") ?: "COMPLETED",
            cashierId = doc.getString("cashierId") ?: "Cashier",
            deviceId = doc.getString("deviceId") ?: "SAN-THIT-POS-01",
            createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
            customerId = doc.getLong("customerId"),
            customerName = doc.getString("customerName"),
            paymentMethod = doc.getString("paymentMethod") ?: "CASH",
            splitCashMinor = doc.getLong("splitCashMinor") ?: 0L,
            splitDigitalMinor = doc.getLong("splitDigitalMinor") ?: 0L,
            deliveryFeeMinor = doc.getLong("deliveryFeeMinor") ?: 0L,
            pickupStatus = doc.getString("pickupStatus") ?: "PICKED_UP",
            expectedPickupDate = doc.getString("expectedPickupDate"),
            pickupNote = doc.getString("pickupNote"),
        )
    }

    private fun docToSaleItem(doc: DocumentSnapshot): SaleItemEntity? {
        val saleId = doc.getString("saleId") ?: return null
        val name = doc.getString("productNameSnapshot") ?: return null
        return SaleItemEntity(
            lineId = doc.getLong("lineId") ?: 0L,
            saleId = saleId,
            productId = doc.getLong("productId") ?: 0L,
            productNameSnapshot = name,
            unitPriceMinor = doc.getLong("unitPriceMinor") ?: 0L,
            quantity = doc.getLong("quantity") ?: 1L,
            lineDiscountMinor = doc.getLong("lineDiscountMinor") ?: 0L,
            lineTaxMinor = doc.getLong("lineTaxMinor") ?: 0L,
            lineTotalMinor = doc.getLong("lineTotalMinor") ?: 0L,
            unitName = doc.getString("unitName") ?: "ခု",
            returnedQuantity = doc.getLong("returnedQuantity") ?: 0L,
            quantityDecimal = doc.getDouble("quantityDecimal") ?: 0.0,
            returnedQuantityDecimal = doc.getDouble("returnedQuantityDecimal") ?: 0.0,
        )
    }

    private fun docToPayment(doc: DocumentSnapshot): PaymentEntity? {
        val saleId = doc.getString("saleId") ?: return null
        val paymentId = doc.getString("id") ?: doc.id
        return PaymentEntity(
            paymentId = paymentId,
            saleId = saleId,
            method = doc.getString("method") ?: "CASH",
            tenderedMinor = doc.getLong("tenderedMinor") ?: 0L,
            changeMinor = doc.getLong("changeMinor") ?: 0L,
            authorizedMinor = doc.getLong("authorizedMinor") ?: 0L,
            authorizationState = doc.getString("authorizationState") ?: "APPROVED",
            providerReference = doc.getString("providerReference"),
            settlementState = doc.getString("settlementState") ?: "SETTLED",
            manualRecorded = doc.getBoolean("manualRecorded") ?: false,
            recordedByUserId = doc.getString("recordedByUserId") ?: "Cashier",
            createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
        )
    }

    private fun docToCustomer(doc: DocumentSnapshot): CustomerEntity? {
        val idLong = doc.getLong("customerId") ?: doc.getString("id")?.toLongOrNull() ?: doc.id.toLongOrNull() ?: return null
        val name = doc.getString("name") ?: return null
        return CustomerEntity(
            customerId = idLong,
            name = name,
            phone = doc.getString("phone") ?: "",
            address = doc.getString("address") ?: "",
            creditLimitMinor = doc.getLong("creditLimitMinor") ?: 0L,
            currentDebtMinor = doc.getLong("currentDebtMinor") ?: 0L,
            creditDueDateEpochMs = doc.getLong("creditDueDateEpochMs"),
            createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
            notes = doc.getString("notes") ?: "",
        )
    }

    private fun docToDebtTx(doc: DocumentSnapshot): DebtTransactionEntity? {
        val txId = doc.getLong("txId") ?: doc.getString("id")?.toLongOrNull() ?: doc.id.toLongOrNull() ?: return null
        val customerId = doc.getLong("customerId") ?: return null
        return DebtTransactionEntity(
            txId = txId,
            customerId = customerId,
            saleId = doc.getString("saleId"),
            type = doc.getString("type") ?: "REPAYMENT",
            amountMinor = doc.getLong("amountMinor") ?: 0L,
            remainingBalanceMinor = doc.getLong("remainingBalanceMinor") ?: 0L,
            note = doc.getString("note") ?: "",
            cashierId = doc.getString("cashierId") ?: "",
            createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
        )
    }

    private fun docToSupplier(doc: DocumentSnapshot): SupplierEntity? {
        val supplierId = doc.getLong("supplierId") ?: doc.getString("id")?.toLongOrNull() ?: doc.id.toLongOrNull() ?: return null
        val name = doc.getString("name") ?: return null
        return SupplierEntity(
            supplierId = supplierId,
            name = name,
            shopName = doc.getString("shopName") ?: "",
            phone = doc.getString("phone") ?: "",
            email = doc.getString("email") ?: "",
            address = doc.getString("address") ?: "",
            taxNumber = doc.getString("taxNumber") ?: "",
            note = doc.getString("note") ?: "",
            createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
        )
    }

    private fun docToPurchase(doc: DocumentSnapshot): PurchaseEntity? {
        val purchaseId = doc.getLong("purchaseId") ?: doc.getString("id")?.toLongOrNull() ?: doc.id.toLongOrNull() ?: return null
        val productName = doc.getString("productName") ?: return null
        return PurchaseEntity(
            purchaseId = purchaseId,
            productId = doc.getLong("productId") ?: 0L,
            productName = productName,
            supplierName = doc.getString("supplierName") ?: "",
            quantity = doc.getLong("quantity") ?: 0L,
            unitCostMinor = doc.getLong("unitCostMinor") ?: 0L,
            totalCostMinor = doc.getLong("totalCostMinor") ?: 0L,
            invoiceRef = doc.getString("invoiceRef") ?: "",
            note = doc.getString("note") ?: "",
            createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
        )
    }

    private fun docToReturn(doc: DocumentSnapshot): ReturnTransactionEntity? {
        val returnId = doc.getString("id") ?: doc.id
        val saleId = doc.getString("saleId") ?: return null
        return ReturnTransactionEntity(
            returnId = returnId,
            saleId = saleId,
            returnedAmountMinor = doc.getLong("returnedAmountMinor") ?: 0L,
            refundMethod = doc.getString("refundMethod") ?: "CASH",
            refundReference = doc.getString("refundReference"),
            reason = doc.getString("reason"),
            actorId = doc.getString("actorId") ?: "Admin",
            createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
        )
    }

    private fun docToShift(doc: DocumentSnapshot): ShiftReportEntity? {
        val shiftId = doc.getLong("shiftId") ?: doc.getString("id")?.toLongOrNull() ?: doc.id.toLongOrNull() ?: return null
        val cashierName = doc.getString("cashierName") ?: return null
        return ShiftReportEntity(
            shiftId = shiftId,
            cashierName = cashierName,
            openTimeEpochMs = doc.getLong("openTimeEpochMs") ?: System.currentTimeMillis(),
            closeTimeEpochMs = doc.getLong("closeTimeEpochMs") ?: 0L,
            openingCashMinor = doc.getLong("openingCashMinor") ?: 0L,
            cashSalesMinor = doc.getLong("cashSalesMinor") ?: 0L,
            digitalSalesMinor = doc.getLong("digitalSalesMinor") ?: 0L,
            creditSalesMinor = doc.getLong("creditSalesMinor") ?: 0L,
            expenseMinor = doc.getLong("expenseMinor") ?: 0L,
            expectedCashMinor = doc.getLong("expectedCashMinor") ?: 0L,
            actualCashCountedMinor = doc.getLong("actualCashCountedMinor") ?: 0L,
            differenceMinor = doc.getLong("differenceMinor") ?: 0L,
            status = doc.getString("status") ?: "CLOSED",
            note = doc.getString("note") ?: "",
        )
    }

    private fun docToStockMovement(doc: DocumentSnapshot): StockMovementEntity? {
        val movementId = doc.getString("id") ?: doc.id
        val productId = doc.getLong("productId") ?: return null
        return StockMovementEntity(
            movementId = movementId,
            productId = productId,
            movementType = doc.getString("movementType") ?: "ADJUST",
            quantityChange = doc.getLong("quantityChange") ?: 0L,
            quantityBefore = doc.getLong("quantityBefore") ?: 0L,
            quantityAfter = doc.getLong("quantityAfter") ?: 0L,
            referenceId = doc.getString("referenceId"),
            reason = doc.getString("reason"),
            actorId = doc.getString("actorId") ?: "SanThit",
            createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
        )
    }

    private fun docToTemplate(doc: DocumentSnapshot): ReceiptTemplateEntity? {
        val templateId = (doc.getLong("templateId") ?: doc.id.toLongOrNull())?.toInt() ?: return null
        val name = doc.getString("name") ?: return null
        return ReceiptTemplateEntity(
            templateId = templateId,
            name = name,
            isDefault = doc.getBoolean("isDefault") ?: false,
            paperWidthDots = (doc.getLong("paperWidthDots") ?: 576L).toInt(),
            itemLayoutStyle = doc.getString("itemLayoutStyle") ?: "STANDARD_2LINE",
            fontScalePercent = (doc.getLong("fontScalePercent") ?: 100L).toInt(),
            lineSpacingDp = (doc.getLong("lineSpacingDp") ?: 4L).toInt(),
            showLogo = doc.getBoolean("showLogo") ?: true,
            customStoreName = doc.getString("customStoreName") ?: "",
            customSubtitle = doc.getString("customSubtitle") ?: "",
            customOwnerOrContact = doc.getString("customOwnerOrContact") ?: "",
            customPhone = doc.getString("customPhone") ?: "",
            customAddress = doc.getString("customAddress") ?: "",
            storeNameFontSize = doc.getString("storeNameFontSize") ?: "LARGE",
            storeNameAlign = doc.getString("storeNameAlign") ?: "CENTER",
            headerDetailsFontSize = doc.getString("headerDetailsFontSize") ?: "MEDIUM",
            showAddress = doc.getBoolean("showAddress") ?: true,
            showPhone = doc.getBoolean("showPhone") ?: true,
            showHeaderNote = doc.getBoolean("showHeaderNote") ?: true,
            customHeaderNote = doc.getString("customHeaderNote") ?: "",
            showCashierInfo = doc.getBoolean("showCashierInfo") ?: true,
            showVoucherNumber = doc.getBoolean("showVoucherNumber") ?: true,
            showCustomerInfo = doc.getBoolean("showCustomerInfo") ?: true,
            customerLabel = doc.getString("customerLabel") ?: "ဈေးဝယ်သူ",
            metaFontSize = doc.getString("metaFontSize") ?: "MEDIUM",
            itemNameWidthWeight = (doc.getDouble("itemNameWidthWeight") ?: 0.42).toFloat(),
            qtyWidthWeight = (doc.getDouble("qtyWidthWeight") ?: 0.16).toFloat(),
            priceWidthWeight = (doc.getDouble("priceWidthWeight") ?: 0.20).toFloat(),
            amountWidthWeight = (doc.getDouble("amountWidthWeight") ?: 0.22).toFloat(),
            rowPaddingDp = (doc.getLong("rowPaddingDp") ?: 4L).toInt(),
            gridLineStyle = doc.getString("gridLineStyle") ?: "DASHED",
            bodyFontSize = doc.getString("bodyFontSize") ?: "MEDIUM",
            itemNameAlign = doc.getString("itemNameAlign") ?: "LEFT",
            qtyAlign = doc.getString("qtyAlign") ?: "CENTER",
            priceAlign = doc.getString("priceAlign") ?: "RIGHT",
            amountAlign = doc.getString("amountAlign") ?: "RIGHT",
            totalsFontSize = doc.getString("totalsFontSize") ?: "MEDIUM",
            showTotalsBreakdown = doc.getBoolean("showTotalsBreakdown") ?: true,
            showPaymentMethod = doc.getBoolean("showPaymentMethod") ?: true,
            showBarcodeOrQr = doc.getBoolean("showBarcodeOrQr") ?: true,
            footerMessage = doc.getString("footerMessage") ?: "",
            returnPolicyText = doc.getString("returnPolicyText") ?: "",
            extraNoticeText = doc.getString("extraNoticeText") ?: "",
            showPrintTime = doc.getBoolean("showPrintTime") ?: true,
            footerAlign = doc.getString("footerAlign") ?: "CENTER",
            footerFontSize = doc.getString("footerFontSize") ?: "SMALL",
            updatedAtEpochMs = doc.getLong("updatedAtEpochMs") ?: System.currentTimeMillis(),
        )
    }
}

object FirebaseFirestoreSyncManager {
    private const val TAG = "FirestoreSyncManager"

    private val autoSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val hasPendingAutoSync = AtomicBoolean(false)
    private val isDrainingAutoSync = AtomicBoolean(false)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTimeEpochMs = MutableStateFlow(0L)
    val lastSyncTimeEpochMs: StateFlow<Long> = _lastSyncTimeEpochMs.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>("Firebase Real-time Auto-Sync အသင့်ရှိပါသည်")
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: Exception) {
            true
        }
    }

    private fun enqueueOfflineWorker(appContext: Context) {
        runCatching {
            val workRequest = OneTimeWorkRequestBuilder<FirebaseRealtimeSyncWorker>()
                .setConstraints(FirebaseRealtimeSyncWorker.constraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag("firebase-realtime-sync")
                .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                "firebase-realtime-sync-queue",
                ExistingWorkPolicy.KEEP,
                workRequest,
            )
        }
    }

    fun triggerRealtimeSync(
        context: Context,
        db: AppDatabase,
        storeName: String = "san thit",
        storeConfig: StoreConfig? = null,
        reason: String = "Data Change",
    ) {
        val appContext = context.applicationContext
        if (Firebase.auth.currentUser == null) {
            _syncStatus.value = "Firebase Sign In ဝင်ပြီးပါက Real-time Auto-Sync အလိုအလျောက် စတင်ပါမည်"
            return
        }

        hasPendingAutoSync.set(true)
        if (!isNetworkAvailable(appContext)) {
            _syncStatus.value = "⏳ အော့ဖ်လိုင်းဖြစ်နေပါသည် (လိုင်းပြန်ရသည်နှင့် Firebase သို့ အလိုအလျောက် ပို့ပါမည်)"
            enqueueOfflineWorker(appContext)
            return
        }

        if (!isDrainingAutoSync.compareAndSet(false, true)) {
            return
        }

        autoSyncScope.launch {
            try {
                while (hasPendingAutoSync.getAndSet(false)) {
                    if (!isNetworkAvailable(appContext)) {
                        _syncStatus.value = "⏳ အော့ဖ်လိုင်းဖြစ်နေပါသည် (လိုင်းပြန်ရသည်နှင့် Firebase သို့ အလိုအလျောက် ပို့ပါမည်)"
                        enqueueOfflineWorker(appContext)
                        break
                    }
                    val cfg = storeConfig ?: StoreConfig(appContext)
                    val res = syncAll(
                        context = appContext,
                        db = db,
                        storeName = storeName,
                        storeConfig = cfg,
                        reason = reason,
                    )
                    if (!res.success) {
                        enqueueOfflineWorker(appContext)
                    }
                }
            } finally {
                isDrainingAutoSync.set(false)
                if (hasPendingAutoSync.get() && isNetworkAvailable(appContext) && Firebase.auth.currentUser != null) {
                    triggerRealtimeSync(appContext, db, storeName, storeConfig, reason)
                }
            }
        }
    }

    suspend fun syncAll(
        context: Context,
        db: AppDatabase,
        storeName: String = "san thit",
        storeConfig: StoreConfig? = null,
        reason: String? = null,
    ): FirebaseSyncResult {
        val appContext = context.applicationContext
        if (Firebase.auth.currentUser == null) {
            val msg = "Google Account Login မဝင်ရသေးပါ (Please sign in with Google)"
            _syncStatus.value = msg
            return FirebaseSyncResult(false, msg)
        }
        if (!isNetworkAvailable(appContext)) {
            val msg = "⏳ အင်တာနက် မရှိသေးပါ — လိုင်းရရှိသည်နှင့် Firebase သို့ အလိုအလျောက် Sync တင်ပါမည်"
            _syncStatus.value = msg
            return FirebaseSyncResult(false, msg)
        }

        return syncMutex.withLock {
            _isSyncing.value = true
            _syncStatus.value = if (!reason.isNullOrBlank()) {
                "Firebase Cloud သို့ POS ဒေတာအားလုံး Sync တင်နေပါသည် ($reason)..."
            } else {
                "Firebase Cloud သို့ POS ဒေတာအားလုံး Sync တင်နေပါသည်..."
            }
            try {
                val repo = FirebaseBackupRepository(appContext)
                val result = repo.backupAll(db, storeConfig ?: StoreConfig(appContext), storeName)
                if (result.success) {
                    val now = System.currentTimeMillis()
                    _lastSyncTimeEpochMs.value = now
                    runCatching { db.outboxDao().markPendingAsSynced(now) }
                    runCatching { WorkManager.getInstance(appContext).cancelUniqueWork("firebase-realtime-sync-queue") }
                }
                _syncStatus.value = result.message
                result
            } catch (e: CancellationException) {
                _syncStatus.value = "Firebase Sync ခေတ္တရပ်ဆိုင်းသွားပါသည်"
                throw e
            } catch (e: Exception) {
                val msg = "Firebase Sync မအောင်မြင်ပါ: ${e.localizedMessage ?: e.message}"
                _syncStatus.value = msg
                FirebaseSyncResult(false, msg)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    suspend fun restoreAll(
        context: Context,
        db: AppDatabase,
        storeConfig: StoreConfig? = null,
        replaceLocal: Boolean = true,
    ): FirebaseSyncResult {
        val appContext = context.applicationContext
        if (Firebase.auth.currentUser == null) {
            val msg = "Google Account Login မဝင်ရသေးပါ (Please sign in with Google)"
            _syncStatus.value = msg
            return FirebaseSyncResult(false, msg)
        }
        if (!isNetworkAvailable(appContext)) {
            val msg = "အင်တာနက် ချိတ်ဆက်မှု မရှိပါ — လိုင်းဖွင့်ပြီး ထပ်မံကြိုးစားပါ"
            _syncStatus.value = msg
            return FirebaseSyncResult(false, msg)
        }

        return syncMutex.withLock {
            _isSyncing.value = true
            _syncStatus.value = "Firebase Cloud မှ POS ဒေတာအားလုံး Restore ပြန်ယူနေပါသည်..."
            try {
                val repo = FirebaseBackupRepository(appContext)
                val result = repo.restoreAll(db, storeConfig ?: StoreConfig(appContext), replaceLocal)
                if (result.success) {
                    val now = System.currentTimeMillis()
                    _lastSyncTimeEpochMs.value = now
                    runCatching { db.outboxDao().markPendingAsSynced(now) }
                }
                _syncStatus.value = result.message
                result
            } catch (e: CancellationException) {
                _syncStatus.value = "Firebase Restore ခေတ္တရပ်ဆိုင်းသွားပါသည်"
                throw e
            } catch (e: Exception) {
                val msg = "Firebase Restore မအောင်မြင်ပါ: ${e.localizedMessage ?: e.message}"
                _syncStatus.value = msg
                FirebaseSyncResult(false, msg)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncSaleBundleAsync(
        context: Context,
        db: AppDatabase,
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        payment: PaymentEntity?,
        updatedProducts: List<ProductEntity>,
        updatedCustomer: CustomerEntity? = null,
        scope: CoroutineScope,
    ) {
        val appContext = context.applicationContext
        if (Firebase.auth.currentUser == null) return
        if (!isNetworkAvailable(appContext)) {
            enqueueOfflineWorker(appContext)
            return
        }
        autoSyncScope.launch {
            val ok = FirebaseBackupRepository(appContext).syncSaleBundle(
                sale = sale,
                items = items,
                payment = payment,
                updatedProducts = updatedProducts,
                updatedCustomer = updatedCustomer,
            )
            if (ok) {
                val now = System.currentTimeMillis()
                runCatching { db.outboxDao().markAggregateAsSynced(sale.saleId, now) }
                _lastSyncTimeEpochMs.value = now
            } else {
                triggerRealtimeSync(appContext, db, reason = "Sale ${sale.saleId}")
            }
        }
    }
}
