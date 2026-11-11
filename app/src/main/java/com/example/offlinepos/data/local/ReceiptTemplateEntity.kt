package com.example.offlinepos.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipt_templates")
data class ReceiptTemplateEntity(
    @PrimaryKey val templateId: Int, // 1 to 5
    val name: String,
    val isDefault: Boolean = false,
    val paperWidthDots: Int = 576, // 384 dots (58mm), 576 dots (80mm) - Default 80mm
    val itemLayoutStyle: String = "STANDARD_2LINE", // "STANDARD_2LINE" (sample 2-line receipt) or "COLUMNS_4" (4 columns)

    // General Font & Spacing Control
    val fontScalePercent: Int = 100, // 70% to 160% overall font scale
    val lineSpacingDp: Int = 4, // 0 to 16 dp line spacing (Compact to Spacious)

    // Header Section
    val showLogo: Boolean = true,
    val customStoreName: String = "",
    val customSubtitle: String = "",
    val customOwnerOrContact: String = "",
    val customPhone: String = "",
    val customAddress: String = "",
    val storeNameFontSize: String = "LARGE", // SMALL, MEDIUM, LARGE, XLARGE, XXLARGE
    val storeNameAlign: String = "CENTER", // LEFT, CENTER, RIGHT
    val headerDetailsFontSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE, XLARGE
    val showAddress: Boolean = true,
    val showPhone: Boolean = true,
    val showHeaderNote: Boolean = true,
    val customHeaderNote: String = "",
    val showCashierInfo: Boolean = true,
    val showVoucherNumber: Boolean = true,
    val showCustomerInfo: Boolean = true,
    val customerLabel: String = "ဈေးဝယ်သူ", // e.g. "ဈေးဝယ်သူ" or "ဝယ်သူအမည်"
    val metaFontSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE, XLARGE

    // Body Grid Section (Excel-like Grid Table)
    val itemNameWidthWeight: Float = 0.42f, // 42%
    val qtyWidthWeight: Float = 0.16f, // 16%
    val priceWidthWeight: Float = 0.20f, // 20%
    val amountWidthWeight: Float = 0.22f, // 22%
    val rowPaddingDp: Int = 4, // 2 = Compact, 4 = Normal, 8 = Spacious
    val gridLineStyle: String = "DASHED", // NONE, DASHED, SOLID, DOUBLE
    val bodyFontSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE, XLARGE
    val itemNameAlign: String = "LEFT",
    val qtyAlign: String = "CENTER",
    val priceAlign: String = "RIGHT",
    val amountAlign: String = "RIGHT",

    // Totals Section
    val totalsFontSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE, XLARGE
    val showTotalsBreakdown: Boolean = true,
    val showPaymentMethod: Boolean = true,
    val showBarcodeOrQr: Boolean = true,

    // Footer Section
    val footerMessage: String = "ဝယ်ယူအားပေးမှုကို အထူးကျေးဇူးတင်ပါသည်\nပစ္စည်းမှန်၊ စျေးနှုန်းမှန်၊ အချိန်မှန်",
    val returnPolicyText: String = "ပစ္စည်း ပြန်လဲပါက (၃) ရက်အတွင်း ဘောင်ချာ ယူဆောင်လာပါရန်",
    val extraNoticeText: String = "",
    val showPrintTime: Boolean = true,
    val footerAlign: String = "CENTER",
    val footerFontSize: String = "SMALL", // SMALL, MEDIUM, LARGE, XLARGE
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
)
