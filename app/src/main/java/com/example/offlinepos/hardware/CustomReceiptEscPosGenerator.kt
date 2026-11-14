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

    private data class ResolvedTypography(
        val width: Int,
        val is58mm: Boolean,
        val globalScale: Float,
        val extraLineSpacing: Float,
        val storeNameSize: Float,
        val headerDetailsSize: Float,
        val metaSize: Float,
        val bodySize: Float,
        val totalsSize: Float,
        val footerSize: Float,
    )

    private fun resolveTypography(
        template: ReceiptTemplateEntity,
        targetWidthDots: Int,
    ): ResolvedTypography {
        val width = if (template.paperWidthDots > 0) template.paperWidthDots else targetWidthDots
        val is58mm = width <= 420
        val globalScale = template.fontScalePercent.coerceIn(60, 200) / 100f
        val extraLineSpacing = template.lineSpacingDp.toFloat().coerceIn(0f, 24f)

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

        return ResolvedTypography(
            width = width,
            is58mm = is58mm,
            globalScale = globalScale,
            extraLineSpacing = extraLineSpacing,
            storeNameSize = getSectionFontSize(
                template.storeNameFontSize,
                small = if (is58mm) 19f else 24f,
                medium = if (is58mm) 22f else 28f,
                large = if (is58mm) 26f else 33f,
                xlarge = if (is58mm) 30f else 39f,
                xxlarge = if (is58mm) 35f else 46f,
            ),
            headerDetailsSize = getSectionFontSize(
                template.headerDetailsFontSize,
                small = if (is58mm) 13.5f else 16.5f,
                medium = if (is58mm) 15.5f else 19f,
                large = if (is58mm) 18f else 22.5f,
                xlarge = if (is58mm) 21f else 26.5f,
            ),
            metaSize = getSectionFontSize(
                template.metaFontSize,
                small = if (is58mm) 13f else 16f,
                medium = if (is58mm) 15f else 18.5f,
                large = if (is58mm) 17.5f else 21.5f,
                xlarge = if (is58mm) 20.5f else 25f,
            ),
            bodySize = getSectionFontSize(
                template.bodyFontSize,
                small = if (is58mm) 13.5f else 16.5f,
                medium = if (is58mm) 16f else 20f,
                large = if (is58mm) 19f else 24f,
                xlarge = if (is58mm) 22.5f else 28.5f,
            ),
            totalsSize = getSectionFontSize(
                template.totalsFontSize,
                small = if (is58mm) 14f else 17.5f,
                medium = if (is58mm) 16.5f else 21f,
                large = if (is58mm) 19.5f else 25f,
                xlarge = if (is58mm) 23f else 29.5f,
            ),
            footerSize = getSectionFontSize(
                template.footerFontSize,
                small = if (is58mm) 13f else 16f,
                medium = if (is58mm) 15f else 18.5f,
                large = if (is58mm) 17.5f else 22f,
                xlarge = if (is58mm) 20.5f else 26f,
            ),
        )
    }

    private fun dividerAdvance(style: String, is58mm: Boolean, extraLineSpacing: Float): Float =
        when (style.uppercase()) {
            "DASHED" -> (if (is58mm) 12f else 15f) + (extraLineSpacing * 0.5f)
            "SOLID" -> (if (is58mm) 10f else 13f) + (extraLineSpacing * 0.5f)
            "DOUBLE" -> (if (is58mm) 14f else 17f) + (extraLineSpacing * 0.5f)
            else -> (if (is58mm) 4f else 6f) + (extraLineSpacing * 0.3f)
        }

    /**
     * Dynamically calculates exact physical receipt height in printer dots from the template sections,
     * typography scaling, line spacing, and actual number of line items, preventing oversized allocations
     * or max-length / endless paper feed errors.
     */
    fun calculatePhysicalHeightDots(
        template: ReceiptTemplateEntity,
        receipt: ReceiptData,
        storeProfile: StoreProfile,
        targetWidthDots: Int = 576,
    ): Int {
        val typo = resolveTypography(template, targetWidthDots)
        val extra = typo.extraLineSpacing
        val is58mm = typo.is58mm

        // Top margin must clear store name ascenders (above baseline)
        val topMargin = if (is58mm) 14f else 18f
        var y = topMargin + typo.storeNameSize
        var lastDrawnBaseline = y
        var lastDescenderExtent = typo.storeNameSize * 0.35f

        fun advanceLine(step: Float, drawnTextSize: Float) {
            lastDrawnBaseline = y
            lastDescenderExtent = drawnTextSize * 0.35f
            y += step
        }

        // 1. Header section (store name already positioned at y)
        advanceLine(typo.storeNameSize + 6f + extra, typo.storeNameSize)
        if (template.customSubtitle.isNotBlank()) {
            template.customSubtitle.split("\n").forEach { sub ->
                if (sub.isNotBlank()) {
                    advanceLine(typo.headerDetailsSize + 4f + extra, typo.headerDetailsSize + 1.5f)
                }
            }
        }
        if (template.customOwnerOrContact.isNotBlank()) {
            template.customOwnerOrContact.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    advanceLine(typo.headerDetailsSize + 3.5f + extra, typo.headerDetailsSize)
                }
            }
        }
        if (template.showPhone) {
            val phoneText = template.customPhone.ifBlank { storeProfile.phone }
            phoneText.split("\n").forEach { pLine ->
                if (pLine.isNotBlank()) {
                    advanceLine(typo.headerDetailsSize + 3.5f + extra, typo.headerDetailsSize)
                }
            }
        }
        if (template.showAddress) {
            val addressText = template.customAddress.ifBlank { storeProfile.address }
            addressText.split("\n").forEach { aLine ->
                if (aLine.isNotBlank()) {
                    advanceLine(typo.headerDetailsSize + 3f + extra, typo.headerDetailsSize - 0.5f)
                }
            }
        }
        if (template.showHeaderNote) {
            val note = template.customHeaderNote.ifBlank { storeProfile.headerNote }
            if (note.isNotBlank()) {
                advanceLine(typo.headerDetailsSize + 4f + extra, typo.headerDetailsSize)
            }
        }

        // Date & Cashier + optional Customer
        advanceLine(typo.metaSize + 5f + extra, typo.metaSize)
        if (template.showCustomerInfo) {
            advanceLine(typo.metaSize + 6f + extra, typo.metaSize)
        }
        y += dividerAdvance(template.gridLineStyle, is58mm, extra)

        // 2. Body items section
        val pad = template.rowPaddingDp.toFloat() + (extra * 0.4f)
        if (template.itemLayoutStyle == "COLUMNS_4") {
            advanceLine(typo.bodySize + 7f + extra, typo.bodySize)
            if (template.gridLineStyle != "NONE") {
                y += dividerAdvance(template.gridLineStyle, is58mm, extra)
            }
            repeat(receipt.items.size) {
                y += pad
                advanceLine(typo.bodySize + pad + 3f + extra, typo.bodySize)
            }
        } else {
            val subDetailSize = typo.bodySize * 0.88f
            repeat(receipt.items.size) {
                advanceLine(typo.bodySize + 3f + extra, typo.bodySize)
                advanceLine(subDetailSize + 4f + extra, subDetailSize)
                y += 4f + (extra * 0.5f)
            }
        }
        y += dividerAdvance(template.gridLineStyle, is58mm, extra)

        // 3. Totals & summary
        advanceLine(typo.totalsSize + 6f + extra, typo.totalsSize)
        if (receipt.discountMinor > 0) {
            advanceLine(typo.totalsSize + 6f + extra, typo.totalsSize)
        }
        if (receipt.deliveryFeeMinor > 0) {
            advanceLine(typo.totalsSize + 6f + extra, typo.totalsSize)
        }
        advanceLine(typo.totalsSize + 8f + extra, typo.totalsSize + 2f)
        if (template.showPaymentMethod) {
            advanceLine(typo.totalsSize + 4f + extra, typo.totalsSize - 1.5f)
            if (receipt.changeMinor > 0) {
                advanceLine(typo.totalsSize + 4f + extra, typo.totalsSize - 1.5f)
            }
        }
        y += dividerAdvance(template.gridLineStyle, is58mm, extra)

        // 4. Voucher & footer notices
        if (template.showVoucherNumber) {
            advanceLine(typo.metaSize + 5f + extra, typo.metaSize)
        }
        if (template.returnPolicyText.isNotBlank()) {
            template.returnPolicyText.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    advanceLine(typo.footerSize + 5f + extra, typo.footerSize)
                }
            }
        }
        val footerMsg = template.footerMessage.ifBlank { storeProfile.footerMessage }
        if (footerMsg.isNotBlank()) {
            footerMsg.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    advanceLine(typo.footerSize + 5f + extra, typo.footerSize)
                }
            }
        }
        if (template.extraNoticeText.isNotBlank()) {
            template.extraNoticeText.split("\n").forEach { noticeLine ->
                if (noticeLine.isNotBlank()) {
                    advanceLine(typo.footerSize + 6f + extra, typo.footerSize + 1f)
                }
            }
        }
        if (template.showPrintTime) {
            advanceLine(typo.footerSize + 4f + extra, typo.footerSize - 1.5f)
        }

        val compactBottomMargin = (lastDescenderExtent + 8f).coerceAtLeast(10f)
        return (lastDrawnBaseline + compactBottomMargin).toInt().coerceAtLeast(100)
    }

    /**
     * Renders a bitmap of the receipt based on the Custom Receipt Template Builder parameters.
     * Calculates exact physical paper height dynamically from line items and active sections before allocating.
     */
    fun renderReceiptBitmap(
        template: ReceiptTemplateEntity,
        receipt: ReceiptData,
        storeProfile: StoreProfile,
        targetWidthDots: Int = 576, // 576 dots for standard 80mm, 384 dots for 58mm
    ): Bitmap {
        val typo = resolveTypography(template, targetWidthDots)
        val width = typo.width
        val is58mm = typo.is58mm
        val globalScale = typo.globalScale
        val extraLineSpacing = typo.extraLineSpacing

        val storeNameSize = typo.storeNameSize
        val headerDetailsSize = typo.headerDetailsSize
        val metaSize = typo.metaSize
        val bodySize = typo.bodySize
        val totalsSize = typo.totalsSize
        val footerSize = typo.footerSize

        val physicalHeightDots = calculatePhysicalHeightDots(
            template = template,
            receipt = receipt,
            storeProfile = storeProfile,
            targetWidthDots = targetWidthDots,
        )
        val bitmap = Bitmap.createBitmap(width, physicalHeightDots, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
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

        val topMargin = if (is58mm) 14f else 18f
        var currentY = topMargin + storeNameSize

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
                }
                "SOLID" -> {
                    canvas.drawLine(12f, currentY, width - 12f, currentY, linePaint)
                }
                "DOUBLE" -> {
                    canvas.drawLine(12f, currentY, width - 12f, currentY, linePaint)
                    canvas.drawLine(12f, currentY + 3.5f, width - 12f, currentY + 3.5f, linePaint)
                }
            }
            currentY += dividerAdvance(style, is58mm, extraLineSpacing)
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
            receipt.items.forEach { line ->
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

                val subDetailSize = bodySize * 0.88f
                textPaint.textSize = subDetailSize
                textPaint.textAlign = Paint.Align.LEFT
                val unitStr = line.unitName.ifBlank { "ခု" }
                val subDetail = "${com.example.offlinepos.domain.Money.formatQty(line.effectiveQuantity())} $unitStr x %,d Ks".format(line.unitPriceMinor)
                canvas.drawText(subDetail, 14f, currentY, textPaint)
                currentY += subDetailSize + 4f + extraLineSpacing

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

        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("ကျသင့်ငွေ", 14f, currentY, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("%,d Ks".format(receipt.subtotalMinor), width - 14f, currentY, textPaint)
        currentY += totalsSize + 6f + extraLineSpacing

        if (receipt.discountMinor > 0) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("လျှော့ငွေ (Discount)", 14f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("-%,d Ks".format(receipt.discountMinor), width - 14f, currentY, textPaint)
            currentY += totalsSize + 6f + extraLineSpacing
        }

        if (receipt.deliveryFeeMinor > 0) {
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText("ပို့ဆောင်ခ (Delivery)", 14f, currentY, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("%,d Ks".format(receipt.deliveryFeeMinor), width - 14f, currentY, textPaint)
            currentY += totalsSize + 6f + extraLineSpacing
        }

        boldPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("စုစုပေါင်း", 14f, currentY, boldPaint)
        boldPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("%,d Ks".format(receipt.totalMinor), width - 14f, currentY, boldPaint)
        currentY += totalsSize + 8f + extraLineSpacing

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

        drawDivider(template.gridLineStyle)

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

        if (template.returnPolicyText.isNotBlank()) {
            template.returnPolicyText.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    canvas.drawText(line.trim(), width / 2f, currentY, textPaint)
                    currentY += footerSize + 5f + extraLineSpacing
                }
            }
        }

        val footerMsg = template.footerMessage.ifBlank { storeProfile.footerMessage }
        if (footerMsg.isNotBlank()) {
            footerMsg.split("\n").forEach { line ->
                if (line.isNotBlank()) {
                    canvas.drawText(line.trim(), width / 2f, currentY, textPaint)
                    currentY += footerSize + 5f + extraLineSpacing
                }
            }
        }

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

        if (template.showPrintTime) {
            val printTimeStr = "Print Time: ${SimpleDateFormat("dd/MM/yyyy hh:mma", Locale.ENGLISH).format(Date(receipt.timestampEpochMs))}"
            textPaint.textSize = footerSize - 1.5f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(printTimeStr, width / 2f, currentY, textPaint)
        }

        return bitmap
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
            cutMode = cutMode,
            kickDrawer = kickDrawer,
        )
    }
}
