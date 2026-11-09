package com.example.offlinepos.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.offlinepos.data.local.ReceiptTemplateEntity
import com.example.offlinepos.ui.ReceiptData
import com.example.offlinepos.ui.StoreProfile
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CustomReceiptEscPosGenerator {

    /**
     * Builds ESC/POS bytes from a customized receipt template and receipt data.
     * Generates a high-fidelity monochrome bitmap rendered strictly according to user template parameters,
     * ensuring flawless Myanmar font rendering and Excel-like grid alignment on thermal printers.
     */
    fun generateEscPosBytes(
        context: Context,
        template: ReceiptTemplateEntity,
        receipt: ReceiptData,
        storeProfile: StoreProfile,
        paperWidth: PaperWidth = PaperWidth.WIDTH_58MM,
    ): ByteArray {
        val widthDots = if (template.paperWidthDots > 0) template.paperWidthDots else paperWidth.dots
        val bitmap = renderReceiptBitmap(
            template = template,
            receipt = receipt,
            storeProfile = storeProfile,
            targetWidthDots = widthDots
        )
        return bitmapToEscPosRaster(bitmap)
    }

    fun generateEscPosForTemplate(
        template: ReceiptTemplateEntity,
        receipt: ReceiptData,
        profile: StoreProfile,
        paperWidthDots: Int = 384,
        kickDrawer: Boolean = false,
    ): ByteArray {
        val widthDots = if (template.paperWidthDots > 0) template.paperWidthDots else paperWidthDots
        val bitmap = renderReceiptBitmap(
            template = template,
            receipt = receipt,
            storeProfile = profile,
            targetWidthDots = widthDots
        )
        val raster = bitmapToEscPosRaster(bitmap)
        return if (kickDrawer) {
            raster + EscPos.CASH_DRAWER_KICK
        } else {
            raster
        }
    }

    /**
     * Renders a bitmap of the receipt based on the Custom Receipt Template Builder parameters.
     * Used for both live WYSIWYG preview in Compose and ESC/POS thermal printing.
     */
    fun renderReceiptBitmap(
        template: ReceiptTemplateEntity,
        receipt: ReceiptData,
        storeProfile: StoreProfile,
        targetWidthDots: Int = 384, // 384 dots for standard 58mm, 576 dots for 80mm
    ): Bitmap {
        val width = if (template.paperWidthDots > 0) template.paperWidthDots else targetWidthDots
        val is58mm = width <= 420

        // Pre-measure height
        val estimatedLines = 35 + (receipt.items.size * 3)
        val estimatedHeight = (estimatedLines * (if (is58mm) 24 else 28)) + 350
        val tempBitmap = Bitmap.createBitmap(width, estimatedHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(tempBitmap)
        canvas.drawColor(Color.WHITE)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = 1.4f
        }

        var currentY = if (is58mm) 20f else 24f

        fun drawDivider(style: String = template.gridLineStyle) {
            when (style.uppercase()) {
                "DASHED" -> {
                    var x = 10f
                    val dashLen = if (is58mm) 5.5f else 7f
                    val spaceLen = if (is58mm) 4f else 5f
                    while (x < width - 10f) {
                        canvas.drawLine(x, currentY, minOf(x + dashLen, width - 10f), currentY, linePaint)
                        x += (dashLen + spaceLen)
                    }
                    currentY += if (is58mm) 12f else 14f
                }
                "SOLID" -> {
                    canvas.drawLine(10f, currentY, width - 10f, currentY, linePaint)
                    currentY += if (is58mm) 10f else 12f
                }
                "DOUBLE" -> {
                    canvas.drawLine(10f, currentY, width - 10f, currentY, linePaint)
                    canvas.drawLine(10f, currentY + 3.5f, width - 10f, currentY + 3.5f, linePaint)
                    currentY += if (is58mm) 13f else 15f
                }
                else -> {
                    // NONE: No row/column lines drawn at all
                    currentY += if (is58mm) 4f else 6f
                }
            }
        }

        // ==========================================
        // 1. HEADER SECTION
        // ==========================================
        val storeNameText = template.customStoreName.ifBlank {
            storeProfile.storeName.ifBlank { "ဆန်းသစ်" }
        }

        val storeNameSize = when (template.storeNameFontSize.uppercase()) {
            "XLARGE" -> if (is58mm) 28f else 36f
            "LARGE" -> if (is58mm) 25f else 30f
            "SMALL" -> if (is58mm) 19f else 22f
            else -> if (is58mm) 22f else 26f
        }
        boldPaint.textSize = storeNameSize
        boldPaint.textAlign = when (template.storeNameAlign.uppercase()) {
            "LEFT" -> Paint.Align.LEFT
            "RIGHT" -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }
        val headerX = when (template.storeNameAlign.uppercase()) {
            "LEFT" -> 14f
            "RIGHT" -> width - 14f
            else -> width / 2f
        }
        canvas.drawText(storeNameText, headerX, currentY, boldPaint)
        currentY += storeNameSize + 6f

        // Custom Subtitle (e.g. "အိမ်ဆောက်ပစ္စည်း အရောင်းဆိုင်")
        if (template.customSubtitle.isNotBlank()) {
            boldPaint.textSize = if (is58mm) 17f else 21f
            boldPaint.textAlign = Paint.Align.CENTER
            template.customSubtitle.split("\n").forEach { sub ->
                if (sub.isNotBlank()) {
                    canvas.drawText(sub.trim(), width / 2f, currentY, boldPaint)
                    currentY += if (is58mm) 20f else 24f
                }
            }
        }

        // Custom Owner or Contact Info (e.g. "ကိုပါပီး+မသီရိကလျာ")
        if (template.customOwnerOrContact.isNotBlank()) {
            textPaint.textSize = if (is58mm) 15.5f else 18.5f
            textPaint.textAlign = Paint.Align.CENTER
            template.customOwnerOrContact.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    canvas.drawText(line.trim(), width / 2f, currentY, textPaint)
                    currentY += if (is58mm) 18.5f else 22f
                }
            }
        }

        // Phone Numbers
        if (template.showPhone) {
            val phoneText = template.customPhone.ifBlank { storeProfile.phone }
            if (phoneText.isNotBlank()) {
                textPaint.textSize = if (is58mm) 14.5f else 18f
                textPaint.textAlign = Paint.Align.CENTER
                phoneText.split("\n").forEach { pLine ->
                    if (pLine.isNotBlank()) {
                        canvas.drawText(pLine.trim(), width / 2f, currentY, textPaint)
                        currentY += if (is58mm) 18f else 22f
                    }
                }
            }
        }

        // Address
        if (template.showAddress) {
            val addressText = template.customAddress.ifBlank { storeProfile.address }
            if (addressText.isNotBlank()) {
                textPaint.textSize = if (is58mm) 14f else 17.5f
                textPaint.textAlign = Paint.Align.CENTER
                addressText.split("\n").forEach { aLine ->
                    if (aLine.isNotBlank()) {
                        canvas.drawText(aLine.trim(), width / 2f, currentY, textPaint)
                        currentY += if (is58mm) 17.5f else 21f
                    }
                }
            }
        }

        // Header Note (Optional)
        if (template.showHeaderNote) {
            val note = template.customHeaderNote.ifBlank { storeProfile.headerNote }
            if (note.isNotBlank()) {
                textPaint.textSize = if (is58mm) 15f else 19f
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(note, width / 2f, currentY, textPaint)
                currentY += if (is58mm) 18f else 22f
            }
        }

        // Metadata: Date, Customer, Cashier
        val metaSize = if (is58mm) 15f else 18f
        textPaint.textSize = metaSize
        boldPaint.textSize = metaSize

        // Date
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ရက်စွဲ: ${receipt.businessDate}", 14f, currentY, textPaint)
        if (template.showCashierInfo) {
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("တာဝန်ခံ: ${receipt.cashierId}", width - 14f, currentY, textPaint)
        }
        currentY += metaSize + 5f

        // Customer
        if (template.showCustomerInfo) {
            val cLabel = template.customerLabel.ifBlank { "ဈေးဝယ်သူ" }
            val cName = receipt.customerName?.ifBlank { null } ?: "MK store"
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("$cLabel: $cName", 14f, currentY, boldPaint)
            currentY += metaSize + 6f
        }

        // Divider before Items (Uses dashed line style for Template 1, none for Template 2)
        drawDivider(template.gridLineStyle)

        // ==========================================
        // 2. BODY SECTION
        // ==========================================
        val pad = template.rowPaddingDp.toFloat()
        val bodySize = when (template.bodyFontSize.uppercase()) {
            "LARGE" -> if (is58mm) 18f else 22f
            "SMALL" -> if (is58mm) 14f else 16.5f
            else -> if (is58mm) 16f else 19f
        }
        boldPaint.textSize = bodySize
        textPaint.textSize = bodySize

        if (template.itemLayoutStyle == "COLUMNS_4") {
            // 4-Column Layout (Template 2)
            val contentWidth = width - 28f
            val colNameW = contentWidth * template.itemNameWidthWeight
            val colQtyW = contentWidth * template.qtyWidthWeight
            val colPriceW = contentWidth * template.priceWidthWeight
            val colAmountW = contentWidth * template.amountWidthWeight

            val x0 = 14f
            val x1 = x0 + colNameW
            val x2 = x1 + colQtyW
            val x3 = x2 + colPriceW
            val xEnd = x3 + colAmountW

            // Table Header: နာမည်၊ ဦးရေ၊ နှုန်း၊ သင့်ငွေ
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("နာမည်", x0, currentY, boldPaint)
            boldPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("ဦးရေ", x1 + (colQtyW / 2f), currentY, boldPaint)
            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("နှုန်း", x2 + colPriceW, currentY, boldPaint)
            canvas.drawText("သင့်ငွေ", xEnd, currentY, boldPaint)
            currentY += bodySize + 7f

            // In Template 2, gridLineStyle is NONE -> No lines between rows/columns!
            if (template.gridLineStyle != "NONE") {
                drawDivider(template.gridLineStyle)
            }

            receipt.items.forEach { line ->
                currentY += pad

                // Name
                textPaint.textAlign = Paint.Align.LEFT
                val maxChars = (colNameW / (bodySize * 0.62f)).toInt().coerceAtLeast(8)
                val displayName = if (line.productNameSnapshot.length > maxChars) {
                    line.productNameSnapshot.take(maxChars - 1) + "…"
                } else {
                    line.productNameSnapshot
                }
                canvas.drawText(displayName, x0, currentY, textPaint)

                // Qty
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(line.quantity.toString(), x1 + (colQtyW / 2f), currentY, textPaint)

                // Unit Price
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%,d".format(line.unitPriceMinor), x2 + colPriceW, currentY, textPaint)

                // Total
                boldPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%,d".format(line.lineTotalMinor), xEnd, currentY, boldPaint)

                currentY += bodySize + pad + 3f
                // NO grid line between rows!
            }
        } else {
            // STANDARD_2LINE Layout (Template 1 - Uploaded Sample Style)
            // No column header table! Each item has Line 1 (Name & Amount) + Line 2 (Qty x Price)
            receipt.items.forEach { line ->
                // Line 1: Product Name on left, Line Total on right
                boldPaint.textSize = if (is58mm) 16f else 19f
                boldPaint.textAlign = Paint.Align.LEFT
                val maxNameChars = if (is58mm) 18 else 26
                val displayName = if (line.productNameSnapshot.length > maxNameChars) {
                    line.productNameSnapshot.take(maxNameChars - 1) + "…"
                } else {
                    line.productNameSnapshot
                }
                canvas.drawText(displayName, 14f, currentY, boldPaint)

                boldPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%,d Ks".format(line.lineTotalMinor), width - 14f, currentY, boldPaint)
                currentY += if (is58mm) 18f else 22f

                // Line 2: e.g. "1 အိတ် x 31,000 Ks"
                textPaint.textSize = if (is58mm) 14.5f else 17f
                textPaint.textAlign = Paint.Align.LEFT
                val unitStr = line.unitName.ifBlank { "ခု" }
                val subDetail = "${line.quantity} $unitStr x %,d Ks".format(line.unitPriceMinor)
                canvas.drawText(subDetail, 14f, currentY, textPaint)
                currentY += if (is58mm) 19f else 23f

                // Spacing between items (no row border lines!)
                currentY += 4f
            }
        }

        // Divider after Items
        drawDivider(template.gridLineStyle)

        // ==========================================
        // 3. TOTALS & SUMMARY SECTION
        // ==========================================
        val totalLabelSize = if (is58mm) 16f else 19f
        textPaint.textSize = totalLabelSize
        boldPaint.textSize = totalLabelSize + 2f

        // Subtotal (ကျသင့်ငွေ)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ကျသင့်ငွေ", 14f, currentY, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("%,d Ks".format(receipt.subtotalMinor), width - 14f, currentY, textPaint)
        currentY += totalLabelSize + 6f

        // Discount if any
        if (receipt.discountMinor > 0) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("လျှော့ငွေ (Discount)", 14f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("-%,d Ks".format(receipt.discountMinor), width - 14f, currentY, textPaint)
            currentY += totalLabelSize + 6f
        }

        // Delivery fee if any
        if (receipt.deliveryFeeMinor > 0) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ပို့ဆောင်ခ (Delivery)", 14f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("%,d Ks".format(receipt.deliveryFeeMinor), width - 14f, currentY, textPaint)
            currentY += totalLabelSize + 6f
        }

        // Grand Total (စုစုပေါင်း)
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("စုစုပေါင်း", 14f, currentY, boldPaint)
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("%,d Ks".format(receipt.totalMinor), width - 14f, currentY, boldPaint)
        currentY += totalLabelSize + 8f

        // Payment Details (Optional)
        if (template.showPaymentMethod) {
            textPaint.textSize = if (is58mm) 14.5f else 17f
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ပေးချေမှု (${receipt.paymentMethod}):", 14f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("%,d Ks".format(receipt.tenderedMinor), width - 14f, currentY, textPaint)
            currentY += 19f

            if (receipt.changeMinor > 0) {
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("ပြန်အမ်းငွေ:", 14f, currentY, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%,d Ks".format(receipt.changeMinor), width - 14f, currentY, textPaint)
                currentY += 19f
            }
        }

        // Divider after Totals
        drawDivider(template.gridLineStyle)

        // Voucher Number (ပြေစာအမှတ်: A261107-37416692)
        if (template.showVoucherNumber) {
            textPaint.textSize = if (is58mm) 15f else 18f
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ပြေစာအမှတ်: ${receipt.displayVoucherNumber}", 14f, currentY, textPaint)
            currentY += if (is58mm) 20f else 23f
        }

        // ==========================================
        // 4. FOOTER NOTICES & POLICIES
        // ==========================================
        val footerSize = when (template.footerFontSize.uppercase()) {
            "MEDIUM" -> if (is58mm) 16f else 19f
            "LARGE" -> if (is58mm) 18f else 22f
            else -> if (is58mm) 14.5f else 17f
        }
        textPaint.textSize = footerSize
        textPaint.textAlign = Paint.Align.CENTER

        // Return Policy Text (e.g. "ဝယ်ယူပြီး ပစ္စည်း များကို ပြန်လဲမပေးပါ")
        if (template.returnPolicyText.isNotBlank()) {
            template.returnPolicyText.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    canvas.drawText(line.trim(), width / 2f, currentY, textPaint)
                    currentY += footerSize + 5f
                }
            }
        }

        // Footer Message (e.g. "ဝယ်ယူအားပေးမှုကိုကျေးဇူးအထူးတင်ရှိပါသည်")
        val footerMsg = template.footerMessage.ifBlank { storeProfile.footerMessage }
        if (footerMsg.isNotBlank()) {
            footerMsg.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    canvas.drawText(line.trim(), width / 2f, currentY, textPaint)
                    currentY += footerSize + 5f
                }
            }
        }

        // Extra Notices (e.g. "သုံးမူး၊ ငါးမူး၊ ခြောက်မူး သံပင် များ ၊\nသွပ် အစားထိုး polystar များ ရပါသည် ။")
        if (template.extraNoticeText.isNotBlank()) {
            boldPaint.textSize = footerSize + 1f
            boldPaint.textAlign = Paint.Align.CENTER
            template.extraNoticeText.split("\n").forEach { noticeLine ->
                if (noticeLine.isNotBlank()) {
                    canvas.drawText(noticeLine.trim(), width / 2f, currentY, boldPaint)
                    currentY += footerSize + 6f
                }
            }
        }

        // Print Time (e.g. "Print Time: 07/11/2026 10:24AM")
        if (template.showPrintTime) {
            val printTimeStr = "Print Time: ${SimpleDateFormat("dd/MM/yyyy hh:mma", Locale.ENGLISH).format(Date(receipt.timestampEpochMs))}"
            textPaint.textSize = if (is58mm) 13f else 15f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(printTimeStr, width / 2f, currentY, textPaint)
            currentY += 18f
        }

        currentY += 18f

        // Crop bitmap to content height
        val finalHeight = currentY.toInt().coerceAtLeast(100)
        return Bitmap.createBitmap(tempBitmap, 0, 0, width, finalHeight)
    }

    /**
     * Converts an ARGB_8888 bitmap to high-contrast monochrome ESC/POS raster command (GS v 0).
     */
    fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val output = ByteArrayOutputStream()
        // ESC @ (Initialize printer)
        output.write(byteArrayOf(0x1B, 0x40))
        // ESC a 1 (Center alignment)
        output.write(byteArrayOf(0x1B, 0x61, 0x01))

        // GS v 0 m xL xH yL yH d1...dk
        val header = byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,
            (widthBytes and 0xFF).toByte(),
            ((widthBytes shr 8) and 0xFF).toByte(),
            (height and 0xFF).toByte(),
            ((height shr 8) and 0xFF).toByte()
        )
        output.write(header)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val threshold = 180 // Binarization luminance threshold

        for (y in 0 until height) {
            for (byteX in 0 until widthBytes) {
                var slice = 0
                for (b in 0 until 8) {
                    val px = (byteX * 8) + b
                    if (px < width) {
                        val color = pixels[(y * width) + px]
                        val r = (color shr 16) and 0xFF
                        val g = (color shr 8) and 0xFF
                        val bl = color and 0xFF
                        val lum = (0.299 * r + 0.587 * g + 0.114 * bl).toInt()
                        if (lum < threshold) {
                            slice = slice or (1 shl (7 - b)) // 1 = black dot
                        }
                    }
                }
                output.write(slice)
            }
        }

        // Line feeds & Cut
        output.write(byteArrayOf(0x1B, 0x64, 0x03)) // feed 3 lines
        output.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // GS V B 0 (Cut paper)
        return output.toByteArray()
    }
}
