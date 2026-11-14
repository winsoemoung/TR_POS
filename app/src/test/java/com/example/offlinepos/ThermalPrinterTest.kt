package com.example.offlinepos

import com.example.offlinepos.hardware.EscPos
import com.example.offlinepos.hardware.PaperWidth
import com.example.offlinepos.hardware.PrinterConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalPrinterTest {

    @Test
    fun escPosCommands_haveCorrectByteSignatures() {
        // ESC @ (Init)
        assertEquals(2, EscPos.INIT.size)
        assertEquals(0x1B.toByte(), EscPos.INIT[0])
        assertEquals(0x40.toByte(), EscPos.INIT[1])

        // GS V B 0 (Cut)
        assertEquals(4, EscPos.CUT.size)
        assertEquals(0x1D.toByte(), EscPos.CUT[0])
        assertEquals(0x56.toByte(), EscPos.CUT[1])
        assertEquals(0x42.toByte(), EscPos.CUT[2])
        assertEquals(0x00.toByte(), EscPos.CUT[3])

        // ESC p 0 25 250 (Cash Drawer Kick)
        assertEquals(5, EscPos.CASH_DRAWER_KICK.size)
        assertEquals(0x1B.toByte(), EscPos.CASH_DRAWER_KICK[0])
        assertEquals(0x70.toByte(), EscPos.CASH_DRAWER_KICK[1])
    }

    @Test
    fun escPosRaster_generatesCorrectHeader() {
        val widthBytes = 48 // 384 dots (58mm)
        val heightDots = 10
        val rawData = ByteArray(widthBytes * heightDots)

        val raster = EscPos.raster(widthBytes, heightDots, rawData)
        // 8 bytes header + rawData
        assertEquals(8 + rawData.size, raster.size)

        // GS v 0 0
        assertEquals(0x1D.toByte(), raster[0])
        assertEquals(0x76.toByte(), raster[1])
        assertEquals(0x30.toByte(), raster[2])
        assertEquals(0x00.toByte(), raster[3])

        // xL, xH
        assertEquals(48.toByte(), raster[4])
        assertEquals(0.toByte(), raster[5])

        // yL, yH
        assertEquals(10.toByte(), raster[6])
        assertEquals(0.toByte(), raster[7])
    }

    @Test
    fun paperWidths_haveCorrectDotCounts() {
        assertEquals(576, PaperWidth.WIDTH_80MM.dots)
        assertEquals(384, PaperWidth.WIDTH_58MM.dots)

        assertEquals(PaperWidth.WIDTH_58MM, PaperWidth.fromString("58MM"))
        assertEquals(PaperWidth.WIDTH_58MM, PaperWidth.fromString("width_58mm"))
        assertEquals(PaperWidth.WIDTH_80MM, PaperWidth.fromString("80MM"))
        assertEquals(PaperWidth.WIDTH_80MM, PaperWidth.fromString(null))
    }

    @Test
    fun printerConnectionTypes_areAvailable() {
        val types = PrinterConnectionType.values()
        assertTrue(types.contains(PrinterConnectionType.BLUETOOTH))
        assertTrue(types.contains(PrinterConnectionType.USB))
        assertTrue(types.contains(PrinterConnectionType.NONE))
    }

    @Test
    fun defaultReceiptTemplate_has80mmAndFontLineSpacing() {
        val tmplDefault = com.example.offlinepos.data.local.ReceiptTemplateEntity(
            templateId = 1,
            name = "Template 1"
        )
        // Verified: All templates default to 80mm (576 dots)
        assertEquals(576, tmplDefault.paperWidthDots)
        assertEquals(100, tmplDefault.fontScalePercent)
        assertEquals(4, tmplDefault.lineSpacingDp)
        assertEquals("LARGE", tmplDefault.storeNameFontSize)
        assertEquals("MEDIUM", tmplDefault.headerDetailsFontSize)
        assertEquals("MEDIUM", tmplDefault.metaFontSize)
        assertEquals("MEDIUM", tmplDefault.bodyFontSize)
        assertEquals("MEDIUM", tmplDefault.totalsFontSize)
        assertEquals("SMALL", tmplDefault.footerFontSize)
    }

    @Test
    fun template1_sampleLayout_hasCorrectSpecsAnd80mmDefault() {
        val tmpl1 = com.example.offlinepos.data.local.ReceiptTemplateEntity(
            templateId = 1,
            name = "Template 1 - နမူနာပြေစာ စံပုံစံ (80mm Sample Style)",
            isDefault = true,
            paperWidthDots = 576,
            itemLayoutStyle = "STANDARD_2LINE",
            showLogo = false,
            customStoreName = "ဆန်းသစ်",
            customSubtitle = "အိမ်ဆောက်ပစ္စည်း အရောင်းဆိုင်",
            customOwnerOrContact = "ကိုပါပီး+မသီရိကလျာ",
            customPhone = "09678473426/09943070451/09442387883/\n09400511789",
            customAddress = "3/3 ဗိုလ်ချုပ်လမ်း / စမ်းမြို့ရပ်ကွက် / ကလေးမြို့",
            customerLabel = "ဈေးဝယ်သူ",
            showCustomerInfo = true,
            gridLineStyle = "DASHED",
            fontScalePercent = 100,
            lineSpacingDp = 4,
            returnPolicyText = "ဝယ်ယူပြီး ပစ္စည်း များကို ပြန်လဲမပေးပါ",
            footerMessage = "ဝယ်ယူအားပေးမှုကိုကျေးဇူးအထူးတင်ရှိပါသည်",
            extraNoticeText = "သုံးမူး၊ ငါးမူး၊ ခြောက်မူး သံပင် များ ၊\nသွပ် အစားထိုး polystar များ ရပါသည် ။",
            showPrintTime = true
        )

        assertEquals(576, tmpl1.paperWidthDots) // Default 80mm
        assertEquals(100, tmpl1.fontScalePercent)
        assertEquals(4, tmpl1.lineSpacingDp)
        assertEquals("STANDARD_2LINE", tmpl1.itemLayoutStyle)
        assertEquals("DASHED", tmpl1.gridLineStyle)
        assertEquals("ဆန်းသစ်", tmpl1.customStoreName)
        assertEquals("ဈေးဝယ်သူ", tmpl1.customerLabel)
        assertTrue(tmpl1.extraNoticeText.contains("polystar"))
        assertTrue(tmpl1.showPrintTime)
    }

    @Test
    fun template2_cleanBorderlessLayout_hasNoRowColumnLinesAnd80mm() {
        val tmpl2 = com.example.offlinepos.data.local.ReceiptTemplateEntity(
            templateId = 2,
            name = "Template 2 - မျဉ်းမပါ သန့်ရှင်းသော ပုံစံ (Clean Borderless 80mm)",
            isDefault = false,
            paperWidthDots = 576,
            itemLayoutStyle = "COLUMNS_4",
            showLogo = true,
            customStoreName = "",
            customPhone = "",
            customAddress = "",
            customerLabel = "ဝယ်သူအမည်",
            showCustomerInfo = true,
            gridLineStyle = "NONE", // No row/column lines
            fontScalePercent = 100,
            lineSpacingDp = 4,
            footerMessage = "ကျေးဇူးတင်ပါသည်",
            returnPolicyText = "ဝယ်ယူပြီးပစ္စည်း ပြန်မလဲပေးပါ"
        )

        assertEquals(576, tmpl2.paperWidthDots) // Default 80mm
        assertEquals("COLUMNS_4", tmpl2.itemLayoutStyle)
        assertEquals("NONE", tmpl2.gridLineStyle) // Verified: NO row/column lines!
        assertEquals("ကျေးဇူးတင်ပါသည်", tmpl2.footerMessage)
        assertTrue(tmpl2.showCustomerInfo)
    }

    @Test
    fun escPosRasterBands_splitsTallBitmapsIntoSafe128DotBands() {
        val widthBytes = 72 // 576 dots (80mm)
        val heightDots = 300 // > 255 rows -> must split into 128 + 128 + 44 bands
        val rawData = ByteArray(widthBytes * heightDots) { index -> (index % 251).toByte() }

        val banded = EscPos.rasterBands(widthBytes, heightDots, rawData)
        // 3 bands * 8-byte header + rawData.size
        assertEquals((3 * 8) + rawData.size, banded.size)

        // Band 1: height = 128
        assertEquals(0x1D.toByte(), banded[0])
        assertEquals(0x76.toByte(), banded[1])
        assertEquals(0x30.toByte(), banded[2])
        assertEquals(0x00.toByte(), banded[3])
        assertEquals(72.toByte(), banded[4])
        assertEquals(0.toByte(), banded[5])
        assertEquals(128.toByte(), banded[6])
        assertEquals(0.toByte(), banded[7])

        // Band 2 starts at 8 + (128 * 72) = 9224: height = 128
        val band2Start = 8 + (128 * widthBytes)
        assertEquals(0x1D.toByte(), banded[band2Start])
        assertEquals(0x76.toByte(), banded[band2Start + 1])
        assertEquals(128.toByte(), banded[band2Start + 6])
        assertEquals(0.toByte(), banded[band2Start + 7])

        // Band 3 starts at 2 * (8 + 128 * 72) = 18448: height = 44
        val band3Start = 2 * (8 + (128 * widthBytes))
        assertEquals(0x1D.toByte(), banded[band3Start])
        assertEquals(0x76.toByte(), banded[band3Start + 1])
        assertEquals(44.toByte(), banded[band3Start + 6])
        assertEquals(0.toByte(), banded[band3Start + 7])
    }

    @Test
    fun escPosBuildRasterReceiptPayload_includesInitCancelChineseBandsFeedAndCut() {
        val widthBytes = 72
        val heightDots = 200
        val rawData = ByteArray(widthBytes * heightDots) { 0x55.toByte() }

        val payload = EscPos.buildRasterReceiptPayload(
            bitmapWidthBytes = widthBytes,
            bitmapHeightDots = heightDots,
            data = rawData,
            feedLines = 5,
            cutMode = EscPos.PrinterCutMode.FEED_AND_CUT,
            kickDrawer = true,
        )

        // Starts with ESC @ (1B 40) + FS . (1C 2E) + ESC a 1 (1B 61 01) + ESC 3 0 (1B 33 00)
        assertEquals(0x1B.toByte(), payload[0])
        assertEquals(0x40.toByte(), payload[1])
        assertEquals(0x1C.toByte(), payload[2])
        assertEquals(0x2E.toByte(), payload[3])
        assertEquals(0x1B.toByte(), payload[4])
        assertEquals(0x61.toByte(), payload[5])
        assertEquals(0x01.toByte(), payload[6])
        assertEquals(0x1B.toByte(), payload[7])
        assertEquals(0x33.toByte(), payload[8])
        assertEquals(0x00.toByte(), payload[9])

        // Tail ends with ESC 2 (1B 32) + ESC d 5 (1B 64 05) + GS V B 0 (1D 56 42 00) + CASH_DRAWER_KICK (5 bytes)
        val n = payload.size
        assertEquals(0x1B.toByte(), payload[n - 14])
        assertEquals(0x32.toByte(), payload[n - 13])
        assertEquals(0x1B.toByte(), payload[n - 12])
        assertEquals(0x64.toByte(), payload[n - 11])
        assertEquals(0x05.toByte(), payload[n - 10])
        assertEquals(0x1D.toByte(), payload[n - 9])
        assertEquals(0x56.toByte(), payload[n - 8])
        assertEquals(0x42.toByte(), payload[n - 7])
        assertEquals(0x00.toByte(), payload[n - 6])
        assertEquals(0x1B.toByte(), payload[n - 5])
        assertEquals(0x70.toByte(), payload[n - 4])
    }

    @Test
    fun printerCutModes_mapCorrectCommands() {
        assertEquals(EscPos.PrinterCutMode.FEED_AND_CUT, EscPos.PrinterCutMode.fromString(null))
        assertEquals(EscPos.PrinterCutMode.PARTIAL_CUT, EscPos.PrinterCutMode.fromString("PARTIAL_CUT"))
        assertEquals(EscPos.PrinterCutMode.FULL_CUT, EscPos.PrinterCutMode.fromString("FULL_CUT"))
        assertEquals(EscPos.PrinterCutMode.NONE, EscPos.PrinterCutMode.fromString("NONE"))
        assertTrue(EscPos.PrinterCutMode.NONE.commandBytes.isEmpty())
    }
}

