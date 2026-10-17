package com.example.offlinepos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PaymentEntity::class,
        AuditEventEntity::class,
        CustomerEntity::class,
        DebtTransactionEntity::class,
        PurchaseEntity::class,
        ShiftReportEntity::class,
        ParkedOrderEntity::class,
        StockMovementEntity::class,
        ReturnTransactionEntity::class,
        OutboxEntity::class,
        SyncTombstoneEntity::class,
    ],
    version = 11,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun checkoutDao(): CheckoutDao
    abstract fun customerDao(): CustomerDao
    abstract fun operationsDao(): OperationsDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        private const val DB_NAME = "offline_pos.db"
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            ensureAllTablesAndColumnsExist(db)
                        }
                    })
                    .build().also { INSTANCE = it }
            }

        private fun ensureAllTablesAndColumnsExist(db: SupportSQLiteDatabase) {
            runCatching {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `customers` (
                        `customerId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `creditLimitMinor` INTEGER NOT NULL,
                        `currentDebtMinor` INTEGER NOT NULL,
                        `creditDueDateEpochMs` INTEGER,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `debt_transactions` (
                        `txId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `customerId` INTEGER NOT NULL,
                        `saleId` TEXT,
                        `type` TEXT NOT NULL,
                        `amountMinor` INTEGER NOT NULL,
                        `remainingBalanceMinor` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `cashierId` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_transactions_customerId` ON `debt_transactions` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_transactions_saleId` ON `debt_transactions` (`saleId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `purchases` (
                        `purchaseId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `productName` TEXT NOT NULL,
                        `supplierName` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `unitCostMinor` INTEGER NOT NULL,
                        `totalCostMinor` INTEGER NOT NULL,
                        `invoiceRef` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchases_productId` ON `purchases` (`productId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shifts` (
                        `shiftId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cashierName` TEXT NOT NULL,
                        `openTimeEpochMs` INTEGER NOT NULL,
                        `closeTimeEpochMs` INTEGER NOT NULL,
                        `openingCashMinor` INTEGER NOT NULL,
                        `cashSalesMinor` INTEGER NOT NULL,
                        `digitalSalesMinor` INTEGER NOT NULL,
                        `creditSalesMinor` INTEGER NOT NULL,
                        `expenseMinor` INTEGER NOT NULL,
                        `expectedCashMinor` INTEGER NOT NULL,
                        `actualCashCountedMinor` INTEGER NOT NULL,
                        `differenceMinor` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `note` TEXT NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `parked_orders` (
                        `parkId` TEXT NOT NULL,
                        `tag` TEXT NOT NULL,
                        `itemsJson` TEXT NOT NULL,
                        `totalMinor` INTEGER NOT NULL,
                        `itemCount` INTEGER NOT NULL,
                        `customerName` TEXT,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`parkId`)
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `stock_movements` (
                        `movementId` TEXT NOT NULL,
                        `productId` INTEGER NOT NULL,
                        `movementType` TEXT NOT NULL,
                        `quantityChange` INTEGER NOT NULL,
                        `quantityBefore` INTEGER NOT NULL,
                        `quantityAfter` INTEGER NOT NULL,
                        `referenceId` TEXT,
                        `reason` TEXT,
                        `actorId` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`movementId`)
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_productId` ON `stock_movements` (`productId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_createdAtEpochMs` ON `stock_movements` (`createdAtEpochMs`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `return_transactions` (
                        `returnId` TEXT NOT NULL,
                        `saleId` TEXT NOT NULL,
                        `returnedAmountMinor` INTEGER NOT NULL,
                        `refundMethod` TEXT NOT NULL,
                        `refundReference` TEXT,
                        `reason` TEXT,
                        `actorId` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`returnId`)
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_return_transactions_saleId` ON `return_transactions` (`saleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_return_transactions_createdAtEpochMs` ON `return_transactions` (`createdAtEpochMs`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_outbox` (
                        `operationId` TEXT NOT NULL,
                        `idempotencyKey` TEXT NOT NULL,
                        `aggregateType` TEXT NOT NULL,
                        `aggregateId` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `schemaVersion` INTEGER NOT NULL,
                        `aggregateVersion` INTEGER NOT NULL,
                        `actorId` TEXT NOT NULL,
                        `deviceId` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `errorClass` TEXT,
                        `lastError` TEXT,
                        `retryCount` INTEGER NOT NULL,
                        `nextAttemptAtEpochMs` INTEGER NOT NULL,
                        `reconciliationResult` TEXT,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`operationId`)
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_outbox_state` ON `sync_outbox` (`state`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_outbox_operationId` ON `sync_outbox` (`operationId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `sync_tombstones` (
                        `tombstoneId` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `entityId` TEXT NOT NULL,
                        `deletedAtEpochMs` INTEGER NOT NULL,
                        `deletedByDeviceId` TEXT NOT NULL,
                        PRIMARY KEY(`tombstoneId`)
                    )
                """)
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_tombstones_entityType_entityId` ON `sync_tombstones` (`entityType`, `entityId`)")

                ensureColumnExists(db, "products", "category", "TEXT NOT NULL DEFAULT 'အထွေထွေ'")
                ensureColumnExists(db, "products", "isRoofingSheet", "INTEGER NOT NULL DEFAULT 0")
                ensureColumnExists(db, "products", "lowStockThreshold", "INTEGER NOT NULL DEFAULT 3")
                ensureColumnExists(db, "products", "imageUri", "TEXT")
                ensureColumnExists(db, "products", "wholesalePriceMinor", "INTEGER NOT NULL DEFAULT 0")
                ensureColumnExists(db, "products", "wholesaleMinQty", "INTEGER NOT NULL DEFAULT 0")
                ensureColumnExists(db, "products", "unitName", "TEXT NOT NULL DEFAULT 'ခု'")
                ensureColumnExists(db, "sales", "deliveryFeeMinor", "INTEGER NOT NULL DEFAULT 0")
                ensureColumnExists(db, "sale_items", "returnedQuantity", "INTEGER NOT NULL DEFAULT 0")
                ensureColumnExists(db, "customers", "creditDueDateEpochMs", "INTEGER")
            }
        }

        private fun ensureColumnExists(db: SupportSQLiteDatabase, table: String, column: String, definition: String) {
            runCatching {
                val cursor = db.query("PRAGMA table_info(`$table`)")
                var found = false
                val nameIdx = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIdx >= 0 && cursor.getString(nameIdx).equals(column, ignoreCase = true)) {
                        found = true
                        break
                    }
                }
                cursor.close()
                if (!found) {
                    db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
                }
            }
        }

        fun closeAndReset() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}
