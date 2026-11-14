package com.example.offlinepos.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.offlinepos.data.local.ReceiptTemplateEntity
import com.example.offlinepos.data.local.effectiveQuantity
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
     * Defaults to 80mm standard width.
     */
    fun generateEscPosBytes(
        context: Context,
        template: ReceiptTemplateEntity,
        receipt: ReceiptData,
        storeProfile: StoreProfile,
        paperWidth: PaperWidth = PaperWidth.WIDTH_80MM,
        cutMode: EscPos.PrinterCutMode = EscPos.PrinterCutMode.FEED_AND_CUT,
    ): ByteArray {
        val widthDots = if (template.paperWidthDots > 0) template.paperWidthDots else paperWidth.dots
        val bitmap = renderReceiptBitmap(
            template = template,
            receipt = receipt,
            storeProfile = storeProfile,
            targetWidthDots = widthDots
        )
        return try {
            bitmapToEscPosRaster(bitmap, cutMode = cutMode, kickDrawer = false)
        } finally {
            bitmap.recycle()
        }
    }

    fun generateEscPosForTemplate(
        template: ReceiptTemplateEntity,
        receipt: ReceiptData,
        profile: StoreProfile,
        paperWidthDots: Int = 576, // Default 80mm
        kickDrawer: Boolean = false,
        cutMode: EscPos.PrinterCutMode = EscPos.PrinterCutMode.FEED_AND_CUT,
    ): ByteArray {
        val widthDots = if (template.paperWidthDots > 0) template.paperWidthDots else paperWidthDots
        val bitmap = renderReceiptBitmap(
            template = template,
            receipt = receipt,
            storeProfile = profile,
            targetWidthDots = widthDots
        )
        return try {
            bitmapToEscPosRaster(bitmap, cutMode = cutMode, kickDrawer = kickDrawer)
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Renders a bitmap of the receipt based on the Custom Receipt Template Builder parameters.
     * Supports dynamic font scaling for all elements, custom line spacing, and 80mm/58mm paper sizes.
     */
    fun renderReceiptBitmap(
        template: ReceiptTemplateEntity,
        receipt: ReceiptData,
        storeProfile: StoreProfile,
        targetWidthDots: Int = 576, // 576 dots for standard 80mm, 384 dots for 58mm
    ): Bitmap {
        val width = if (template.paperWidthDots > 0) template.paperWidthDots else targetWidthDots
        val is58mm = width <= 420

        // Global font scaling (70% - 160%)
        val globalScale = (template.fontScalePercent.coerceIn(60, 200) / 100f)

        // Custom line spacing extra dp (0 - 24dp)
        val extraLineSpacing = template.lineSpacingDp.toFloat().coerceIn(0f, 24f)

        // Helper to resolve font sizes per section with global scale applied
        fun getSectionFontSize(
            sizeKey: String,
            small: Float,
            medium: Float,
            large: Float,
            xlarge: Float,
            xxlarge: Float = xlarge * 1.2f,
        ): Float {
            val base = when (sizeKey.uppercase()) {
                "SMALL" -> small
                "MEDIUM" -> medium
                "LARGE" -> large
                "XLARGE" -> xlarge
                "XXLARGE" -> xxlarge
                else -> medium
            }
            return base * globalScale
        }

        // Section Font Sizes
        val storeNameSize = getSectionFontSize(
            template.storeNameFontSize,
            small = if (is58mm) 19f else 24f,
            medium = if (is58mm) 22f else 28f,
            large = if (is58mm) 26f else 33f,
            xlarge = if (is58mm) 30f else 39f,
            xxlarge = if (is58mm) 35f else 46f,
        )

        val headerDetailsSize = getSectionFontSize(
            template.headerDetailsFontSize,
            small = if (is58mm) 13.5f else 16.5f,
            medium = if (is58mm) 15.5f else 19f,
            large = if (is58mm) 18f else 22.5f,
            xlarge = if (is58mm) 21f else 26.5f,
        )

        val metaSize = getSectionFontSize(
            template.metaFontSize,
            small = if (is58mm) 13f else 16f,
            medium = if (is58mm) 15f else 18.5f,
            large = if (is58mm) 17.5f else 21.5f,
            xlarge = if (is58mm) 20.5f else 25f,
        )

        val bodySize = getSectionFontSize(
            template.bodyFontSize,
            small = if (is58mm) 13.5f else 16.5f,
            medium = if (is58mm) 16f else 20f,
            large = if (is58mm) 19f else 24f,
            xlarge = if (is58mm) 22.5f else 28.5f,
        )

        val totalsSize = getSectionFontSize(
            template.totalsFontSize,
            small = if (is58mm) 14f else 17.5f,
            medium = if (is58mm) 16.5f else 21f,
            large = if (is58mm) 19.5f else 25f,
            xlarge = if (is58mm) 23f else 29.5f,
        )

        val footerSize = getSectionFontSize(
            template.footerFontSize,
            small = if (is58mm) 13f else 16f,
            medium = if (is58mm) 15f else 18.5f,
            large = if (is58mm) 17.5f else 22f,
            xlarge = if (is58mm) 20.5f else 26f,
        )

        // Pre-measure height to avoid bitmap overflow
        val estimatedLines = 45 + (receipt.items.size * 3)
        val estimatedHeight = ((estimatedLines * ((if (is58mm) 24 else 32) + extraLineSpacing.toInt())) * globalScale).toInt() + 600
        val tempBitmap = Bitmap.createBitmap(width, estimatedHeight.coerceAtLeast(350), Bitmap.Config.ARGB_8888)
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

        var currentY = if (is58mm) 22f else 28f

        fun drawDivider(style: String = template.gridLineStyle) {
            when (style.uppercase()) {
                "DASHED" -> {
                    var x = 12f
                    val dashLen = if (is58mm) 6f else 8f
                    val spaceLen = if (is58mm) 4f else 5.5f
                    while (x < width - 12f) {
                        canvas.drawLine(x, currentY, minOf(x + dashLen, width - 12f), currentY, linePaint)
                        x += (dashLen + spaceLen)
                    }
                    currentY += (if (is58mm) 12f else 15f) + (extraLineSpacing * 0.5f)
                }
                "SOLID" -> {
                    canvas.drawLine(12f, currentY, width - 12f, currentY, linePaint)
                    currentY += (if (is58mm) 10f else 13f) + (extraLineSpacing * 0.5f)
                }
                "DOUBLE" -> {
                    canvas.drawLine(12f, currentY, width - 12f, currentY, linePaint)
                    canvas.drawLine(12f, currentY + 3.5f, width - 12f, currentY + 3.5f, linePaint)
                    currentY += (if (is58mm) 14f else 17f) + (extraLineSpacing * 0.5f)
                }
                else -> {
                    // NONE: No row/column lines drawn at all
                    currentY += (if (is58mm) 4f else 6f) + (extraLineSpacing * 0.3f)
                }
            }
        }

        // ==========================================
        // 1. HEADER SECTION
        // ==========================================
        val storeNameText = template.customStoreName.ifBlank {
            storeProfile.storeName.ifBlank { "ဆန်းသစ်" }
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
        currentY += storeNameSize + 6f + extraLineSpacing

        // Custom Subtitle (e.g. "အိမ်ဆောက်ပစ္စည်း အရောင်းဆိုင်")
        if (template.customSubtitle.isNotBlank()) {
            boldPaint.textSize = headerDetailsSize + 1.5f
            boldPaint.textAlign = Paint.Align.CENTER
            template.customSubtitle.split("\n").forEach { sub ->
                if (sub.isNotBlank()) {
                    canvas.drawText(sub.trim(), width / 2f, currentY, boldPaint)
                    currentY += headerDetailsSize + 4f + extraLineSpacing
                }
            }
        }

        // Custom Owner or Contact Info (e.g. "ကိုပါပီး+မသီရိကလျာ")
        if (template.customOwnerOrContact.isNotBlank()) {
            textPaint.textSize = headerDetailsSize
            textPaint.textAlign = Paint.Align.CENTER
            template.customOwnerOrContact.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    canvas.drawText(line.trim(), width / 2f, currentY, textPaint)
                    currentY += headerDetailsSize + 3.5f + extraLineSpacing
                }
            }
        }

        // Phone Numbers
        if (template.showPhone) {
            val phoneText = template.customPhone.ifBlank { storeProfile.phone }
            if (phoneText.isNotBlank()) {
                textPaint.textSize = headerDetailsSize
                textPaint.textAlign = Paint.Align.CENTER
                phoneText.split("\n").forEach { pLine ->
                    if (pLine.isNotBlank()) {
                        canvas.drawText(pLine.trim(), width / 2f, currentY, textPaint)
                        currentY += headerDetailsSize + 3.5f + extraLineSpacing
                    }
                }
            }
        }

        // Address
        if (template.showAddress) {
            val addressText = template.customAddress.ifBlank { storeProfile.address }
            if (addressText.isNotBlank()) {
                textPaint.textSize = headerDetailsSize - 0.5f
                textPaint.textAlign = Paint.Align.CENTER
                addressText.split("\n").forEach { aLine ->
                    if (aLine.isNotBlank()) {
                        canvas.drawText(aLine.trim(), width / 2f, currentY, textPaint)
                        currentY += headerDetailsSize + 3f + extraLineSpacing
                    }
                }
            }
        }

        // Header Note (Optional)
        if (template.showHeaderNote) {
            val note = template.customHeaderNote.ifBlank { storeProfile.headerNote }
            if (note.isNotBlank()) {
                textPaint.textSize = headerDetailsSize
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(note, width / 2f, currentY, textPaint)
                currentY += headerDetailsSize + 4f + extraLineSpacing
            }
        }

        // Metadata: Date, Customer, Cashier
        textPaint.textSize = metaSize
        boldPaint.textSize = metaSize

        // Date & Cashier
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ရက်စွဲ: ${receipt.businessDate}", 14f, currentY, textPaint)
        if (template.showCashierInfo) {
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("တာဝန်ခံ: ${receipt.cashierId}", width - 14f, currentY, textPaint)
        }
        currentY += metaSize + 5f + extraLineSpacing

        // Customer
        if (template.showCustomerInfo) {
            val cLabel = template.customerLabel.ifBlank { "ဈေးဝယ်သူ" }
            val cName = receipt.customerName?.ifBlank { null } ?: "MK store"
            boldPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("$cLabel: $cName", 14f, currentY, boldPaint)
            currentY += metaSize + 6f + extraLineSpacing
        }

        // Divider before Items
        drawDivider(template.gridLineStyle)

        // ==========================================
        // 2. BODY SECTION
        // ==========================================
        val pad = template.rowPaddingDp.toFloat() + (extraLineSpacing * 0.4f)
        boldPaint.textSize = bodySize
        textPaint.textSize = bodySize

        if (template.itemLayoutStyle == "COLUMNS_4") {
            // 4-Column Layout (Template 2, 3, 4, 5)
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
            currentY += bodySize + 7f + extraLineSpacing

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
                canvas.drawText(com.example.offlinepos.domain.Money.formatQty(line.effectiveQuantity()), x1 + (colQtyW / 2f), currentY, textPaint)

                // Unit Price
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%,d".format(line.unitPriceMinor), x2 + colPriceW, currentY, textPaint)

                // Total
                boldPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%,d".format(line.lineTotalMinor), xEnd, currentY, boldPaint)

                currentY += bodySize + pad + 3f + extraLineSpacing
            }
        } else {
            // STANDARD_2LINE Layout (Template 1 - Uploaded Sample Style)
            // No column header table! Each item has Line 1 (Name & Amount) + Line 2 (Qty x Price)
            receipt.items.forEach { line ->
                // Line 1: Product Name on left, Line Total on right
                boldPaint.textSize = bodySize
                boldPaint.textAlign = Paint.Align.LEFT
                val maxNameChars = if (is58mm) (20 / globalScale).toInt().coerceAtLeast(10) else (32 / globalScale).toInt().coerceAtLeast(16)
                val displayName = if (line.productNameSnapshot.length > maxNameChars) {
                    line.productNameSnapshot.take(maxNameChars - 1) + "…"
                } else {
                    line.productNameSnapshot
                }
                canvas.drawText(displayName, 14f, currentY, boldPaint)

                boldPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%,d Ks".format(line.lineTotalMinor), width - 14f, currentY, boldPaint)
                currentY += bodySize + 3f + extraLineSpacing

                // Line 2: e.g. "1 အိတ် x 31,000 Ks"
                val subDetailSize = bodySize * 0.88f
                textPaint.textSize = subDetailSize
                textPaint.textAlign = Paint.Align.LEFT
                val unitStr = line.unitName.ifBlank { "ခု" }
                val subDetail = "${com.example.offlinepos.domain.Money.formatQty(line.effectiveQuantity())} $unitStr x %,d Ks".format(line.unitPriceMinor)
                canvas.drawText(subDetail, 14f, currentY, textPaint)
                currentY += subDetailSize + 4f + extraLineSpacing

                // Spacing between items
                currentY += 4f + (extraLineSpacing * 0.5f)
            }
        }

        // Divider after Items
        drawDivider(template.gridLineStyle)

        // ==========================================
        // 3. TOTALS & SUMMARY SECTION
        // ==========================================
        textPaint.textSize = totalsSize
        boldPaint.textSize = totalsSize + 2f

        // Subtotal (ကျသင့်ငွေ)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ကျသင့်ငွေ", 14f, currentY, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("%,d Ks".format(receipt.subtotalMinor), width - 14f, currentY, textPaint)
        currentY += totalsSize + 6f + extraLineSpacing

        // Discount if any
        if (receipt.discountMinor > 0) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("လျှော့ငွေ (Discount)", 14f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("-%,d Ks".format(receipt.discountMinor), width - 14f, currentY, textPaint)
            currentY += totalsSize + 6f + extraLineSpacing
        }

        // Delivery fee if any
        if (receipt.deliveryFeeMinor > 0) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ပို့ဆောင်ခ (Delivery)", 14f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("%,d Ks".format(receipt.deliveryFeeMinor), width - 14f, currentY, textPaint)
            currentY += totalsSize + 6f + extraLineSpacing
        }

        // Grand Total (စုစုပေါင်း)
        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("စုစုပေါင်း", 14f, currentY, boldPaint)
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("%,d Ks".format(receipt.totalMinor), width - 14f, currentY, boldPaint)
        currentY += totalsSize + 8f + extraLineSpacing

        // Payment Details (Optional)
        if (template.showPaymentMethod) {
            textPaint.textSize = totalsSize - 1.5f
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ပေးချေမှု (${receipt.paymentMethod}):", 14f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("%,d Ks".format(receipt.tenderedMinor), width - 14f, currentY, textPaint)
            currentY += totalsSize + 4f + extraLineSpacing

            if (receipt.changeMinor > 0) {
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("ပြန်အမ်းငွေ:", 14f, currentY, textPaint)
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText("%,d Ks".format(receipt.changeMinor), width - 14f, currentY, textPaint)
                currentY += totalsSize + 4f + extraLineSpacing
            }
        }

        // Divider after Totals
        drawDivider(template.gridLineStyle)

        // Voucher Number (ပြေစာအမှတ်: A261107-37416692)
        if (template.showVoucherNumber) {
            textPaint.textSize = metaSize
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ပြေစာအမှတ်: ${receipt.displayVoucherNumber}", 14f, currentY, textPaint)
            currentY += metaSize + 5f + extraLineSpacing
        }

        // ==========================================
        // 4. FOOTER NOTICES & POLICIES
        // ==========================================
        textPaint.textSize = footerSize
        textPaint.textAlign = Paint.Align.CENTER

        // Return Policy Text (e.g. "ဝယ်ယူပြီး ပစ္စည်း များကို ပြန်လဲမပေးပါ")
        if (template.returnPolicyText.isNotBlank()) {
            template.returnPolicyText.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    canvas.drawText(line.trim(), width / 2f, currentY, textPaint)
                    currentY += footerSize + 5f + extraLineSpacing
                }
            }
        }

        // Footer Message (e.g. "ဝယ်ယူအားပေးမှုကိုကျေးဇူးအထူးတင်ရှိပါသည်")
        val footerMsg = template.footerMessage.ifBlank { storeProfile.footerMessage }
        if (footerMsg.isNotBlank()) {
            footerMsg.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    canvas.drawText(line.trim(), width / 2f, currentY, textPaint)
                    currentY += footerSize + 5f + extraLineSpacing
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
                    currentY += footerSize + 6f + extraLineSpacing
                }
            }
        }

        // Print Time (e.g. "Print Time: 07/11/2026 10:24AM")
        if (template.showPrintTime) {
            val printTimeStr = "Print Time: ${SimpleDateFormat("dd/MM/yyyy hh:mma", Locale.ENGLISH).format(Date(receipt.timestampEpochMs))}"
            textPaint.textSize = footerSize - 1.5f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(printTimeStr, width / 2f, currentY, textPaint)
            currentY += footerSize + 4f + extraLineSpacing
        }

        currentY += 18f + extraLineSpacing

        // Crop bitmap to exact content height
        val finalHeight = currentY.toInt().coerceIn(100, tempBitmap.height)
        val cropped = Bitmap.createBitmap(tempBitmap, 0, 0, width, finalHeight)
        if (cropped !== tempBitmap) tempBitmap.recycle()
        return cropped
    }

    /**
     * Converts an ARGB_8888 bitmap into a buffer-safe banded monochrome ESC/POS raster payload (GS v 0).
     */
    fun bitmapToEscPosRaster(
        bitmap: Bitmap,
        cutMode: EscPos.PrinterCutMode = EscPos.PrinterCutMode.FEED_AND_CUT,
        kickDrawer: Boolean = false,
    ): ByteArray {
        val (widthBytes, height, rawBits) = BluetoothPrinterHelper.bitmapToMonochromeBytes(
            bitmap = bitmap,
            luminanceThreshold = 180,
        )
        return EscPos.buildRasterReceiptPayload(
            bitmapWidthBytes = widthBytes,
            bitmapHeightDots = height,
            data = rawBits,
            feedLines = 5,
            cutMode = cutMode,
            kickDrawer = kickDrawer,
        )
    }
}
