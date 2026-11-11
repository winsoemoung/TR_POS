package com.example.offlinepos.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN category TEXT NOT NULL DEFAULT 'အထွေထွေ'")
        db.execSQL("ALTER TABLE products ADD COLUMN isRoofingSheet INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE products ADD COLUMN lowStockThreshold INTEGER NOT NULL DEFAULT 3")
        db.execSQL("ALTER TABLE products ADD COLUMN imageUri TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN wholesalePriceMinor INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE products ADD COLUMN wholesaleMinQty INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE products ADD COLUMN unitName TEXT NOT NULL DEFAULT 'ခု'")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sales ADD COLUMN deliveryFeeMinor INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS stock_movements (
            movementId TEXT NOT NULL PRIMARY KEY,
            productId INTEGER NOT NULL,
            movementType TEXT NOT NULL,
            quantityChange INTEGER NOT NULL,
            quantityBefore INTEGER NOT NULL,
            quantityAfter INTEGER NOT NULL,
            referenceId TEXT,
            reason TEXT,
            actorId TEXT NOT NULL,
            createdAtEpochMs INTEGER NOT NULL
        )""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_productId ON stock_movements(productId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_stock_movements_createdAtEpochMs ON stock_movements(createdAtEpochMs)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sale_items ADD COLUMN returnedQuantity INTEGER NOT NULL DEFAULT 0")
    }
}
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS return_transactions (
            returnId TEXT NOT NULL PRIMARY KEY,
            saleId TEXT NOT NULL,
            returnedAmountMinor INTEGER NOT NULL,
            refundMethod TEXT NOT NULL,
            refundReference TEXT,
            reason TEXT,
            actorId TEXT NOT NULL,
            createdAtEpochMs INTEGER NOT NULL
        )""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_return_transactions_saleId ON return_transactions(saleId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_return_transactions_createdAtEpochMs ON return_transactions(createdAtEpochMs)")
    }
}
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE customers ADD COLUMN creditDueDateEpochMs INTEGER")
    }
}
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS sync_outbox (
            operationId TEXT NOT NULL PRIMARY KEY,
            idempotencyKey TEXT NOT NULL,
            aggregateType TEXT NOT NULL,
            aggregateId TEXT NOT NULL,
            payloadJson TEXT NOT NULL,
            schemaVersion INTEGER NOT NULL,
            aggregateVersion INTEGER NOT NULL,
            actorId TEXT NOT NULL,
            deviceId TEXT NOT NULL,
            state TEXT NOT NULL,
            errorClass TEXT,
            lastError TEXT,
            retryCount INTEGER NOT NULL,
            nextAttemptAtEpochMs INTEGER NOT NULL,
            reconciliationResult TEXT,
            createdAtEpochMs INTEGER NOT NULL,
            updatedAtEpochMs INTEGER NOT NULL
        )""")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_state ON sync_outbox(state)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_outbox_operationId ON sync_outbox(operationId)")
    }
}
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS sync_tombstones (
            tombstoneId TEXT NOT NULL PRIMARY KEY,
            entityType TEXT NOT NULL,
            entityId TEXT NOT NULL,
            deletedAtEpochMs INTEGER NOT NULL,
            deletedByDeviceId TEXT NOT NULL
        )""")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_tombstones_entityType_entityId ON sync_tombstones(entityType, entityId)")
    }
}
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Repair new6-5 installations that created the old, non-Room index name.
        db.execSQL("DROP INDEX IF EXISTS index_sync_tombstones_entity")
        db.execSQL("DROP INDEX IF EXISTS index_sync_tombstones_entityType_entityId")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_sync_tombstones_entityType_entityId ON sync_tombstones(entityType, entityId)")

        // Ensure products table has all columns required by Room schema v11
        runCatching {
            val cursor = db.query("PRAGMA table_info(products)")
            val columns = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                val nameIdx = cursor.getColumnIndex("name")
                if (nameIdx >= 0) columns.add(cursor.getString(nameIdx))
            }
            cursor.close()
            if (!columns.contains("wholesalePriceMinor")) {
                db.execSQL("ALTER TABLE products ADD COLUMN wholesalePriceMinor INTEGER NOT NULL DEFAULT 0")
            }
            if (!columns.contains("wholesaleMinQty")) {
                db.execSQL("ALTER TABLE products ADD COLUMN wholesaleMinQty INTEGER NOT NULL DEFAULT 0")
            }
            if (!columns.contains("unitName")) {
                db.execSQL("ALTER TABLE products ADD COLUMN unitName TEXT NOT NULL DEFAULT 'ခု'")
            }
        }
    }
}
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add wholesale unit name and multiplier to products table
        runCatching {
            val cursor = db.query("PRAGMA table_info(products)")
            val columns = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                val nameIdx = cursor.getColumnIndex("name")
                if (nameIdx >= 0) columns.add(cursor.getString(nameIdx))
            }
            cursor.close()
            if (!columns.contains("wholesaleUnitName")) {
                db.execSQL("ALTER TABLE products ADD COLUMN wholesaleUnitName TEXT NOT NULL DEFAULT 'ဖာ'")
            }
            if (!columns.contains("wholesaleUnitMultiplier")) {
                db.execSQL("ALTER TABLE products ADD COLUMN wholesaleUnitMultiplier INTEGER NOT NULL DEFAULT 1")
            }
        }

        // 2. Performance Indices for rapid sales and inventory lookup
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_createdAtEpochMs ON sales(createdAtEpochMs)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_businessDate ON sales(businessDate)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_customerId ON sales(customerId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sales_status ON sales(status)")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_sku ON products(sku)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_category ON products(category)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_products_isActive ON products(isActive)")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_phone ON customers(phone)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_currentDebtMinor ON customers(currentDebtMinor)")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_createdAtEpochMs ON purchases(createdAtEpochMs)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_supplierName ON purchases(supplierName)")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
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
                `showAddress` INTEGER NOT NULL DEFAULT 1,
                `showPhone` INTEGER NOT NULL DEFAULT 1,
                `showHeaderNote` INTEGER NOT NULL DEFAULT 1,
                `customHeaderNote` TEXT NOT NULL DEFAULT '',
                `showCashierInfo` INTEGER NOT NULL DEFAULT 1,
                `showVoucherNumber` INTEGER NOT NULL DEFAULT 1,
                `showCustomerInfo` INTEGER NOT NULL DEFAULT 1,
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
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `paperWidthDots` INTEGER NOT NULL DEFAULT 384")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `itemLayoutStyle` TEXT NOT NULL DEFAULT 'STANDARD_2LINE'")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `customStoreName` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `customSubtitle` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `customOwnerOrContact` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `customPhone` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `customAddress` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `customerLabel` TEXT NOT NULL DEFAULT 'ဈေးဝယ်သူ'")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `extraNoticeText` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `showPrintTime` INTEGER NOT NULL DEFAULT 1")
    }
}

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `fontScalePercent` INTEGER NOT NULL DEFAULT 100")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `lineSpacingDp` INTEGER NOT NULL DEFAULT 4")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `headerDetailsFontSize` TEXT NOT NULL DEFAULT 'MEDIUM'")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `metaFontSize` TEXT NOT NULL DEFAULT 'MEDIUM'")
        db.execSQL("ALTER TABLE `receipt_templates` ADD COLUMN `totalsFontSize` TEXT NOT NULL DEFAULT 'MEDIUM'")
        // Set default 80mm (576 dots) for all receipt templates
        db.execSQL("UPDATE `receipt_templates` SET `paperWidthDots` = 576")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
    MIGRATION_10_11,
    MIGRATION_11_12,
    MIGRATION_12_13,
    MIGRATION_13_14,
    MIGRATION_14_15,
)


