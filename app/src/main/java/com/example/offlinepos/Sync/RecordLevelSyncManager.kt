package com.example.offlinepos.Sync

import android.content.Context
import android.database.Cursor
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.offlinepos.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Two-way Conflict Resolution Auto-merge Manager for Google Drive.
 *
 * Implements offline-first, bidirectional synchronization:
 * 1. Downloads remote JSON records from Google Drive AppData folder.
 * 2. Compares local database records against remote records to detect conflicts.
 * 3. Applies intelligent two-way conflict resolution auto-merge:
 *    - Products: SKU & Barcode identity, field-level merge (newer prices, metadata, stock reconciliations).
 *    - Customers: Phone/ID matching, debt balance mathematical reconciliation.
 *    - Sales & Transactions: Additive union preserving all transactions, with status conflict precedence (REFUNDED > COMPLETED).
 *    - Tombstones: Bidirectional deletion tracking.
 * 4. Applies merged state safely to local SQLite database with ID collision protection.
 * 5. Uploads the consolidated, conflict-free record bundle back to Google Drive AppData.
 */
class RecordLevelSyncManager(private val context: Context) {

    data class MergeResult(
        val mergedBundle: JSONObject,
        val totalRecords: Int,
        val conflictsDetected: Int,
        val conflictsResolved: Int,
        val summary: String,
    )

    companion object {
        private const val SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"
        private const val FILE_NAME = "offline_pos_records_v2.json"
        private const val DEVICE_ID = "TR-POS-TERMINAL"
        val TABLES = listOf(
            "products", "sales", "sale_items", "payments", "customers", "debt_transactions",
            "purchases", "shifts", "parked_orders", "stock_movements", "return_transactions",
            "audit_events", "sync_outbox", "sync_tombstones",
        )
    }

    suspend fun sync(accountEmail: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val token = GoogleAuthHelper.fetchToken(context, accountEmail, SCOPE)
            val db = AppDatabase.get(context)
            
            // 1. Read local state
            val local = readBundle(db)
            
            // 2. Fetch remote state from Google Drive if available
            val remote = findFileId(token)?.let { download(token, it) }
            
            // 3. Two-Way Conflict Resolution Auto-Merge
            val mergeResult = mergeBundles(local, remote)
            val merged = mergeResult.mergedBundle
            
            // 4. Apply merged changes safely back to local SQLite
            applyBundle(db, merged, mergeResult)
            
            // 5. Upload merged bundle back to Google Drive
            val file = File(context.cacheDir, FILE_NAME)
            file.writeText(merged.toString())
            val id = findFileId(token)
            val code = if (id == null) create(token, file) else update(token, id, file)
            if (code !in 200..299) error("Record merge upload failed: HTTP $code")
            
            // 6. Save sync & conflict resolution metrics to SharedPreferences
            context.getSharedPreferences("pos_sync_status", Context.MODE_PRIVATE).edit()
                .putLong("last_record_sync_ms", System.currentTimeMillis())
                .putInt("last_record_count", mergeResult.totalRecords)
                .putInt("last_conflicts_detected", mergeResult.conflictsDetected)
                .putInt("last_conflicts_resolved", mergeResult.conflictsResolved)
                .putString("last_conflict_summary", mergeResult.summary)
                .apply()
                
            mergeResult.totalRecords
        }
    }

    private fun readBundle(db: AppDatabase): JSONObject {
        val tables = JSONObject()
        val sqlite = db.openHelper.readableDatabase
        TABLES.forEach { table ->
            val rows = JSONArray()
            try {
                sqlite.query(SimpleSQLiteQuery("SELECT * FROM $table")).use { cursor ->
                    val columns = cursor.columnNames
                    while (cursor.moveToNext()) {
                        val row = JSONObject()
                        columns.forEachIndexed { index, column -> row.put(column, cursorValue(cursor, index)) }
                        rows.put(row)
                    }
                }
            } catch (_: Exception) { }
            tables.put(table, rows)
        }
        return JSONObject()
            .put("format", "offlinepos-records-v2")
            .put("deviceId", DEVICE_ID)
            .put("conflictPolicy", "TWO_WAY_AUTO_MERGE")
            .put("productIdentity", "SKU")
            .put("tables", tables)
    }

    private fun cursorValue(cursor: Cursor, index: Int): Any = when (cursor.getType(index)) {
        Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
        Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
        Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
        Cursor.FIELD_TYPE_BLOB -> android.util.Base64.encodeToString(cursor.getBlob(index), android.util.Base64.NO_WRAP)
        else -> cursor.getString(index)
    }

    /**
     * Comprehensive Two-Way Conflict Resolution Auto-Merge.
     */
    fun mergeBundles(local: JSONObject, remote: JSONObject?): MergeResult {
        if (remote == null) {
            val total = countRows(local)
            return MergeResult(
                mergedBundle = local,
                totalRecords = total,
                conflictsDetected = 0,
                conflictsResolved = 0,
                summary = "Local records pushed to Drive (Initial sync: $total records)",
            )
        }

        var conflictsDetected = 0
        var conflictsResolved = 0
        var totalMergedRecords = 0

        val out = JSONObject()
            .put("format", "offlinepos-records-v2")
            .put("deviceId", DEVICE_ID)
            .put("conflictPolicy", "TWO_WAY_AUTO_MERGE")
            .put("productIdentity", "SKU")

        val outputTables = JSONObject()
        val localTables = local.optJSONObject("tables") ?: JSONObject()
        val remoteTables = remote.optJSONObject("tables") ?: JSONObject()

        TABLES.forEach { table ->
            val localMap = linkedMapOf<String, JSONObject>()
            val remoteMap = linkedMapOf<String, JSONObject>()

            val localRows = localTables.optJSONArray(table)
            if (localRows != null) {
                for (i in 0 until localRows.length()) {
                    val row = localRows.optJSONObject(i) ?: continue
                    localMap[rowKey(table, row)] = row
                }
            }

            val remoteRows = remoteTables.optJSONArray(table)
            if (remoteRows != null) {
                for (i in 0 until remoteRows.length()) {
                    val row = remoteRows.optJSONObject(i) ?: continue
                    remoteMap[rowKey(table, row)] = row
                }
            }

            val mergedMap = linkedMapOf<String, JSONObject>()
            val allKeys = LinkedHashSet<String>().apply {
                addAll(localMap.keys)
                addAll(remoteMap.keys)
            }

            for (key in allKeys) {
                val localRow = localMap[key]
                val remoteRow = remoteMap[key]

                if (localRow != null && remoteRow == null) {
                    // Created locally, push to remote
                    mergedMap[key] = localRow
                } else if (localRow == null && remoteRow != null) {
                    // Created remotely, pull to local
                    mergedMap[key] = remoteRow
                } else if (localRow != null && remoteRow != null) {
                    // Record exists on both devices
                    if (areRowsEqual(localRow, remoteRow)) {
                        // Identical content, no conflict
                        mergedMap[key] = localRow
                    } else {
                        // Conflict detected! Auto-merge intelligently
                        conflictsDetected++
                        val resolved = when (table) {
                            "products" -> autoMergeProduct(localRow, remoteRow)
                            "customers" -> autoMergeCustomer(localRow, remoteRow)
                            "sales" -> autoMergeSale(localRow, remoteRow)
                            else -> autoMergeGeneric(table, localRow, remoteRow)
                        }
                        mergedMap[key] = resolved
                        conflictsResolved++
                    }
                }
            }

            val array = JSONArray()
            mergedMap.values.sortedBy { rowKey(table, it) }.forEach { array.put(it) }
            outputTables.put(table, array)
            totalMergedRecords += array.length()
        }

        out.put("tables", outputTables)
        out.put("conflictsDetected", conflictsDetected)
        out.put("conflictsResolved", conflictsResolved)
        out.put("mergedAtEpochMs", System.currentTimeMillis())

        val summary = if (conflictsDetected > 0) {
            "Two-way Auto-merge: ပဋိပက္ခ $conflictsResolved ခုကို အလိုအလျောက် ပေါင်းစပ်ဖြေရှင်းပြီးပါပြီ ($totalMergedRecords records)"
        } else {
            "Two-way Auto-merge: အချက်အလက် $totalMergedRecords ခု ချောမွေ့စွာ တွဲစပ်ပြီးပါပြီ"
        }

        return MergeResult(
            mergedBundle = out,
            totalRecords = totalMergedRecords,
            conflictsDetected = conflictsDetected,
            conflictsResolved = conflictsResolved,
            summary = summary,
        )
    }

    private fun areRowsEqual(a: JSONObject, b: JSONObject): Boolean {
        if (a.length() != b.length()) return false
        val keys = a.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (!b.has(key)) return false
            val va = a.opt(key)?.toString().orEmpty()
            val vb = b.opt(key)?.toString().orEmpty()
            if (va != vb) return false
        }
        return true
    }

    private fun autoMergeProduct(local: JSONObject, remote: JSONObject): JSONObject {
        val localTime = local.optLong("updatedAtEpochMs", 0L)
        val remoteTime = remote.optLong("updatedAtEpochMs", 0L)
        val newerTime = maxOf(localTime, remoteTime)
        val priceSource = if (remoteTime > localTime) remote else local

        val merged = JSONObject()

        // 1. SKU & Barcode
        val sku = local.optString("sku").ifBlank { remote.optString("sku") }
        merged.put("sku", sku)

        val localBarcode = local.optString("barcode").trim()
        val remoteBarcode = remote.optString("barcode").trim()
        val barcode = when {
            localBarcode.isNotBlank() && remoteBarcode.isBlank() -> localBarcode
            remoteBarcode.isNotBlank() && localBarcode.isBlank() -> remoteBarcode
            remoteTime > localTime && remoteBarcode.isNotBlank() -> remoteBarcode
            else -> localBarcode.ifBlank { remoteBarcode }
        }
        if (barcode.isNotBlank() && barcode != "null") merged.put("barcode", barcode) else merged.put("barcode", JSONObject.NULL)

        // 2. Name & Category
        val remoteName = remote.optString("name")
        val localName = local.optString("name")
        merged.put("name", if (remoteTime > localTime && remoteName.isNotBlank()) remoteName else localName.ifBlank { remoteName })

        val remoteCat = remote.optString("category")
        val localCat = local.optString("category")
        merged.put("category", if (remoteTime > localTime && remoteCat.isNotBlank()) remoteCat else localCat.ifBlank { remoteCat })

        // 3. Pricing & Specs
        merged.put("costPriceMinor", priceSource.optLong("costPriceMinor", local.optLong("costPriceMinor", 0L)))
        merged.put("sellPriceMinor", priceSource.optLong("sellPriceMinor", local.optLong("sellPriceMinor", 0L)))
        merged.put("wholesalePriceMinor", priceSource.optLong("wholesalePriceMinor", local.optLong("wholesalePriceMinor", 0L)))
        merged.put("wholesaleMinQty", priceSource.optLong("wholesaleMinQty", local.optLong("wholesaleMinQty", 0L)))
        merged.put("lowStockThreshold", priceSource.optLong("lowStockThreshold", local.optLong("lowStockThreshold", 3L)))
        merged.put("unitName", priceSource.optString("unitName", local.optString("unitName", "ခု")))
        merged.put("isRoofingSheet", priceSource.optBoolean("isRoofingSheet", local.optBoolean("isRoofingSheet", false)))

        // 4. Image Uri
        val localImg = local.optString("imageUri").takeIf { it.isNotBlank() && it != "null" }
        val remoteImg = remote.optString("imageUri").takeIf { it.isNotBlank() && it != "null" }
        if (remoteTime > localTime && remoteImg != null) merged.put("imageUri", remoteImg)
        else if (localImg != null) merged.put("imageUri", localImg)
        else if (remoteImg != null) merged.put("imageUri", remoteImg)
        else merged.put("imageUri", JSONObject.NULL)

        // 5. Stock Quantity: Latest updated inventory state wins
        val stockSource = if (remoteTime > localTime) remote else local
        merged.put("stockQuantity", stockSource.optLong("stockQuantity", 0L))

        // 6. Active / Archived: Newer timestamp determines state
        val localActive = local.optBoolean("isActive", true)
        val remoteActive = remote.optBoolean("isActive", true)
        val isActive = if (localActive == remoteActive) localActive else (if (remoteTime > localTime) remoteActive else localActive)
        merged.put("isActive", isActive)

        // Local continuity
        if (local.has("productId")) merged.put("productId", local.optLong("productId"))
        else if (remote.has("productId")) merged.put("productId", remote.optLong("productId"))

        merged.put("updatedAtEpochMs", maxOf(newerTime, System.currentTimeMillis()))
        return merged
    }

    private fun autoMergeCustomer(local: JSONObject, remote: JSONObject): JSONObject {
        val localTime = local.optLong("createdAtEpochMs", 0L)
        val remoteTime = remote.optLong("createdAtEpochMs", 0L)
        val source = if (remoteTime > localTime) remote else local

        val merged = JSONObject()
        if (local.has("customerId")) merged.put("customerId", local.optLong("customerId"))
        else if (remote.has("customerId")) merged.put("customerId", remote.optLong("customerId"))

        merged.put("name", source.optString("name").ifBlank { local.optString("name") })
        merged.put("phone", source.optString("phone").ifBlank { local.optString("phone") })
        merged.put("address", source.optString("address").ifBlank { local.optString("address") })
        merged.put("notes", source.optString("notes").ifBlank { local.optString("notes") })
        merged.put("creditLimitMinor", source.optLong("creditLimitMinor", 0L))
        merged.put("currentDebtMinor", source.optLong("currentDebtMinor", local.optLong("currentDebtMinor", 0L)))
        if (source.has("creditDueDateEpochMs")) merged.put("creditDueDateEpochMs", source.opt("creditDueDateEpochMs"))
        merged.put("createdAtEpochMs", minOf(local.optLong("createdAtEpochMs", System.currentTimeMillis()), remote.optLong("createdAtEpochMs", System.currentTimeMillis())))
        return merged
    }

    private fun autoMergeSale(local: JSONObject, remote: JSONObject): JSONObject {
        // Status Precedence: REFUNDED or VOIDED overrides COMPLETED
        val localStatus = local.optString("status")
        val remoteStatus = remote.optString("status")
        if (localStatus == "REFUNDED" || localStatus == "VOIDED") return local
        if (remoteStatus == "REFUNDED" || remoteStatus == "VOIDED") return remote

        val localTime = local.optLong("createdAtEpochMs", 0L)
        val remoteTime = remote.optLong("createdAtEpochMs", 0L)
        return if (remoteTime > localTime) remote else local
    }

    private fun autoMergeGeneric(table: String, local: JSONObject, remote: JSONObject): JSONObject {
        val localTime = getRecordTimestamp(local)
        val remoteTime = getRecordTimestamp(remote)
        return if (remoteTime > localTime) remote else local
    }

    private fun getRecordTimestamp(row: JSONObject): Long {
        return row.optLong(
            "deletedAtEpochMs",
            row.optLong(
                "updatedAtEpochMs",
                row.optLong("createdAtEpochMs", row.optLong("openTimeEpochMs", row.optLong("timestampEpochMs", 0L)))
            )
        )
    }

    private fun rowKey(table: String, row: JSONObject): String {
        return when (table) {
            "products" -> {
                val sku = row.optString("sku").trim().lowercase()
                if (sku.isNotBlank()) "sku:$sku"
                else {
                    val barcode = row.optString("barcode").trim()
                    if (barcode.isNotBlank()) "barcode:$barcode"
                    else "prod_id:${row.opt("productId")}"
                }
            }
            "sale_items" -> {
                val saleId = row.optString("saleId")
                val lineId = row.opt("lineId")?.toString().orEmpty()
                val prod = row.optString("productNameSnapshot")
                val qty = row.optLong("quantity", 0L)
                "item:${saleId}_${lineId}_${prod}_$qty"
            }
            "sales" -> "sale:${row.optString("saleId")}"
            "payments" -> {
                val paymentId = row.optString("paymentId")
                val saleId = row.optString("saleId")
                "payment:${paymentId}_$saleId"
            }
            "customers" -> {
                val phone = row.optString("phone").trim()
                if (phone.isNotBlank()) "phone:$phone"
                else "customer:${row.opt("customerId")}"
            }
            "debt_transactions" -> "tx:${row.opt("txId")}"
            "purchases" -> "purchase:${row.opt("purchaseId")}"
            "shifts" -> "shift:${row.opt("shiftId")}"
            "parked_orders" -> "park:${row.opt("parkId")}"
            "stock_movements" -> "movement:${row.opt("movementId")}"
            "return_transactions" -> "return:${row.opt("returnId")}"
            "audit_events" -> "event:${row.opt("eventId")}"
            "sync_outbox" -> "outbox:${row.opt("operationId")}"
            "sync_tombstones" -> "tombstone:${row.opt("tombstoneId")}"
            else -> {
                val candidates = listOf(
                    "tombstoneId", "lineId", "paymentId", "txId", "movementId", "returnId", "eventId",
                    "operationId", "saleId", "purchaseId", "shiftId", "parkId", "customerId", "productId"
                )
                val key = candidates.firstOrNull { row.has(it) } ?: "_row"
                "$key:${row.opt(key)}"
            }
        }
    }

    private fun applyBundle(db: AppDatabase, bundle: JSONObject, mergeResult: MergeResult) {
        val sqlite = db.openHelper.writableDatabase
        val tables = bundle.optJSONObject("tables") ?: return
        sqlite.beginTransaction()
        try {
            sqlite.execSQL("PRAGMA foreign_keys = OFF")

            // 1. Products: match by SKU or barcode, prevent ID collisions
            val productRows = tables.optJSONArray("products")
            if (productRows != null) {
                applyProducts(sqlite, productRows)
            }

            // 2. Customers: match by Phone or ID
            val customerRows = tables.optJSONArray("customers")
            if (customerRows != null) {
                applyCustomers(sqlite, customerRows)
            }

            // 3. Transactions & other tables
            val otherTables = listOf(
                "sales", "sale_items", "payments", "debt_transactions",
                "purchases", "shifts", "parked_orders", "stock_movements",
                "return_transactions", "audit_events", "sync_outbox", "sync_tombstones",
            )
            for (table in otherTables) {
                val rows = tables.optJSONArray(table) ?: continue
                val columns = tableColumns(sqlite, table)
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue

                    if (table == "sale_items") {
                        val names = columns.filter { it != "lineId" && row.has(it) }
                        if (names.isEmpty()) continue
                        val saleId = row.optString("saleId")
                        val prodName = row.optString("productNameSnapshot")
                        val qty = row.optLong("quantity", 0L)
                        var exists = false
                        sqlite.query(
                            SimpleSQLiteQuery(
                                "SELECT 1 FROM sale_items WHERE saleId = ? AND productNameSnapshot = ? AND quantity = ? LIMIT 1",
                                arrayOf(saleId, prodName, qty)
                            )
                        ).use { c ->
                            if (c.moveToFirst()) exists = true
                        }
                        if (!exists) {
                            val args = names.map { valueForSql(row.opt(it)) }.toTypedArray()
                            try {
                                sqlite.execSQL(
                                    "INSERT INTO sale_items (${names.joinToString(",")}) VALUES (${names.joinToString(",") { "?" }})",
                                    args
                                )
                            } catch (_: Exception) { }
                        }
                        continue
                    }

                    val names = columns.filter { row.has(it) }
                    if (names.isEmpty()) continue
                    val args = names.map { valueForSql(row.opt(it)) }.toTypedArray()
                    try {
                        sqlite.execSQL(
                            "INSERT OR REPLACE INTO $table (${names.joinToString(",")}) VALUES (${names.joinToString(",") { "?" }})",
                            args
                        )
                    } catch (_: Exception) { }
                }
            }

            // 4. Reconcile Customer Debt mathematically from unioned debt_transactions
            try {
                sqlite.execSQL("""
                    UPDATE customers SET currentDebtMinor = (
                        SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT_SALE' THEN amountMinor WHEN type = 'REPAYMENT' THEN -amountMinor ELSE 0 END), 0)
                        FROM debt_transactions WHERE debt_transactions.customerId = customers.customerId
                    ) WHERE customerId IN (SELECT DISTINCT customerId FROM debt_transactions)
                """.trimIndent())
                sqlite.execSQL("UPDATE customers SET currentDebtMinor = 0 WHERE currentDebtMinor < 0")
            } catch (_: Exception) { }

            // 5. Apply Tombstones for deletions
            val tombstones = tables.optJSONArray("sync_tombstones") ?: JSONArray()
            for (i in 0 until tombstones.length()) {
                val t = tombstones.optJSONObject(i) ?: continue
                when (t.optString("entityType")) {
                    "customers" -> sqlite.execSQL("DELETE FROM customers WHERE customerId = ?", arrayOf(t.optString("entityId")))
                    "parked_orders" -> sqlite.execSQL("DELETE FROM parked_orders WHERE parkId = ?", arrayOf(t.optString("entityId")))
                    "products" -> sqlite.execSQL("UPDATE products SET isActive = 0, updatedAtEpochMs = ? WHERE sku = ?", arrayOf(t.optLong("deletedAtEpochMs"), t.optString("entityId")))
                }
            }

            // 6. Log Two-way Auto-merge Audit Event if conflicts resolved
            if (mergeResult.conflictsResolved > 0) {
                try {
                    sqlite.execSQL(
                        "INSERT INTO audit_events (action, category, details, timestampEpochMs, cashierId) VALUES (?, ?, ?, ?, ?)",
                        arrayOf("TWO_WAY_AUTO_MERGE", "SYNC", mergeResult.summary, System.currentTimeMillis(), "SYSTEM")
                    )
                } catch (_: Exception) { }
            }

            sqlite.execSQL("PRAGMA foreign_keys = ON")
            sqlite.setTransactionSuccessful()
        } finally {
            sqlite.endTransaction()
        }
    }

    private fun applyProducts(sqlite: SupportSQLiteDatabase, rows: JSONArray) {
        val columns = tableColumns(sqlite, "products")
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val sku = row.optString("sku").trim()
            val barcode = row.optString("barcode").trim().takeIf { it.isNotBlank() && it != "null" }

            var existingId: Long? = null
            if (sku.isNotBlank()) {
                sqlite.query(SimpleSQLiteQuery("SELECT productId FROM products WHERE sku = ? LIMIT 1", arrayOf(sku))).use { c ->
                    if (c.moveToFirst()) existingId = c.getLong(0)
                }
            }
            if (existingId == null && barcode != null) {
                sqlite.query(SimpleSQLiteQuery("SELECT productId FROM products WHERE barcode = ? LIMIT 1", arrayOf(barcode))).use { c ->
                    if (c.moveToFirst()) existingId = c.getLong(0)
                }
            }

            if (existingId != null) {
                val updateColumns = columns.filter { it != "productId" && row.has(it) }
                val args = (updateColumns.map { valueForSql(row.opt(it)) } + existingId).toTypedArray()
                val setClause = updateColumns.joinToString(", ") { "$it = ?" }
                try {
                    sqlite.execSQL("UPDATE products SET $setClause WHERE productId = ?", args)
                } catch (_: Exception) { }
            } else {
                var idInUse = false
                val rowId = row.optLong("productId", -1L)
                if (rowId > 0) {
                    sqlite.query(SimpleSQLiteQuery("SELECT productId FROM products WHERE productId = ? LIMIT 1", arrayOf(rowId))).use { c ->
                        if (c.moveToFirst()) idInUse = true
                    }
                }
                val insertColumns = if (idInUse || rowId <= 0) columns.filter { it != "productId" && row.has(it) } else columns.filter { row.has(it) }
                val args = insertColumns.map { valueForSql(row.opt(it)) }.toTypedArray()
                try {
                    sqlite.execSQL("INSERT OR REPLACE INTO products (${insertColumns.joinToString(",")}) VALUES (${insertColumns.joinToString(",") { "?" }})", args)
                } catch (_: Exception) { }
            }
        }
    }

    private fun applyCustomers(sqlite: SupportSQLiteDatabase, rows: JSONArray) {
        val columns = tableColumns(sqlite, "customers")
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            val phone = row.optString("phone").trim().takeIf { it.isNotBlank() }
            val rowId = row.optLong("customerId", -1L)

            var existingId: Long? = null
            if (rowId > 0) {
                sqlite.query(SimpleSQLiteQuery("SELECT customerId FROM customers WHERE customerId = ? LIMIT 1", arrayOf(rowId))).use { c ->
                    if (c.moveToFirst()) existingId = c.getLong(0)
                }
            }
            if (existingId == null && phone != null) {
                sqlite.query(SimpleSQLiteQuery("SELECT customerId FROM customers WHERE phone = ? LIMIT 1", arrayOf(phone))).use { c ->
                    if (c.moveToFirst()) existingId = c.getLong(0)
                }
            }

            if (existingId != null) {
                val updateColumns = columns.filter { it != "customerId" && row.has(it) }
                val args = (updateColumns.map { valueForSql(row.opt(it)) } + existingId).toTypedArray()
                val setClause = updateColumns.joinToString(", ") { "$it = ?" }
                try {
                    sqlite.execSQL("UPDATE customers SET $setClause WHERE customerId = ?", args)
                } catch (_: Exception) { }
            } else {
                val insertColumns = if (rowId <= 0) columns.filter { it != "customerId" && row.has(it) } else columns.filter { row.has(it) }
                val args = insertColumns.map { valueForSql(row.opt(it)) }.toTypedArray()
                try {
                    sqlite.execSQL("INSERT INTO customers (${insertColumns.joinToString(",")}) VALUES (${insertColumns.joinToString(",") { "?" }})", args)
                } catch (_: Exception) { }
            }
        }
    }

    fun recordTombstone(db: AppDatabase, entityType: String, entityId: String) {
        val now = System.currentTimeMillis()
        db.openHelper.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO sync_tombstones(tombstoneId,entityType,entityId,deletedAtEpochMs,deletedByDeviceId) VALUES(?,?,?,?,?)",
            arrayOf("$entityType:$entityId", entityType, entityId, now, DEVICE_ID),
        )
    }

    private fun countRows(bundle: JSONObject): Int {
        var count = 0
        bundle.optJSONObject("tables")?.keys()?.forEach { count += bundle.optJSONObject("tables")?.optJSONArray(it)?.length() ?: 0 }
        return count
    }

    private fun tableColumns(db: SupportSQLiteDatabase, table: String): List<String> {
        val result = mutableListOf<String>()
        db.query(SimpleSQLiteQuery("PRAGMA table_info($table)")).use { c ->
            val index = c.getColumnIndex("name")
            while (c.moveToNext()) if (index >= 0) result += c.getString(index)
        }
        return result
    }

    private fun valueForSql(value: Any?): Any? = if (value == null || value == JSONObject.NULL) null else value

    private fun findFileId(token: String): String? {
        val c = open(URL("https://www.googleapis.com/drive/v3/files?q='appDataFolder'%20in%20parents%20and%20name='$FILE_NAME'%20and%20trashed=false&spaces=appDataFolder&fields=files(id)"), token, "GET")
        return try { if (c.responseCode !in 200..299) null else JSONObject(c.inputStream.bufferedReader().use { it.readText() }).optJSONArray("files")?.optJSONObject(0)?.optString("id") } finally { c.disconnect() }
    }

    private fun download(token: String, id: String) = JSONObject(open(URL("https://www.googleapis.com/drive/v3/files/$id?alt=media"), token, "GET").inputStream.bufferedReader().use { it.readText() })

    private fun create(token: String, file: File): Int {
        val boundary = "offlinepos_records_${System.currentTimeMillis()}"
        val c = open(URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"), token, "POST")
        c.doOutput = true; c.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        val meta = JSONObject().put("name", FILE_NAME).put("parents", JSONArray().put("appDataFolder"))
        c.outputStream.use { out -> out.write("--$boundary\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n$meta\r\n--$boundary\r\nContent-Type: application/json\r\n\r\n".toByteArray()); file.inputStream().use { it.copyTo(out) }; out.write("\r\n--$boundary--\r\n".toByteArray()) }
        return c.responseCode.also { c.disconnect() }
    }

    private fun update(token: String, id: String, file: File): Int {
        val c = open(URL("https://www.googleapis.com/upload/drive/v3/files/$id?uploadType=media"), token, "PATCH")
        c.doOutput = true; c.setRequestProperty("Content-Type", "application/json")
        file.inputStream().use { input -> c.outputStream.use { output -> input.copyTo(output) } }
        return c.responseCode.also { c.disconnect() }
    }

    private fun open(url: URL, token: String, method: String) = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = method
        connectTimeout = 20_000
        readTimeout = 60_000
        setRequestProperty("Authorization", "Bearer $token")
        setRequestProperty("Accept", "application/json")
    }
}
