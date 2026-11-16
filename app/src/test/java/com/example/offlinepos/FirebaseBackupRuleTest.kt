package com.example.offlinepos

import androidx.room.Room
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
import com.example.offlinepos.firebase.FirebaseBackupRepository
import com.example.offlinepos.ui.PromoCode
import com.example.offlinepos.ui.StoreConfig
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class FirebaseBackupRuleTest : FirestoreEmulatorTestBase() {

    private fun createInMemoryDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Test
    fun backupAll_and_restoreAll_fullRoundTrip_succeedsAcrossAllCollections() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repo = FirebaseBackupRepository(firestore, auth)
        val sourceDb = createInMemoryDb()
        val restoredDb = createInMemoryDb()
        val now = System.currentTimeMillis()
        val saleId = "sale-${UUID.randomUUID()}"

        try {
            val storeConfig = StoreConfig(appContext)
            storeConfig.saveProfile(
                storeConfig.getProfile().copy(
                    storeName = "San Thit Cloud Store",
                    address = "Yangon Test Road",
                    phone = "09-12345678",
                )
            )
            storeConfig.setCurrencySymbol("MMK")
            storeConfig.savePromoCode(PromoCode(code = "CLOUD10", discountPercent = 10.0, description = "10% off"))

            sourceDb.productDao().upsert(
                ProductEntity(
                    productId = 101L,
                    name = "Steel Pipe 12ft",
                    sku = "SP-101",
                    barcode = "885000101",
                    costPriceMinor = 8000L,
                    sellPriceMinor = 10500L,
                    stockQuantity = 13L,
                    stockQuantityDecimal = 12.5,
                    updatedAtEpochMs = now,
                )
            )
            sourceDb.customerDao().insertCustomer(
                CustomerEntity(
                    customerId = 201L,
                    name = "U Kyaw",
                    phone = "09987654321",
                    currentDebtMinor = 5000L,
                    createdAtEpochMs = now,
                )
            )
            sourceDb.supplierDao().insertSupplier(
                SupplierEntity(
                    supplierId = 301L,
                    name = "Golden Metal Co",
                    phone = "09111222333",
                    createdAtEpochMs = now,
                )
            )
            sourceDb.receiptTemplateDao().insertOrUpdate(
                ReceiptTemplateEntity(
                    templateId = 401,
                    name = "Thermal 80mm Cloud",
                    isDefault = true,
                    updatedAtEpochMs = now,
                )
            )
            sourceDb.checkoutDao().upsertSale(
                SaleEntity(
                    saleId = saleId,
                    businessDate = "2026-11-16",
                    subtotalMinor = 21000L,
                    discountMinor = 0L,
                    taxMinor = 0L,
                    totalMinor = 21000L,
                    status = "COMPLETED",
                    cashierId = "Admin",
                    deviceId = "POS-01",
                    createdAtEpochMs = now,
                    customerId = 201L,
                    customerName = "U Kyaw",
                    paymentMethod = "CASH",
                )
            )
            sourceDb.checkoutDao().upsertSaleItems(
                listOf(
                    SaleItemEntity(
                        lineId = 1L,
                        saleId = saleId,
                        productId = 101L,
                        productNameSnapshot = "Steel Pipe 12ft",
                        unitPriceMinor = 10500L,
                        quantity = 2L,
                        lineDiscountMinor = 0L,
                        lineTaxMinor = 0L,
                        lineTotalMinor = 21000L,
                        unitName = "ချောင်း",
                        quantityDecimal = 2.0,
                    )
                )
            )
            sourceDb.checkoutDao().upsertPayments(
                listOf(
                    PaymentEntity(
                        paymentId = "pay-${UUID.randomUUID()}",
                        saleId = saleId,
                        method = "CASH",
                        tenderedMinor = 25000L,
                        changeMinor = 4000L,
                        authorizedMinor = 21000L,
                        authorizationState = "APPROVED",
                        providerReference = null,
                        settlementState = "SETTLED",
                        manualRecorded = false,
                        recordedByUserId = "Admin",
                        createdAtEpochMs = now,
                    )
                )
            )
            sourceDb.customerDao().insertDebtTransaction(
                DebtTransactionEntity(
                    txId = 501L,
                    customerId = 201L,
                    saleId = saleId,
                    type = "CREDIT_SALE",
                    amountMinor = 5000L,
                    remainingBalanceMinor = 5000L,
                    note = "Partial credit",
                    cashierId = "Admin",
                    createdAtEpochMs = now,
                )
            )
            sourceDb.operationsDao().insertPurchase(
                PurchaseEntity(
                    purchaseId = 601L,
                    productId = 101L,
                    productName = "Steel Pipe 12ft",
                    supplierName = "Golden Metal Co",
                    quantity = 20L,
                    unitCostMinor = 8000L,
                    totalCostMinor = 160000L,
                    createdAtEpochMs = now,
                )
            )
            sourceDb.checkoutDao().upsertReturnTransactions(
                listOf(
                    ReturnTransactionEntity(
                        returnId = "ret-${UUID.randomUUID()}",
                        saleId = saleId,
                        returnedAmountMinor = 10500L,
                        refundMethod = "CASH",
                        refundReference = null,
                        reason = "Damaged",
                        actorId = "Admin",
                        createdAtEpochMs = now,
                    )
                )
            )
            sourceDb.operationsDao().insertShift(
                ShiftReportEntity(
                    shiftId = 701L,
                    cashierName = "Admin",
                    openTimeEpochMs = now - 3600_000L,
                    closeTimeEpochMs = now,
                    openingCashMinor = 100000L,
                    cashSalesMinor = 21000L,
                    digitalSalesMinor = 0L,
                    creditSalesMinor = 0L,
                    expenseMinor = 0L,
                    expectedCashMinor = 121000L,
                    actualCashCountedMinor = 121000L,
                    differenceMinor = 0L,
                    status = "CLOSED",
                )
            )
            sourceDb.checkoutDao().upsertStockMovements(
                listOf(
                    StockMovementEntity(
                        movementId = "mov-${UUID.randomUUID()}",
                        productId = 101L,
                        movementType = "SALE",
                        quantityChange = -2L,
                        quantityBefore = 15L,
                        quantityAfter = 13L,
                        referenceId = saleId,
                        reason = "POS Sale",
                        actorId = "Admin",
                        createdAtEpochMs = now,
                    )
                )
            )

            val backupRes = withTimeout(OP_TIMEOUT_MS) {
                repo.backupAll(sourceDb, storeConfig)
            }
            assertTrue("Expected backup success but got: ${backupRes.message}", backupRes.success)
            assertEquals(1, backupRes.productsSynced)
            assertTrue(backupRes.salesSynced >= 1)
            assertTrue(backupRes.saleItemsSynced >= 1)
            assertEquals(1, backupRes.customersSynced)

            // Reset StoreConfig before Full Restore to verify config restoration
            storeConfig.saveProfile(storeConfig.getProfile().copy(storeName = "Reset Store"))
            storeConfig.setCurrencySymbol("USD")

            val restoreRes = withTimeout(OP_TIMEOUT_MS) {
                repo.restoreAll(restoredDb, storeConfig, replaceLocal = true)
            }
            assertTrue("Expected restore success but got: ${restoreRes.message}", restoreRes.success)

            val restoredProducts = restoredDb.productDao().getAll()
            val restoredPipe = restoredProducts.find { it.productId == 101L }
            assertNotNull(restoredPipe)
            assertEquals("Steel Pipe 12ft", restoredPipe!!.name)
            assertEquals(12.5, restoredPipe.stockQuantityDecimal ?: 0.0, 0.001)

            val restoredSale = restoredDb.checkoutDao().getAllSales().find { it.saleId == saleId }
            assertNotNull(restoredSale)
            assertEquals(21000L, restoredSale!!.totalMinor)

            val restoredItems = restoredDb.checkoutDao().getItemsForSale(saleId)
            assertEquals(1, restoredItems.size)
            assertEquals("Steel Pipe 12ft", restoredItems.first().productNameSnapshot)

            val restoredPayment = restoredDb.checkoutDao().getPaymentForSale(saleId)
            assertNotNull(restoredPayment)
            assertEquals(25000L, restoredPayment!!.tenderedMinor)

            assertTrue(restoredDb.customerDao().getAllCustomers().any { it.customerId == 201L && it.name == "U Kyaw" })
            assertTrue(restoredDb.customerDao().getAllDebtTransactions().any { it.txId == 501L && it.amountMinor == 5000L })
            assertTrue(restoredDb.supplierDao().getAllSuppliersSync().any { it.supplierId == 301L && it.name == "Golden Metal Co" })
            assertTrue(restoredDb.operationsDao().getAllPurchases().any { it.purchaseId == 601L && it.totalCostMinor == 160000L })
            assertTrue(restoredDb.checkoutDao().getAllReturnTransactions().any { it.saleId == saleId && it.returnedAmountMinor == 10500L })
            assertTrue(restoredDb.operationsDao().getAllShifts().any { it.shiftId == 701L && it.cashierName == "Admin" })
            assertTrue(restoredDb.checkoutDao().getAllStockMovements().any { it.referenceId == saleId && it.quantityChange == -2L })
            assertTrue(restoredDb.receiptTemplateDao().getAllTemplatesSync().any { it.templateId == 401 && it.name == "Thermal 80mm Cloud" })

            assertEquals("San Thit Cloud Store", storeConfig.getProfile().storeName)
            assertEquals("MMK", storeConfig.getCurrencySymbol())
            assertTrue(storeConfig.getPromoCodes().any { it.code == "CLOUD10" })
        } finally {
            sourceDb.close()
            restoredDb.close()
        }
    }

    @Test
    fun syncSaleBundle_authenticatedOwner_writesSaleAndItems() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FirebaseBackupRepository(firestore, auth)
        val saleId = "bundle-${UUID.randomUUID()}"
        val now = System.currentTimeMillis()

        val sale = SaleEntity(
            saleId = saleId,
            businessDate = "2026-11-16",
            subtotalMinor = 5000L,
            discountMinor = 0L,
            taxMinor = 0L,
            totalMinor = 5000L,
            status = "COMPLETED",
            cashierId = "Admin",
            deviceId = "POS-01",
            createdAtEpochMs = now,
            paymentMethod = "CASH",
        )
        val item = SaleItemEntity(
            lineId = 7L,
            saleId = saleId,
            productId = 101L,
            productNameSnapshot = "Cement Bag",
            unitPriceMinor = 5000L,
            quantity = 1L,
            lineDiscountMinor = 0L,
            lineTaxMinor = 0L,
            lineTotalMinor = 5000L,
            quantityDecimal = 1.0,
        )
        val prod = ProductEntity(
            productId = 101L,
            name = "Cement Bag",
            sku = "CEM-01",
            barcode = null,
            costPriceMinor = 4000L,
            sellPriceMinor = 5000L,
            stockQuantity = 9L,
            updatedAtEpochMs = now,
        )

        withTimeout(OP_TIMEOUT_MS) {
            repo.syncSaleBundle(sale, listOf(item), null, listOf(prod), null)
        }

        val snap = withTimeout(OP_TIMEOUT_MS) {
            firestore.collection("users").document(uid).collection("sales").document(saleId).get().await()
        }
        assertTrue(snap.exists())
        assertEquals(5000L, snap.getLong("totalMinor"))
    }

    @Test
    fun crossUserRead_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirebaseBackupRepository(firestore, auth)
        val db = createInMemoryDb()
        try {
            db.productDao().upsert(
                ProductEntity(
                    productId = 909L,
                    name = "Alice Private Item",
                    sku = "ALICE-909",
                    barcode = null,
                    costPriceMinor = 100L,
                    sellPriceMinor = 200L,
                    stockQuantity = 5L,
                    updatedAtEpochMs = System.currentTimeMillis(),
                )
            )
            val res = withTimeout(OP_TIMEOUT_MS) { aliceRepo.backupAll(db) }
            assertTrue(res.success)
        } finally {
            db.close()
        }

        signInTestUser(BOB_EMAIL)
        try {
            withTimeout(OP_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid)
                    .collection("products").get().await()
            }
            fail("Expected PERMISSION_DENIED when Bob reads Alice's products")
        } catch (e: Exception) {
            val ffe = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", ffe)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe!!.code)
        }
    }

    @Test
    fun unauthenticatedRead_and_restore_failCleanly() = runBlocking {
        auth.signOut()
        val repo = FirebaseBackupRepository(firestore, auth)
        val db = createInMemoryDb()
        try {
            val restoreRes = repo.restoreAll(db, replaceLocal = true)
            assertFalse(restoreRes.success)

            try {
                withTimeout(OP_TIMEOUT_MS) {
                    firestore.collection("users").document("some_user")
                        .collection("products").get().await()
                }
                fail("Expected PERMISSION_DENIED for unauthenticated read")
            } catch (e: Exception) {
                val ffe = e.unwrapFirestoreException()
                assertNotNull("Expected FirebaseFirestoreException, got $e", ffe)
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ffe!!.code)
            }
        } finally {
            db.close()
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_pos_backup@test.com"
        const val BOB_EMAIL = "bob_pos_backup@test.com"
        const val OP_TIMEOUT_MS = 15000L
    }
}
