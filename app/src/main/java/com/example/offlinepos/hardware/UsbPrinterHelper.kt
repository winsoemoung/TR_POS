package com.example.offlinepos.hardware

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.offlinepos.ui.ReceiptData
import com.example.offlinepos.ui.StoreProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object UsbPrinterHelper {
    const val ACTION_USB_PERMISSION = "com.example.offlinepos.USB_PRINTER_PERMISSION"

    /**
     * Determines if a USB device is likely an ESC/POS Thermal Receipt Printer.
     */
    fun isPrinterDevice(device: UsbDevice): Boolean {
        if (device.deviceClass == UsbConstants.USB_CLASS_PRINTER) return true
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_PRINTER) return true
            // Thermal printers often register with vendor class (0 or 255) and have bulk OUT endpoint
            val hasBulkOut = (0 until usbInterface.endpointCount).any { e ->
                val ep = usbInterface.getEndpoint(e)
                ep.direction == UsbConstants.USB_DIR_OUT && ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK
            }
            val isIgnoredClass = usbInterface.interfaceClass in listOf(
                UsbConstants.USB_CLASS_AUDIO,
                UsbConstants.USB_CLASS_VIDEO,
                UsbConstants.USB_CLASS_MASS_STORAGE,
                UsbConstants.USB_CLASS_HID,
                UsbConstants.USB_CLASS_COMM
            )
            if (hasBulkOut && !isIgnoredClass) return true
        }
        return false
    }

    /**
     * Finds all connected USB thermal printers.
     */
    fun getConnectedUsbPrinters(context: Context): List<UsbDevice> {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return emptyList()
        val deviceList = usbManager.deviceList ?: return emptyList()
        return deviceList.values.filter { isPrinterDevice(it) }
    }

    /**
     * Checks if permission is granted for the given USB device.
     */
    fun hasPermission(context: Context, device: UsbDevice): Boolean {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return false
        return usbManager.hasPermission(device)
    }

    /**
     * Requests permission to communicate with the USB printer device.
     */
    fun requestPermission(context: Context, device: UsbDevice, onResult: (Boolean) -> Unit) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: run {
            onResult(false)
            return
        }

        if (usbManager.hasPermission(device)) {
            onResult(true)
            return
        }

        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION).apply { setPackage(context.packageName) },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(recvContext: Context?, intent: Intent?) {
                if (intent?.action == ACTION_USB_PERMISSION) {
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}

                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    onResult(granted)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }

        usbManager.requestPermission(device, permissionIntent)
    }

    /**
     * Finds the bulk OUT endpoint and its interface on the USB printer device.
     */
    fun findPrinterEndpoint(device: UsbDevice): Pair<UsbInterface, UsbEndpoint>? {
        for (i in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(i)
            for (e in 0 until usbInterface.endpointCount) {
                val endpoint = usbInterface.getEndpoint(e)
                if (endpoint.direction == UsbConstants.USB_DIR_OUT &&
                    endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK
                ) {
                    return Pair(usbInterface, endpoint)
                }
            }
        }
        return null
    }

    /**
     * Returns a user-friendly name for the USB device.
     */
    fun getDisplayName(device: UsbDevice): String {
        val prodName = runCatching { device.productName }.getOrNull()
        val mfrName = runCatching { device.manufacturerName }.getOrNull()
        return when {
            !prodName.isNullOrBlank() && !mfrName.isNullOrBlank() -> "$mfrName $prodName"
            !prodName.isNullOrBlank() -> prodName
            else -> "USB POS Printer (${device.vendorId.toString(16)}:${device.productId.toString(16)})"
        }
    }

    /**
     * Prints a transaction receipt to a USB thermal printer.
     */
    suspend fun printReceipt(
        context: Context,
        device: UsbDevice,
        receipt: ReceiptData,
        profile: StoreProfile,
        paperWidthDots: Int = 576,
        kickDrawer: Boolean = true,
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return@withContext Pair(false, "USB Manager မရရှိနိုင်ပါ")

        if (!usbManager.hasPermission(device)) {
            return@withContext Pair(false, "USB Printer အသုံးပြုခွင့် (Permission) မပေးရသေးပါ")
        }

        val pair = findPrinterEndpoint(device)
            ?: return@withContext Pair(false, "Printer ၏ Bulk OUT Endpoint ရှာမတွေ့ပါ")

        val usbInterface = pair.first
        val endpoint = pair.second

        var connection: UsbDeviceConnection? = null
        try {
            connection = usbManager.openDevice(device)
                ?: return@withContext Pair(false, "USB Printer နှင့် ချိတ်ဆက်၍မရပါ (Open Device Failed)")

            if (!connection.claimInterface(usbInterface, true)) {
                return@withContext Pair(false, "USB Interface ကို အသုံးပြုခွင့် ရယူ၍မရပါ (Claim Interface Failed)")
            }

            // Render receipt bitmap
            val bitmap = BluetoothPrinterHelper.renderReceiptBitmap(receipt, profile, paperWidthDots)
            val rasterData = BluetoothPrinterHelper.convertBitmapToEscPos(bitmap)

            // Prepare ESC/POS byte sequence
            val feedLines = byteArrayOf(0x1B.toByte(), 0x64.toByte(), 0x03.toByte())
            val kickData = if (kickDrawer) EscPos.CASH_DRAWER_KICK else byteArrayOf()

            val payload = EscPos.INIT + rasterData + feedLines + EscPos.CUT + kickData

            // Transfer in chunks to prevent USB buffer overflow
            val chunkSize = 4096
            var offset = 0
            while (offset < payload.size) {
                val len = minOf(chunkSize, payload.size - offset)
                val chunk = payload.copyOfRange(offset, offset + len)
                val transferred = connection.bulkTransfer(endpoint, chunk, len, 6000)
                if (transferred < 0) {
                    throw IOException("USB Printer သို့ ဒေတာပို့ဆောင်စဉ် ချို့ယွင်းချက်ဖြစ်ပေါ်ပါသည် ($transferred)")
                }
                offset += len
            }

            val printerName = getDisplayName(device)
            Pair(true, "ဘေလ်ပြေစာကို USB Printer ($printerName) သို့ အောင်မြင်စွာ ပရင့်ထုတ်ပြီးပါပြီ")
        } catch (e: Exception) {
            Pair(false, "USB ပရင့်ထုတ်မှု မအောင်မြင်ပါ: ${e.message}")
        } finally {
            try {
                connection?.releaseInterface(usbInterface)
            } catch (_: Exception) {}
            try {
                connection?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Prints raw ESC/POS bytes (used for test prints).
     */
    suspend fun printRawBytes(
        context: Context,
        device: UsbDevice,
        data: ByteArray,
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: return@withContext Pair(false, "USB Manager မရရှိနိုင်ပါ")

        if (!usbManager.hasPermission(device)) {
            return@withContext Pair(false, "USB Printer အသုံးပြုခွင့် မပေးရသေးပါ")
        }

        val pair = findPrinterEndpoint(device)
            ?: return@withContext Pair(false, "Printer ၏ Bulk OUT Endpoint ရှာမတွေ့ပါ")

        val usbInterface = pair.first
        val endpoint = pair.second

        var connection: UsbDeviceConnection? = null
        try {
            connection = usbManager.openDevice(device)
                ?: return@withContext Pair(false, "USB Printer ချိတ်ဆက်မရပါ")

            if (!connection.claimInterface(usbInterface, true)) {
                return@withContext Pair(false, "USB Interface ရယူမရပါ")
            }

            val chunkSize = 4096
            var offset = 0
            while (offset < data.size) {
                val len = minOf(chunkSize, data.size - offset)
                val chunk = data.copyOfRange(offset, offset + len)
                val transferred = connection.bulkTransfer(endpoint, chunk, len, 5000)
                if (transferred < 0) {
                    throw IOException("USB data transfer error ($transferred)")
                }
                offset += len
            }

            Pair(true, "USB Printer စမ်းသပ်ပရင့်ထုတ်မှု အောင်မြင်ပါသည်")
        } catch (e: Exception) {
            Pair(false, "USB ပရင့်ထုတ်မှု မအောင်မြင်ပါ: ${e.message}")
        } finally {
            try { connection?.releaseInterface(usbInterface) } catch (_: Exception) {}
            try { connection?.close() } catch (_: Exception) {}
        }
    }
}
