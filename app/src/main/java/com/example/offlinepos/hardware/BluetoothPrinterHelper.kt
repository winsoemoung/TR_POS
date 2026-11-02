package com.example.offlinepos.hardware

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.offlinepos.ui.ReceiptData
import com.example.offlinepos.ui.StoreProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object BluetoothPrinterHelper {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    const val PRINTER_WIDTH_80MM = 576 // 80mm standard: 576 dots (72 bytes)
    const val PRINTER_WIDTH_58MM = 384 // 58mm standard: 384 dots (48 bytes)
    const val PRINTER_WIDTH_DOTS = PRINTER_WIDTH_80MM
    private const val PREFS = "bluetooth_printer_prefs"
    private const val SELECTED_ADDRESS = "selected_printer_address"

    fun selectedPrinterAddress(context: Context): String? = context
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(SELECTED_ADDRESS, null)

    fun selectPrinter(context: Context, device: BluetoothDevice) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(SELECTED_ADDRESS, device.address)
            .apply()
    }

    @SuppressLint("MissingPermission")
    fun requestPairing(device: BluetoothDevice): Boolean = try {
        device.bondState == BluetoothDevice.BOND_BONDED || device.createBond()
    } catch (_: SecurityException) { false }

    @SuppressLint("MissingPermission")
    private suspend fun connectWithRetry(device: BluetoothDevice): BluetoothSocket {
        var lastError: Throwable? = null
        repeat(3) { attempt ->
            var socket: BluetoothSocket? = null
            try {
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    if (!requestPairing(device)) error("Printer is not paired")
                    delay(700L)
                }
                BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery()
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                if (socket.isConnected) return socket
                error("Printer socket is not connected")
            } catch (t: Throwable) {
                lastError = t
                try { socket?.close() } catch (_: Exception) {}
                if (attempt < 2) delay(400L * (1L shl attempt))
            }
        }
        throw lastError ?: IllegalStateException("Unable to connect to printer")
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<BluetoothDevice> {
        return try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            adapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Renders a complete high-fidelity thermal receipt in Myanmar language
     * supporting both 80mm (576 dots) and 58mm (384 dots) widths.
     */
    fun renderReceiptBitmap(
        receipt: ReceiptData,
        profile: StoreProfile,
        paperWidthDots: Int = PRINTER_WIDTH_80MM,
    ): Bitmap {
        val width = paperWidthDots
        val is58mm = width <= 400

        // Responsive font sizes & layout positions
        val titleSize = if (is58mm) 24f else 32f
        val headerSize = if (is58mm) 17f else 21f
        val bodySize = if (is58mm) 16f else 21f
        val bodyBoldSize = if (is58mm) 17f else 22f
        val totalSize = if (is58mm) 23f else 30f

        val leftMargin = if (is58mm) 10f else 20f
        val colQty = if (is58mm) 180f else 285f
        val colRate = if (is58mm) 250f else 395f
        val colTotal = if (is58mm) 330f else 515f
        val summaryValX = if (is58mm) 240f else 380f
        val grandTotalValX = if (is58mm) 210f else 320f

        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = titleSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val paintHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = headerSize
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }
        val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = bodySize
            typeface = Typeface.DEFAULT
        }
        val paintBodyBold = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = bodyBoldSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paintTotal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = totalSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Estimate height
        val baseHeaderHeight = if (profile.customLogoPath != null) 340 else 240
        val itemsHeight = (receipt.items.size * (if (is58mm) 48 else 55)) + 60
        val summaryHeight = 280
        val footerLines = profile.footerMessage.split("\n").count { it.isNotBlank() }.coerceAtLeast(1)
        val footerHeight = (footerLines * 30) + 120
        val totalHeight = baseHeaderHeight + itemsHeight + summaryHeight + footerHeight

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = 30f

        // 0. Optional Logo
        if (!profile.customLogoPath.isNullOrBlank()) {
            val logoFile = File(profile.customLogoPath)
            if (logoFile.exists()) {
                val logoBmp = BitmapFactory.decodeFile(logoFile.absolutePath)
                if (logoBmp != null) {
                    val targetLogoWidth = if (is58mm) 100 else 140
                    val scale = targetLogoWidth.toFloat() / logoBmp.width.toFloat()
                    val targetLogoHeight = (logoBmp.height * scale).toInt()
                    val scaledLogo = Bitmap.createScaledBitmap(logoBmp, targetLogoWidth, targetLogoHeight, true)
                    canvas.drawBitmap(scaledLogo, (width - targetLogoWidth) / 2f, y, null)
                    y += targetLogoHeight + 12f
                }
            }
        }

        // 1. Store header (style and alignment are user-configurable)
        ReceiptStyle.apply(paintTitle, profile.receiptHeaderStyle, profile.receiptHeaderSize, profile.receiptHeaderAlign, titleSize)
        val headerX = when (paintTitle.textAlign) {
            Paint.Align.LEFT -> leftMargin
            Paint.Align.RIGHT -> width - leftMargin
            else -> width / 2f
        }
        canvas.drawText(profile.storeName, headerX, y, paintTitle)
        y += paintTitle.textSize + 6f

        if (profile.headerNote.isNotBlank()) {
            ReceiptStyle.apply(paintHeader, profile.receiptHeaderStyle, profile.receiptHeaderSize, profile.receiptHeaderAlign, headerSize)
            canvas.drawText(profile.headerNote, headerX, y, paintHeader)
            y += paintHeader.textSize + 6f
        }
        if (profile.address.isNotBlank()) {
            canvas.drawText(profile.address, headerX, y, paintHeader)
            y += paintHeader.textSize + 6f
        }
        if (profile.phone.isNotBlank()) {
            canvas.drawText("ဖုန်း: ${profile.phone}", headerX, y, paintHeader)
            y += paintHeader.textSize + 8f
        }

        // Divider
        drawDashedLine(canvas, y, width)
        y += 24f

        // Bill Info
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(receipt.timestampEpochMs))
        val vNo = receipt.displayVoucherNumber
        canvas.drawText("ဘောင်ချာအမှတ်: $vNo", leftMargin, y, paintBodyBold)
        y += 24f
        canvas.drawText("ရက်စွဲ: $dateStr", leftMargin, y, paintBody)
        y += 22f
        canvas.drawText("Ref Code: #${receipt.saleId.take(8).uppercase()}", leftMargin, y, paintBody)
        val cashierX = if (is58mm) leftMargin else 320f
        if (is58mm) {
            y += 22f
            canvas.drawText("အရောင်းဝန်ထမ်း: ${receipt.cashierId}", cashierX, y, paintBody)
        } else {
            canvas.drawText("အရောင်းဝန်ထမ်း: ${receipt.cashierId}", cashierX, y, paintBody)
        }
        if (!receipt.customerName.isNullOrBlank()) {
            y += 22f
            canvas.drawText("ဝယ်သူ: ${receipt.customerName}", leftMargin, y, paintBodyBold)
        }
        y += 26f

        // Table Header
        drawDashedLine(canvas, y, width)
        y += 24f
        canvas.drawText("ပစ္စည်း", leftMargin, y, paintBodyBold)
        canvas.drawText("ဦးရေ", colQty, y, paintBodyBold)
        canvas.drawText("နှုန်း", colRate, y, paintBodyBold)
        canvas.drawText("သင့်ငွေ", colTotal, y, paintBodyBold)
        y += 24f
        drawDashedLine(canvas, y, width)
        y += 28f

        // Line Items
        receipt.items.forEach { line ->
            val maxLen = if (is58mm) 14 else 24
            val nameDisplay = if (line.productNameSnapshot.length > maxLen) {
                line.productNameSnapshot.take(maxLen - 2) + ".."
            } else {
                line.productNameSnapshot
            }
            canvas.drawText(nameDisplay, leftMargin, y, paintBodyBold)
            y += if (is58mm) 20f else 24f

            canvas.drawText("${line.quantity}", colQty, y, paintBody)
            canvas.drawText("%,d".format(line.unitPriceMinor), colRate, y, paintBody)
            canvas.drawText("%,d".format(line.lineTotalMinor), colTotal, y, paintBodyBold)
            y += if (is58mm) 24f else 30f
        }

        // Divider
        drawDashedLine(canvas, y, width)
        y += 26f

        // Summary Rows
        drawSummaryRow(canvas, "ကျသင့်ငွေ (Subtotal):", "%,d Ks".format(receipt.totals.subtotalMinor), leftMargin, summaryValX, y, paintBody)
        y += 26f

        if (receipt.totals.discountMinor > 0) {
            drawSummaryRow(canvas, "လျှော့စျေး (Discount):", "-%,d Ks".format(receipt.totals.discountMinor), leftMargin, summaryValX, y, paintBody)
            y += 26f
        }

        if (receipt.deliveryFeeMinor > 0) {
            drawSummaryRow(canvas, "သယ်ယူ ပို့ဆောင်ခ:", "%,d Ks".format(receipt.deliveryFeeMinor), leftMargin, summaryValX, y, paintBody)
            y += 26f
        }

        drawSolidLine(canvas, y, width)
        y += 32f

        // Grand Total
        canvas.drawText("စုစုပေါင်း (TOTAL):", leftMargin, y, paintTotal)
        canvas.drawText("%,d Ks".format(receipt.totals.totalMinor), grandTotalValX, y, paintTotal)
        y += 36f

        drawSolidLine(canvas, y, width)
        y += 26f

        val paymentMethodBurmese = when (receipt.paymentMethod) {
            "CASH" -> "ငွေသား (Cash)"
            "KPAY" -> "📱 KPay"
            "WAVEPAY" -> "🟡 WavePay"
            "CREDIT" -> "📝 အကြွေး (Credit)"
            "SPLIT" -> "⚡ Split Payment"
            else -> receipt.paymentMethod
        }
        drawSummaryRow(canvas, "ပေးချေမှုပုံစံ:", paymentMethodBurmese, leftMargin, summaryValX, y, paintBody)
        y += 26f
        if (receipt.paymentMethod == "CASH") {
            drawSummaryRow(canvas, "ပေးငွေ (Tendered):", "%,d Ks".format(receipt.tenderedMinor), leftMargin, summaryValX, y, paintBody)
            y += 26f
            drawSummaryRow(canvas, "ပြန်အမ်းငွေ (Change):", "%,d Ks".format(receipt.changeMinor), leftMargin, summaryValX, y, paintBodyBold)
            y += 30f
        } else if (receipt.paymentMethod == "SPLIT") {
            drawSummaryRow(canvas, "• ငွေသား (Cash):", "%,d Ks".format(receipt.splitCashMinor), leftMargin, summaryValX, y, paintBody)
            y += 24f
            drawSummaryRow(canvas, "• Digital (KPay):", "%,d Ks".format(receipt.splitDigitalMinor), leftMargin, summaryValX, y, paintBody)
            y += 30f
        } else if (receipt.paymentMethod == "CREDIT") {
            drawSummaryRow(canvas, "အကြွေးမှတ်တမ်းငွေ:", "%,d Ks".format(receipt.totals.totalMinor), leftMargin, summaryValX, y, paintBodyBold)
            y += 30f
        }

        drawDashedLine(canvas, y, width)
        y += 26f

        // Footer
        ReceiptStyle.apply(paintHeader, profile.receiptFooterStyle, profile.receiptFooterSize, profile.receiptFooterAlign, headerSize)
        val footerX = when (paintHeader.textAlign) {
            Paint.Align.LEFT -> leftMargin
            Paint.Align.RIGHT -> width - leftMargin
            else -> width / 2f
        }
        profile.footerMessage.split("\n").forEach { line ->
            if (line.isNotBlank()) {
                canvas.drawText(line.trim(), footerX, y, paintHeader)
                y += paintHeader.textSize + 6f
            }
        }
        y += 8f
        canvas.drawText("*** ကျေးဇူးတင်ပါသည် ***", width / 2f, y, paintHeader)
        y += 36f

        // Trim bitmap to actual rendered height
        val finalHeight = y.toInt().coerceAtMost(totalHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, width, finalHeight)
    }

    /**
     * Renders a quick test ticket to verify thermal printer connectivity and alignment.
     */
    fun renderTestReceiptBitmap(profile: StoreProfile, paperWidthDots: Int = PRINTER_WIDTH_80MM): Bitmap {
        val width = paperWidthDots
        val height = 450
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = if (width <= 400) 22f else 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val paintBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = if (width <= 400) 16f else 20f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        var y = 40f
        canvas.drawText(profile.storeName, width / 2f, y, paintTitle)
        y += 32f
        canvas.drawText("=== THERMAL PRINTER TEST ===", width / 2f, y, paintBody)
        y += 30f
        drawDashedLine(canvas, y, width)
        y += 30f

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("ရက်စွဲ: $dateStr", width / 2f, y, paintBody)
        y += 28f
        val modeStr = if (width <= 400) "Paper Width: 58mm (384 dots)" else "Paper Width: 80mm (576 dots)"
        canvas.drawText(modeStr, width / 2f, y, paintBody)
        y += 28f
        canvas.drawText("စမ်းသပ် ပရင့်ထုတ်ခြင်း အောင်မြင်ပါသည်", width / 2f, y, paintBody)
        y += 30f
        drawDashedLine(canvas, y, width)
        y += 34f
        canvas.drawText("OFFLINE POS - READY TO PRINT", width / 2f, y, paintBody)
        y += 40f

        return Bitmap.createBitmap(bitmap, 0, 0, width, y.toInt().coerceAtMost(height))
    }

    private fun drawDashedLine(canvas: Canvas, y: Float, width: Int) {
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
        }
        var x = 8f
        while (x < width - 8f) {
            canvas.drawLine(x, y, x + 7f, y, paint)
            x += 12f
        }
    }

    private fun drawSolidLine(canvas: Canvas, y: Float, width: Int) {
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2.5f
        }
        canvas.drawLine(8f, y, width - 8f, y, paint)
    }

    private fun drawSummaryRow(
        canvas: Canvas,
        label: String,
        value: String,
        leftMargin: Float,
        valX: Float,
        y: Float,
        paint: Paint,
    ) {
        canvas.drawText(label, leftMargin, y, paint)
        canvas.drawText(value, valX, y, paint)
    }

    /**
     * Converts an Android Bitmap into standard ESC/POS raster bit-image command data.
     * Compatible with 80mm (576 dots = 72 bytes) and 58mm (384 dots = 48 bytes).
     */
    fun convertBitmapToEscPos(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val rawBits = ByteArray(widthBytes * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val luminance = (r * 299 + g * 587 + b * 114) / 1000

                // If pixel is dark (black ink)
                if (luminance < 165) {
                    val byteIndex = y * widthBytes + (x / 8)
                    val bitIndex = 7 - (x % 8)
                    rawBits[byteIndex] = (rawBits[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }

        return EscPos.raster(widthBytes, height, rawBits)
    }

    /**
     * Connects to a paired Bluetooth thermal printer and prints the formatted Burmese receipt.
     */
    @SuppressLint("MissingPermission")
    suspend fun printReceipt(
        device: BluetoothDevice,
        receipt: ReceiptData,
        profile: StoreProfile,
        paperWidthDots: Int = PRINTER_WIDTH_80MM,
        kickDrawer: Boolean = true,
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            val bitmap = renderReceiptBitmap(receipt, profile, paperWidthDots)
            val rasterData = convertBitmapToEscPos(bitmap)

            socket = connectWithRetry(device)
            outputStream = socket.outputStream

            // Send Init, Print Bitmap, Feed & Cut Paper, Kick Cash Drawer
            outputStream.write(EscPos.INIT)
            outputStream.write(rasterData)
            outputStream.write(byteArrayOf(0x1B.toByte(), 0x64.toByte(), 0x03.toByte())) // Feed 3 lines
            outputStream.write(EscPos.CUT)
            if (kickDrawer) {
                outputStream.write(EscPos.CASH_DRAWER_KICK)
            }
            outputStream.flush()

            val devName = try { device.name ?: "Bluetooth Printer" } catch (_: SecurityException) { "Bluetooth Printer" }
            Pair(true, "ဘေလ်ပြေစာကို $devName သို့ အောင်မြင်စွာ ပရင့်ထုတ်ပြီးပါပြီ")
        } catch (e: Exception) {
            Pair(false, "Bluetooth Printer ချိတ်ဆက်မှု မအောင်မြင်ပါ: ${e.message}")
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Test print over Bluetooth.
     */
    @SuppressLint("MissingPermission")
    suspend fun printTestReceipt(
        device: BluetoothDevice,
        profile: StoreProfile,
        paperWidthDots: Int = PRINTER_WIDTH_80MM,
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            val bitmap = renderTestReceiptBitmap(profile, paperWidthDots)
            val rasterData = convertBitmapToEscPos(bitmap)

            socket = connectWithRetry(device)
            outputStream = socket.outputStream

            outputStream.write(EscPos.INIT)
            outputStream.write(rasterData)
            outputStream.write(byteArrayOf(0x1B.toByte(), 0x64.toByte(), 0x03.toByte()))
            outputStream.write(EscPos.CUT)
            outputStream.flush()

            val devName = try { device.name ?: "Bluetooth Printer" } catch (_: SecurityException) { "Bluetooth Printer" }
            Pair(true, "စမ်းသပ်ပရင့်ကို $devName သို့ အောင်မြင်စွာ ထုတ်ပြီးပါပြီ")
        } catch (e: Exception) {
            Pair(false, "Bluetooth စမ်းသပ်ပရင့် မအောင်မြင်ပါ: ${e.message}")
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * Print raw ESC/POS bytes over Bluetooth.
     */
    @SuppressLint("MissingPermission")
    suspend fun printRawBytes(
        device: BluetoothDevice,
        bytes: ByteArray,
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null
        try {
            socket = connectWithRetry(device)
            outputStream = socket.outputStream
            outputStream.write(bytes)
            outputStream.flush()
            val devName = try { device.name ?: "Bluetooth Printer" } catch (_: SecurityException) { "Bluetooth Printer" }
            Pair(true, "ဘေလ်ပြေစာကို $devName သို့ အောင်မြင်စွာ ပရင့်ထုတ်ပြီးပါပြီ")
        } catch (e: Exception) {
            Pair(false, "Bluetooth Printer ချိတ်ဆက်မှု မအောင်မြင်ပါ: ${e.message}")
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
