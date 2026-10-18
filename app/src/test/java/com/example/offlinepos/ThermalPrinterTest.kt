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
}
