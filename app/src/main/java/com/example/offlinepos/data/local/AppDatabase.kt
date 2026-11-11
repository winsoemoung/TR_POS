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
        SupplierEntity::class,
        ReceiptTemplateEntity::class,
    ],
    version = 15,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun checkoutDao(): CheckoutDao
    abstract fun customerDao(): CustomerDao
    abstract fun operationsDao(): OperationsDao
    abstract fun outboxDao(): OutboxDao
    abstract fun supplierDao(): SupplierDao
    abstract fun receiptTemplateDao(): ReceiptTemplateDao

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
                ensureColumnExists(db, "products", "wholesaleUnitName", "TEXT NOT NULL DEFAULT 'ဖာ'")
                ensureColumnExists(db, "products", "wholesaleUnitMultiplier", "INTEGER NOT NULL DEFAULT 1")
                ensureColumnExists(db, "sales", "deliveryFeeMinor", "INTEGER NOT NULL DEFAULT 0")
                ensureColumnExists(db, "sales", "pickupStatus", "TEXT NOT NULL DEFAULT 'PICKED_UP'")
                ensureColumnExists(db, "sales", "expectedPickupDate", "TEXT")
                ensureColumnExists(db, "sales", "pickupNote", "TEXT")
                ensureColumnExists(db, "sale_items", "returnedQuantity", "INTEGER NOT NULL DEFAULT 0")
                ensureColumnExists(db, "customers", "creditDueDateEpochMs", "INTEGER")

                // Ensure all performance indices exist
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_createdAtEpochMs` ON `sales` (`createdAtEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_businessDate` ON `sales` (`businessDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_customerId` ON `sales` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_status` ON `sales` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_pickupStatus` ON `sales` (`pickupStatus`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_sku` ON `products` (`sku`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_category` ON `products` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_isActive` ON `products` (`isActive`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_updatedAtEpochMs` ON `products` (`updatedAtEpochMs`)")

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_phone` ON `customers` (`phone`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_currentDebtMinor` ON `customers` (`currentDebtMinor`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `suppliers` (
                        `supplierId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `shopName` TEXT NOT NULL DEFAULT '',
                        `phone` TEXT NOT NULL DEFAULT '',
                        `email` TEXT NOT NULL DEFAULT '',
                        `address` TEXT NOT NULL DEFAULT '',
                        `taxNumber` TEXT NOT NULL DEFAULT '',
                        `note` TEXT NOT NULL DEFAULT '',
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_suppliers_name` ON `suppliers` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_suppliers_shopName` ON `suppliers` (`shopName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_suppliers_phone` ON `suppliers` (`phone`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `receipt_templates` (
                        `templateId` INTEGER PRIMARY KEY NOT NULL,
                        `name` TEXT NOT NULL,
                        `isDefault` INTEGER NOT NULL DEFAULT 0,
                        `showLogo` INTEGER NOT NULL DEFAULT 1,
                        `storeNameFontSize` TEXT NOT NULL DEFAULT 'LARGE',
                        `storeNameAlign` TEXT NOT NULL DEFAULT 'CENTER',
                        `headerDetailsFontSize` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `showAddress` INTEGER NOT NULL DEFAULT 1,
                        `showPhone` INTEGER NOT NULL DEFAULT 1,
                        `showHeaderNote` INTEGER NOT NULL DEFAULT 1,
                        `customHeaderNote` TEXT NOT NULL DEFAULT '',
                        `showCashierInfo` INTEGER NOT NULL DEFAULT 1,
                        `showVoucherNumber` INTEGER NOT NULL DEFAULT 1,
                        `showCustomerInfo` INTEGER NOT NULL DEFAULT 1,
                        `customerLabel` TEXT NOT NULL DEFAULT 'ဈေးဝယ်သူ',
                        `metaFontSize` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `itemNameWidthWeight` REAL NOT NULL DEFAULT 0.42,
                        `qtyWidthWeight` REAL NOT NULL DEFAULT 0.16,
                        `priceWidthWeight` REAL NOT NULL DEFAULT 0.20,
                        `amountWidthWeight` REAL NOT NULL DEFAULT 0.22,
                        `rowPaddingDp` INTEGER NOT NULL DEFAULT 4,
                        `gridLineStyle` TEXT NOT NULL DEFAULT 'DASHED',
                        `bodyFontSize` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `itemNameAlign` TEXT NOT NULL DEFAULT 'LEFT',
                        `qtyAlign` TEXT NOT NULL DEFAULT 'CENTER',
                        `priceAlign` TEXT NOT NULL DEFAULT 'RIGHT',
                        `amountAlign` TEXT NOT NULL DEFAULT 'RIGHT',
                        `totalsFontSize` TEXT NOT NULL DEFAULT 'MEDIUM',
                        `paperWidthDots` INTEGER NOT NULL DEFAULT 576,
                        `itemLayoutStyle` TEXT NOT NULL DEFAULT 'STANDARD_2LINE',
                        `customStoreName` TEXT NOT NULL DEFAULT '',
                        `customSubtitle` TEXT NOT NULL DEFAULT '',
                        `customOwnerOrContact` TEXT NOT NULL DEFAULT '',
                        `customPhone` TEXT NOT NULL DEFAULT '',
                        `customAddress` TEXT NOT NULL DEFAULT '',
                        `extraNoticeText` TEXT NOT NULL DEFAULT '',
                        `showPrintTime` INTEGER NOT NULL DEFAULT 1,
                        `fontScalePercent` INTEGER NOT NULL DEFAULT 100,
                        `lineSpacingDp` INTEGER NOT NULL DEFAULT 4,
                        `showTotalsBreakdown` INTEGER NOT NULL DEFAULT 1,
                        `showPaymentMethod` INTEGER NOT NULL DEFAULT 1,
                        `showBarcodeOrQr` INTEGER NOT NULL DEFAULT 1,
                        `footerMessage` TEXT NOT NULL DEFAULT '',
                        `returnPolicyText` TEXT NOT NULL DEFAULT '',
                        `footerAlign` TEXT NOT NULL DEFAULT 'CENTER',
                        `footerFontSize` TEXT NOT NULL DEFAULT 'SMALL',
                        `updatedAtEpochMs` INTEGER NOT NULL
                    )
                """)

                ensureColumnExists(db, "receipt_templates", "paperWidthDots", "INTEGER NOT NULL DEFAULT 576")
                ensureColumnExists(db, "receipt_templates", "itemLayoutStyle", "TEXT NOT NULL DEFAULT 'STANDARD_2LINE'")
                ensureColumnExists(db, "receipt_templates", "customStoreName", "TEXT NOT NULL DEFAULT ''")
                ensureColumnExists(db, "receipt_templates", "customSubtitle", "TEXT NOT NULL DEFAULT ''")
                ensureColumnExists(db, "receipt_templates", "customOwnerOrContact", "TEXT NOT NULL DEFAULT ''")
                ensureColumnExists(db, "receipt_templates", "customPhone", "TEXT NOT NULL DEFAULT ''")
                ensureColumnExists(db, "receipt_templates", "customAddress", "TEXT NOT NULL DEFAULT ''")
                ensureColumnExists(db, "receipt_templates", "customerLabel", "TEXT NOT NULL DEFAULT 'ဈေးဝယ်သူ'")
                ensureColumnExists(db, "receipt_templates", "extraNoticeText", "TEXT NOT NULL DEFAULT ''")
                ensureColumnExists(db, "receipt_templates", "showPrintTime", "INTEGER NOT NULL DEFAULT 1")
                ensureColumnExists(db, "receipt_templates", "headerDetailsFontSize", "TEXT NOT NULL DEFAULT 'MEDIUM'")
                ensureColumnExists(db, "receipt_templates", "metaFontSize", "TEXT NOT NULL DEFAULT 'MEDIUM'")
                ensureColumnExists(db, "receipt_templates", "totalsFontSize", "TEXT NOT NULL DEFAULT 'MEDIUM'")
                ensureColumnExists(db, "receipt_templates", "fontScalePercent", "INTEGER NOT NULL DEFAULT 100")
                ensureColumnExists(db, "receipt_templates", "lineSpacingDp", "INTEGER NOT NULL DEFAULT 4")

                // Update all templates 1 to 5 to default to 80mm (576 dots)
                db.execSQL("UPDATE `receipt_templates` SET `paperWidthDots` = 576 WHERE `paperWidthDots` = 384")

                // Ensure initial 5 receipt template slots exist
                runCatching {
                    val countCursor = db.query("SELECT COUNT(*) FROM `receipt_templates`")
                    var count = 0
                    if (countCursor.moveToFirst()) count = countCursor.getInt(0)
                    countCursor.close()
                    if (count == 0) {
                        db.execSQL("""
                            INSERT INTO `receipt_templates` (
                                templateId, name, isDefault, paperWidthDots, itemLayoutStyle, showLogo,
                                customStoreName, customSubtitle, customOwnerOrContact, customPhone, customAddress,
                                storeNameFontSize, storeNameAlign, headerDetailsFontSize, showAddress, showPhone, showHeaderNote,
                                customHeaderNote, showCashierInfo, showVoucherNumber, showCustomerInfo, customerLabel, metaFontSize,
                                itemNameWidthWeight, qtyWidthWeight, priceWidthWeight, amountWidthWeight,
                                rowPaddingDp, gridLineStyle, bodyFontSize, itemNameAlign, qtyAlign, priceAlign, amountAlign,
                                totalsFontSize, fontScalePercent, lineSpacingDp,
                                showTotalsBreakdown, showPaymentMethod, showBarcodeOrQr, footerMessage, returnPolicyText,
                                extraNoticeText, showPrintTime, footerAlign, footerFontSize, updatedAtEpochMs
                            ) VALUES
                            (1, 'Template 1 - နမူနာပြေစာ စံပုံစံ (80mm Sample Style)', 1, 576, 'STANDARD_2LINE', 0,
                             'ဆန်းသစ်', 'အိမ်ဆောက်ပစ္စည်း အရောင်းဆိုင်', 'ကိုပါပီး+မသီရိကလျာ',
                             '09678473426/09943070451/09442387883/' || char(10) || '09400511789',
                             '3/3 ဗိုလ်ချုပ်လမ်း / စမ်းမြို့ရပ်ကွက် / ကလေးမြို့',
                             'LARGE', 'CENTER', 'MEDIUM', 1, 1, 0, '', 0, 1, 1, 'ဈေးဝယ်သူ', 'MEDIUM',
                             0.42, 0.16, 0.20, 0.22, 4, 'DASHED', 'MEDIUM', 'LEFT', 'CENTER', 'RIGHT', 'RIGHT',
                             'MEDIUM', 100, 4,
                             1, 0, 0, 'ဝယ်ယူအားပေးမှုကိုကျေးဇူးအထူးတင်ရှိပါသည်', 'ဝယ်ယူပြီး ပစ္စည်း များကို ပြန်လဲမပေးပါ',
                             'သုံးမူး၊ ငါးမူး၊ ခြောက်မူး သံပင် များ ၊' || char(10) || 'သွပ် အစားထိုး polystar များ ရပါသည် ။',
                             1, 'CENTER', 'SMALL', ${System.currentTimeMillis()}),
                            (2, 'Template 2 - မျဉ်းမပါ သန့်ရှင်းသော ပုံစံ (Clean Borderless 80mm)', 0, 576, 'COLUMNS_4', 1,
                             '', '', '', '', '',
                             'LARGE', 'CENTER', 'MEDIUM', 1, 1, 1, 'တရားဝင် အရောင်းပြေစာ', 1, 1, 1, 'ဝယ်သူအမည်', 'MEDIUM',
                             0.40, 0.15, 0.20, 0.25, 4, 'NONE', 'MEDIUM', 'LEFT', 'CENTER', 'RIGHT', 'RIGHT',
                             'MEDIUM', 100, 4,
                             1, 1, 0, 'ကျေးဇူးတင်ပါသည်', 'ဝယ်ယူပြီးပစ္စည်း ပြန်မလဲပေးပါ',
                             '', 0, 'CENTER', 'SMALL', ${System.currentTimeMillis()}),
                            (3, 'Template 3 - အရွယ်စံ ပုံမှန် (Standard 80mm)', 0, 576, 'COLUMNS_4', 0,
                             '', '', '', '', '',
                             'MEDIUM', 'LEFT', 'MEDIUM', 1, 1, 0, '', 1, 1, 0, 'ဝယ်သူ', 'MEDIUM',
                             0.45, 0.15, 0.18, 0.22, 4, 'DASHED', 'MEDIUM', 'LEFT', 'CENTER', 'RIGHT', 'RIGHT',
                             'MEDIUM', 100, 4,
                             1, 1, 0, 'Thank you!', '', '', 0, 'CENTER', 'SMALL', ${System.currentTimeMillis()}),
                            (4, 'Template 4 - ပို့ဆောင်ရေး/ဂိုဒေါင် (Delivery Voucher 80mm)', 0, 576, 'COLUMNS_4', 1,
                             '', '', '', '', '',
                             'XLARGE', 'CENTER', 'MEDIUM', 1, 1, 1, 'ပစ္စည်း ပို့ဆောင်ရေး ပြေစာ', 1, 1, 1, 'လက်ခံသူအမည်', 'MEDIUM',
                             0.50, 0.20, 0.00, 0.30, 6, 'DOUBLE', 'LARGE', 'LEFT', 'CENTER', 'RIGHT', 'RIGHT',
                             'LARGE', 100, 5,
                             1, 1, 0, 'ပစ္စည်း လက်ခံရရှိကြောင်း လက်မှတ်ရေးထိုးပါ', 'ဂိုဒေါင်ထုတ်စလစ်', '', 1, 'CENTER', 'MEDIUM', ${System.currentTimeMillis()}),
                            (5, 'Template 5 - လက်ကား အထူးပြေစာ (Wholesale Invoice 80mm)', 0, 576, 'COLUMNS_4', 1,
                             '', '', '', '', '',
                             'LARGE', 'CENTER', 'MEDIUM', 1, 1, 1, 'လက်ကား သီးသန့် ရှင်းတမ်း', 1, 1, 1, 'ဆိုင်အမည်/ဝယ်သူ', 'MEDIUM',
                             0.38, 0.18, 0.20, 0.24, 4, 'SOLID', 'MEDIUM', 'LEFT', 'CENTER', 'RIGHT', 'RIGHT',
                             'MEDIUM', 100, 4,
                             1, 1, 0, 'လက်ကား ဝယ်ယူမှုအတွက် ကျေးဇူးတင်ပါသည်', 'ငွေလွှဲပြေစာအား ထိန်းသိမ်းထားပါ', '', 1, 'CENTER', 'SMALL', ${System.currentTimeMillis()})
                        """)
                    } else {
                        // Apply default 80mm and user updates to all templates
                        db.execSQL("""
                            UPDATE `receipt_templates` SET
                                paperWidthDots = 576
                            WHERE paperWidthDots = 384
                        """)
                    }
                }

                // Ensure initial default suppliers if empty
                runCatching {
                    val supCursor = db.query("SELECT COUNT(*) FROM `suppliers`")
                    var sCount = 0
                    if (supCursor.moveToFirst()) sCount = supCursor.getInt(0)
                    supCursor.close()
                    if (sCount == 0) {
                        db.execSQL("""
                            INSERT INTO `suppliers` (name, shopName, phone, email, address, note, createdAtEpochMs) VALUES
                            ('ဦးမောင်မောင်', 'ရွှေမြန်မာ ကုန်မာလက်ကား', '09-450011223', 'shwemyanmar@pos.com', 'အမှတ် (၁၂)၊ ဘုရင့်နောင်လမ်း၊ ရန်ကုန်', 'အိမ်ဆောက်ပစ္စည်းနှင့် ကုန်မာလက်ကား', ${System.currentTimeMillis()}),
                            ('ဒေါ်နီနီ', 'အောင်သမာဓိ ဖြန့်ချိရေး', '09-790033445', 'aungtharmardi@pos.com', 'အမှတ် (၈၈)၊ စက်မှုဇုန် (၁)၊ မန္တလေး', 'ဘိလပ်မြေနှင့် သံချောင်း တိုက်ရိုက်ကိုယ်စားလှယ်', ${System.currentTimeMillis()}),
                            ('ကိုကျော်စွာ', 'Asia General Distribution', '09-250066778', 'asia_dist@pos.com', 'အမှတ် (၄၅)၊ လမ်းမတော်၊ ရန်ကုန်', 'လျှပ်စစ်နှင့် ရေပိုက်ပစ္စည်းများ', ${System.currentTimeMillis()})
                        """)
                    }
                }

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchases_createdAtEpochMs` ON `purchases` (`createdAtEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_purchases_supplierName` ON `purchases` (`supplierName`)")
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

        fun vacuumAndOptimizeDatabase(context: Context): DatabaseMaintenanceResult {
            return try {
                val db = get(context).openHelper.writableDatabase
                // 1. Flush Write-Ahead Log
                db.query("PRAGMA wal_checkpoint(FULL)").close()
                // 2. Reclaim free pages and rebuild B-Trees
                db.execSQL("VACUUM")
                // 3. Update SQLite statistics for query planner
                db.execSQL("PRAGMA optimize")
                
                val dbFile = context.getDatabasePath(DB_NAME)
                val sizeBytes = if (dbFile.exists()) dbFile.length() else 0L
                val formatted = formatFileSize(sizeBytes)
                DatabaseMaintenanceResult(
                    isSuccess = true,
                    sizeFormatted = formatted,
                    message = "Database ဖိုင်ဆိုက်ကို အောင်မြင်စွာ VACUUM & Optimize ပြုလုပ်ပြီးပါပြီ ($formatted)"
                )
            } catch (e: Exception) {
                DatabaseMaintenanceResult(
                    isSuccess = false,
                    sizeFormatted = "-",
                    message = "Database optimize လုပ်ဆောင်ရာတွင် အမှားဖြစ်ပေါ်ခဲ့သည်: ${e.message}"
                )
            }
        }

        fun getDatabaseFileSize(context: Context): String {
            val dbFile = context.getDatabasePath(DB_NAME)
            val sizeBytes = if (dbFile.exists()) dbFile.length() else 0L
            return formatFileSize(sizeBytes)
        }

        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
                bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
                else -> "$bytes Bytes"
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

data class DatabaseMaintenanceResult(
    val isSuccess: Boolean,
    val sizeFormatted: String,
    val message: String,
)
