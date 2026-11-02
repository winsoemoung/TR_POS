package com.example.offlinepos.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.offlinepos.data.local.ReceiptTemplateEntity
import com.example.offlinepos.domain.CartTotals
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
        paperWidth: PaperWidth = PaperWidth.WIDTH_80MM,
    ): ByteArray {
        val bitmap = renderReceiptBitmap(
            template = template,
            receipt = receipt,
            storeProfile = storeProfile,
            targetWidthDots = paperWidth.dots
        )
        return bitmapToEscPosRaster(bitmap)
    }

    fun generateEscPosForTemplate(
        template: ReceiptTemplateEntity,
        receipt: ReceiptData,
        profile: StoreProfile,
        paperWidthDots: Int = 576,
        kickDrawer: Boolean = false,
    ): ByteArray {
        val bitmap = renderReceiptBitmap(
            template = template,
            receipt = receipt,
            storeProfile = profile,
            targetWidthDots = paperWidthDots
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
        targetWidthDots: Int = 576, // 576 dots for 80mm, 384 dots for 58mm
    ): Bitmap {
        val width = targetWidthDots
        // Pre-measure height
        val estimatedLines = 25 + (receipt.items.size * 3)
        val estimatedHeight = (estimatedLines * 30) + 300
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
            strokeWidth = 1.5f
        }

        var currentY = 24f

        fun drawDivider(style: String = template.gridLineStyle) {
            when (style.uppercase()) {
                "DASHED" -> {
                    var x = 10f
                    while (x < width - 10) {
                        canvas.drawLine(x, currentY, x + 8f, currentY, linePaint)
                        x += 14f
                    }
                    currentY += 12f
                }
                "SOLID" -> {
                    canvas.drawLine(10f, currentY, width - 10f, currentY, linePaint)
                    currentY += 12f
                }
                "DOUBLE" -> {
                    canvas.drawLine(10f, currentY, width - 10f, currentY, linePaint)
                    canvas.drawLine(10f, currentY + 4f, width - 10f, currentY + 4f, linePaint)
                    currentY += 16f
                }
                else -> {
                    currentY += 8f
                }
            }
        }

        // ==========================================
        // 1. HEADER SECTION
        // ==========================================
        // Store Name
        val storeNameSize = when (template.storeNameFontSize.uppercase()) {
            "XLARGE" -> 36f
            "LARGE" -> 30f
            "SMALL" -> 22f
            else -> 26f
        }
        boldPaint.textSize = storeNameSize
        boldPaint.textAlign = when (template.storeNameAlign.uppercase()) {
            "LEFT" -> Paint.Align.LEFT
            "RIGHT" -> Paint.Align.RIGHT
            else -> Paint.Align.CENTER
        }
        val headerX = when (template.storeNameAlign.uppercase()) {
            "LEFT" -> 16f
            "RIGHT" -> width - 16f
            else -> width / 2f
        }
        canvas.drawText(storeProfile.storeName.ifBlank { "TR-POS" }, headerX, currentY, boldPaint)
        currentY += storeNameSize + 6f

        // Custom Header Note / Subtitle
        if (template.showHeaderNote) {
            val note = template.customHeaderNote.ifBlank { storeProfile.headerNote }
            if (note.isNotBlank()) {
                textPaint.textSize = 20f
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(note, width / 2f, currentY, textPaint)
                currentY += 24f
            }
        }

        // Address & Phone
        textPaint.textSize = 19f
        textPaint.textAlign = Paint.Align.CENTER
        if (template.showAddress && storeProfile.address.isNotBlank()) {
            canvas.drawText(storeProfile.address, width / 2f, currentY, textPaint)
            currentY += 24f
        }
        if (template.showPhone && storeProfile.phone.isNotBlank()) {
            canvas.drawText("ဖုန်း: ${storeProfile.phone}", width / 2f, currentY, textPaint)
            currentY += 24f
        }

        drawDivider("SOLID")

        // Metadata: Voucher Number, Date, Cashier, Customer
        textPaint.textSize = 18f
        if (template.showVoucherNumber) {
            boldPaint.textSize = 20f
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ဘောင်ချာအမှတ်: ${receipt.displayVoucherNumber}", 14f, currentY, boldPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Ref: #${receipt.saleId.take(8).uppercase()}", width - 14f, currentY, textPaint)
            currentY += 24f
        }

        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("နေ့စွဲ: ${receipt.businessDate}", 14f, currentY, textPaint)
        if (template.showCashierInfo) {
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("တာဝန်ခံ: ${receipt.cashierId}", width - 14f, currentY, textPaint)
        }
        currentY += 24f

        if (template.showCustomerInfo && !receipt.customerName.isNullOrBlank()) {
            boldPaint.textSize = 18f
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ဝယ်သူအမည်: ${receipt.customerName}", 14f, currentY, boldPaint)
            currentY += 24f
        }

        // ==========================================
        // 2. BODY SECTION (Excel-like Grid Table)
        // ==========================================
        drawDivider("SOLID")

        val pad = template.rowPaddingDp.toFloat()
        val bodySize = when (template.bodyFontSize.uppercase()) {
            "LARGE" -> 22f
            "SMALL" -> 16f
            else -> 19f
        }
        boldPaint.textSize = bodySize
        textPaint.textSize = bodySize

        // Calculate Column Widths from weights
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

        // Table Header
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ပစ္စည်းအမည်", x0, currentY, boldPaint)
        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("ဦးရေ", x1 + (colQtyW / 2f), currentY, boldPaint)
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("နှုန်း", x2 + colPriceW, currentY, boldPaint)
        canvas.drawText("သင့်ငွေ", xEnd, currentY, boldPaint)
        currentY += bodySize + 8f

        drawDivider(template.gridLineStyle)

        // Item Rows
        receipt.items.forEach { line ->
            currentY += pad

            // Item Name (handles text wrap if needed)
            textPaint.textAlign = when (template.itemNameAlign.uppercase()) {
                "CENTER" -> Paint.Align.CENTER
                "RIGHT" -> Paint.Align.RIGHT
                else -> Paint.Align.LEFT
            }
            val nameX = when (template.itemNameAlign.uppercase()) {
                "CENTER" -> x0 + (colNameW / 2f)
                "RIGHT" -> x1 - 4f
                else -> x0
            }
            val maxChars = (colNameW / (bodySize * 0.6f)).toInt().coerceAtLeast(8)
            val displayName = if (line.productNameSnapshot.length > maxChars) line.productNameSnapshot.take(maxChars - 1) + "…" else line.productNameSnapshot
            canvas.drawText(displayName, nameX, currentY, textPaint)

            // Qty
            textPaint.textAlign = when (template.qtyAlign.uppercase()) {
                "LEFT" -> Paint.Align.LEFT
                "RIGHT" -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            val qtyX = when (template.qtyAlign.uppercase()) {
                "LEFT" -> x1 + 4f
                "RIGHT" -> x2 - 4f
                else -> x1 + (colQtyW / 2f)
            }
            canvas.drawText(line.quantity.toString(), qtyX, currentY, textPaint)

            // Unit Price
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("%,d".format(line.unitPriceMinor), x2 + colPriceW, currentY, textPaint)

            // Line Total
            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("%,d".format(line.lineTotalMinor), xEnd, currentY, boldPaint)

            currentY += bodySize + pad + 4f
        }

        drawDivider("SOLID")

        // ==========================================
        // 3. FOOTER SECTION
        // ==========================================
        if (template.showTotalsBreakdown) {
            textPaint.textSize = 19f
            boldPaint.textSize = 21f

            // Subtotal
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("စုစုပေါင်းကျသင့်ငွေ:", 14f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("%,d Ks".format(receipt.subtotalMinor), width - 14f, currentY, textPaint)
            currentY += 24f

            if (receipt.discountMinor > 0) {
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("လျှော့ငွေ (Discount):", 14f, currentY, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("-%,d Ks".format(receipt.discountMinor), width - 14f, currentY, textPaint)
                currentY += 24f
            }

            if (receipt.deliveryFeeMinor > 0) {
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("ပို့ဆောင်ခ (Delivery):", 14f, currentY, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%,d Ks".format(receipt.deliveryFeeMinor), width - 14f, currentY, textPaint)
                currentY += 24f
            }

            drawDivider("DASHED")

            // Grand Total
            boldPaint.textSize = 24f
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("အသားတင် စုစုပေါင်း (TOTAL):", 14f, currentY, boldPaint)
            boldPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("%,d Ks".format(receipt.totalMinor), width - 14f, currentY, boldPaint)
            currentY += 30f

            if (template.showPaymentMethod) {
                textPaint.textSize = 18f
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("ပေးချေမှုပုံစံ (${receipt.paymentMethod}):", 14f, currentY, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%,d Ks".format(receipt.tenderedMinor), width - 14f, currentY, textPaint)
                currentY += 22f

                if (receipt.changeMinor > 0) {
                    textPaint.textAlign = Paint.Align.LEFT
                    canvas.drawText("ပြန်အမ်းငွေ (Change):", 14f, currentY, textPaint)
                    textPaint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("%,d Ks".format(receipt.changeMinor), width - 14f, currentY, textPaint)
                    currentY += 22f
                }
            }
        }

        // Pending pickup status if applicable
        if (receipt.pickupStatus == "PENDING_PICKUP") {
            currentY += 6f
            boldPaint.textSize = 19f
            boldPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("📦 [ပစ္စည်း မယူရသေးပါ - PENDING PICKUP]", width / 2f, currentY, boldPaint)
            currentY += 22f
            textPaint.textSize = 17f
            canvas.drawText("ထုတ်ယူမည့်ရက်စွဲ: ${receipt.expectedPickupDate ?: "မသိရသေးပါ"}", width / 2f, currentY, textPaint)
            currentY += 22f
        }

        drawDivider("SOLID")

        // Footer Thank-You & Return Policy
        val footerMsg = template.footerMessage.ifBlank { storeProfile.footerMessage }
        if (footerMsg.isNotBlank()) {
            val footerSize = when (template.footerFontSize.uppercase()) {
                "MEDIUM" -> 19f
                "LARGE" -> 22f
                else -> 17f
            }
            textPaint.textSize = footerSize
            textPaint.textAlign = when (template.footerAlign.uppercase()) {
                "LEFT" -> Paint.Align.LEFT
                "RIGHT" -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            val footX = when (template.footerAlign.uppercase()) {
                "LEFT" -> 14f
                "RIGHT" -> width - 14f
                else -> width / 2f
            }
            footerMsg.split("\n").forEach { line ->
                canvas.drawText(line, footX, currentY, textPaint)
                currentY += footerSize + 4f
            }
        }

        if (template.returnPolicyText.isNotBlank()) {
            textPaint.textSize = 15f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(template.returnPolicyText, width / 2f, currentY, textPaint)
            currentY += 20f
        }

        currentY += 20f

        // Trim bitmap to actual content height
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
