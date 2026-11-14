package com.example.offlinepos.hardware

import java.io.ByteArrayOutputStream

object EscPos {
    const val DEFAULT_MAX_BAND_HEIGHT_DOTS = 128

    val INIT: ByteArray = byteArrayOf(0x1B.toByte(), 0x40.toByte()) // ESC @
    val CANCEL_CHINESE_MODE: ByteArray = byteArrayOf(0x1C.toByte(), 0x2E.toByte()) // FS .
    val ALIGN_CENTER: ByteArray = byteArrayOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte()) // ESC a 1
    val LINE_SPACING_ZERO: ByteArray = byteArrayOf(0x1B.toByte(), 0x33.toByte(), 0x00.toByte()) // ESC 3 0
    val LINE_SPACING_DEFAULT: ByteArray = byteArrayOf(0x1B.toByte(), 0x32.toByte()) // ESC 2

    val CUT: ByteArray = byteArrayOf(0x1D.toByte(), 0x56.toByte(), 0x42.toByte(), 0x00.toByte()) // GS V B 0
    val CUT_PARTIAL: ByteArray = byteArrayOf(0x1D.toByte(), 0x56.toByte(), 0x01.toByte()) // GS V 1
    val CUT_FULL: ByteArray = byteArrayOf(0x1D.toByte(), 0x56.toByte(), 0x00.toByte()) // GS V 0
    val CASH_DRAWER_KICK: ByteArray = byteArrayOf(0x1B.toByte(), 0x70.toByte(), 0x00.toByte(), 0x19.toByte(), 0xFA.toByte())

    enum class PrinterCutMode(val label: String, val commandBytes: ByteArray) {
        FEED_AND_CUT("Standard Feed & Cut (GS V B 0)", CUT),
        PARTIAL_CUT("Partial Cut (GS V 1)", CUT_PARTIAL),
        FULL_CUT("Full Cut (GS V 0)", CUT_FULL),
        NONE("No Auto Cut", byteArrayOf());

        companion object {
            fun fromString(value: String?): PrinterCutMode =
                values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FEED_AND_CUT
        }
    }

    fun feedLines(lines: Int): ByteArray =
        byteArrayOf(0x1B.toByte(), 0x64.toByte(), lines.coerceIn(0, 255).toByte())

    fun raster(bitmapWidthBytes: Int, bitmapHeightDots: Int, data: ByteArray): ByteArray {
        require(bitmapWidthBytes > 0 && bitmapHeightDots > 0) {
            "Bitmap dimensions must be positive"
        }
        require(data.size == bitmapWidthBytes * bitmapHeightDots) {
            "Bitmap size mismatch"
        }
        val header = byteArrayOf(
            0x1D.toByte(), 0x76.toByte(), 0x30.toByte(), 0x00.toByte(),
            (bitmapWidthBytes and 0xFF).toByte(),
            ((bitmapWidthBytes shr 8) and 0xFF).toByte(),
            (bitmapHeightDots and 0xFF).toByte(),
            ((bitmapHeightDots shr 8) and 0xFF).toByte(),
        )
        return header + data
    }

    /**
     * Splits a monochrome raster bitmap into horizontal bands of at most [maxBandHeightDots] rows
     * (default 128 dots). Many ESC/POS thermal printer firmwares truncate or drop out of GS v 0
     * when a single raster block exceeds 255 dot-rows or the printer's internal image buffer,
     * causing remaining pixel bytes to print as garbage GBK/ASCII text.
     */
    fun rasterBands(
        bitmapWidthBytes: Int,
        bitmapHeightDots: Int,
        data: ByteArray,
        maxBandHeightDots: Int = DEFAULT_MAX_BAND_HEIGHT_DOTS,
    ): ByteArray {
        require(bitmapWidthBytes > 0 && bitmapHeightDots > 0) {
            "Bitmap dimensions must be positive"
        }
        require(data.size == bitmapWidthBytes * bitmapHeightDots) {
            "Bitmap size mismatch"
        }
        val safeBandMax = maxBandHeightDots.coerceIn(1, 255)
        val out = ByteArrayOutputStream(data.size + ((bitmapHeightDots / safeBandMax) + 1) * 8)

        var startRow = 0
        while (startRow < bitmapHeightDots) {
            val bandHeight = minOf(safeBandMax, bitmapHeightDots - startRow)
            val byteStart = startRow * bitmapWidthBytes
            val byteCount = bandHeight * bitmapWidthBytes

            out.write(
                byteArrayOf(
                    0x1D.toByte(), 0x76.toByte(), 0x30.toByte(), 0x00.toByte(),
                    (bitmapWidthBytes and 0xFF).toByte(),
                    ((bitmapWidthBytes shr 8) and 0xFF).toByte(),
                    (bandHeight and 0xFF).toByte(),
                    ((bandHeight shr 8) and 0xFF).toByte(),
                )
            )
            out.write(data, byteStart, byteCount)
            startRow += bandHeight
        }
        return out.toByteArray()
    }

    /**
     * Builds a complete, buffer-safe ESC/POS print payload with initialization, Chinese-mode cancel,
     * banded GS v 0 raster strips, trailing paper feed, paper cut, and optional cash drawer kick.
     */
    fun buildRasterReceiptPayload(
        bitmapWidthBytes: Int,
        bitmapHeightDots: Int,
        data: ByteArray,
        feedLines: Int = 5,
        cutMode: PrinterCutMode = PrinterCutMode.FEED_AND_CUT,
        kickDrawer: Boolean = false,
        maxBandHeightDots: Int = DEFAULT_MAX_BAND_HEIGHT_DOTS,
    ): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(INIT)
        out.write(CANCEL_CHINESE_MODE)
        out.write(ALIGN_CENTER)
        out.write(LINE_SPACING_ZERO)
        out.write(rasterBands(bitmapWidthBytes, bitmapHeightDots, data, maxBandHeightDots))
        out.write(LINE_SPACING_DEFAULT)
        if (feedLines > 0) {
            out.write(feedLines(feedLines))
        }
        if (cutMode.commandBytes.isNotEmpty()) {
            out.write(cutMode.commandBytes)
        }
        if (kickDrawer) {
            out.write(CASH_DRAWER_KICK)
        }
        return out.toByteArray()
    }
}
