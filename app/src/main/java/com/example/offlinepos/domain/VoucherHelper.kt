package com.example.offlinepos.domain

import com.example.offlinepos.data.local.SaleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VoucherHelper {
    private val dateFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())

    /**
     * Formats an automatic voucher number, e.g. "TR-261027-0001"
     */
    fun formatVoucherNumber(dateEpochMs: Long, sequenceNumber: Int): String {
        val datePart = dateFormat.format(Date(dateEpochMs))
        val seq = if (sequenceNumber > 0) sequenceNumber else 1
        return "TR-$datePart-%04d".format(seq)
    }

    /**
     * Derives the auto voucher number for a SaleEntity given the list of all sales.
     * Orders sales for the same business date by createdAtEpochMs to determine the 1-based daily sequence.
     */
    fun getVoucherNumber(sale: SaleEntity, allSales: List<SaleEntity>): String {
        val sameDaySales = allSales
            .filter { it.businessDate == sale.businessDate }
            .sortedBy { it.createdAtEpochMs }
        val index = sameDaySales.indexOfFirst { it.saleId == sale.saleId }
        val seq = if (index >= 0) index + 1 else 1
        return formatVoucherNumber(sale.createdAtEpochMs, seq)
    }

    /**
     * Derives the voucher number for a saleId and date from a list of all sales.
     */
    fun getVoucherNumber(saleId: String, dateEpochMs: Long, allSales: List<SaleEntity>): String {
        val sale = allSales.find { it.saleId == saleId }
        if (sale != null) {
            return getVoucherNumber(sale, allSales)
        }
        return formatVoucherNumberFallback(saleId, dateEpochMs)
    }

    /**
     * Fallback voucher number if allSales is not available.
     */
    fun formatVoucherNumberFallback(saleId: String, dateEpochMs: Long): String {
        val datePart = dateFormat.format(Date(dateEpochMs))
        val shortSeq = (kotlin.math.abs(saleId.hashCode()) % 999) + 1
        return "TR-$datePart-%04d".format(shortSeq)
    }

    /**
     * Checks if a user's search query matches the voucher number, short code, numeric sequence, or customer name.
     * Supports:
     * - Full voucher number: "TR-261027-0001"
     * - Partial voucher number: "261027" or "0001" or "01" or "1"
     * - Short UUID ref: "#7B32DF" or "7B32"
     * - Customer name: "ဦးမောင်မောင်"
     */
    fun matchesQuery(voucherNo: String, saleId: String, customerName: String?, query: String): Boolean {
        val q = query.trim()
        if (q.isBlank()) return true

        // Direct containment in voucherNo (case-insensitive)
        if (voucherNo.contains(q, ignoreCase = true)) return true

        // Direct containment in saleId UUID
        if (saleId.contains(q, ignoreCase = true)) return true

        // Direct containment in customer name
        if (customerName?.contains(q, ignoreCase = true) == true) return true

        // Query by numeric sequence: e.g. "1" matches "...-0001", "25" matches "...-0025"
        val queryNumber = q.toIntOrNull()
        if (queryNumber != null) {
            val suffix4 = "-%04d".format(queryNumber)
            val suffix3 = "-%03d".format(queryNumber)
            val suffixPlain = "-$queryNumber"
            if (voucherNo.endsWith(suffix4) || voucherNo.endsWith(suffix3) || voucherNo.endsWith(suffixPlain)) {
                return true
            }
        }

        // Query with leading '#'
        if (q.startsWith("#")) {
            val clean = q.removePrefix("#").trim()
            if (saleId.contains(clean, ignoreCase = true) || voucherNo.contains(clean, ignoreCase = true)) {
                return true
            }
        }

        return false
    }
}
