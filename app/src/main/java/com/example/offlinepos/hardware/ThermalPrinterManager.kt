package com.example.offlinepos.hardware

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.example.offlinepos.ui.ReceiptData
import com.example.offlinepos.ui.StoreProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class PrinterConnectionType {
    BLUETOOTH,
    USB,
    NONE
}

enum class PaperWidth(val dots: Int, val label: String) {
    WIDTH_80MM(576, "80mm (Standard)"),
    WIDTH_58MM(384, "58mm (Compact)");

    companion object {
        fun fromString(str: String?): PaperWidth = when (str?.uppercase()) {
            "58MM", "WIDTH_58MM" -> WIDTH_58MM
            else -> WIDTH_80MM
        }
    }
}

object ThermalPrinterManager {
    private const val PREFS_NAME = "thermal_printer_manager_prefs"
    private const val KEY_CONN_TYPE = "conn_type"
    private const val KEY_BT_ADDRESS = "bt_address"
    private const val KEY_BT_NAME = "bt_name"
    private const val KEY_USB_NAME = "usb_name"
    private const val KEY_USB_VENDOR_ID = "usb_vendor_id"
    private const val KEY_USB_PRODUCT_ID = "usb_product_id"
    private const val KEY_PAPER_WIDTH = "paper_width"
    private const val KEY_AUTO_PRINT = "auto_print"
    private const val KEY_KICK_DRAWER = "kick_drawer"
    private const val KEY_COPIES = "copies"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConnectionType(context: Context): PrinterConnectionType {
        val str = prefs(context).getString(KEY_CONN_TYPE, null)
        return when (str) {
            "BLUETOOTH" -> PrinterConnectionType.BLUETOOTH
            "USB" -> PrinterConnectionType.USB
            else -> {
                // Backward compatibility: if bluetooth was previously selected in BluetoothPrinterHelper
                val legacyBt = BluetoothPrinterHelper.selectedPrinterAddress(context)
                if (!legacyBt.isNullOrBlank()) PrinterConnectionType.BLUETOOTH else PrinterConnectionType.NONE
            }
        }
    }

    fun setConnectionType(context: Context, type: PrinterConnectionType) {
        prefs(context).edit().putString(KEY_CONN_TYPE, type.name).apply()
    }

    fun getPaperWidth(context: Context): PaperWidth {
        val str = prefs(context).getString(KEY_PAPER_WIDTH, "80MM")
        return PaperWidth.fromString(str)
    }

    fun setPaperWidth(context: Context, width: PaperWidth) {
        prefs(context).edit().putString(KEY_PAPER_WIDTH, if (width == PaperWidth.WIDTH_58MM) "58MM" else "80MM").apply()
    }

    fun isAutoPrintEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTO_PRINT, false)
    }

    fun setAutoPrintEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_PRINT, enabled).apply()
    }

    fun isKickDrawerEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_KICK_DRAWER, true)
    }

    fun setKickDrawerEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_KICK_DRAWER, enabled).apply()
    }

    fun getNumCopies(context: Context): Int {
        return prefs(context).getInt(KEY_COPIES, 1).coerceIn(1, 3)
    }

    fun setNumCopies(context: Context, copies: Int) {
        prefs(context).edit().putInt(KEY_COPIES, copies.coerceIn(1, 3)).apply()
    }

    fun getSavedBluetoothAddress(context: Context): String? {
        val addr = prefs(context).getString(KEY_BT_ADDRESS, null)
        return if (!addr.isNullOrBlank()) addr else BluetoothPrinterHelper.selectedPrinterAddress(context)
    }

    fun getSavedBluetoothName(context: Context): String? {
        return prefs(context).getString(KEY_BT_NAME, null)
    }

    fun saveBluetoothPrinter(context: Context, address: String, name: String) {
        prefs(context).edit()
            .putString(KEY_CONN_TYPE, PrinterConnectionType.BLUETOOTH.name)
            .putString(KEY_BT_ADDRESS, address)
            .putString(KEY_BT_NAME, name)
            .apply()
        // Also sync legacy helper
        context.getSharedPreferences("bluetooth_printer_prefs", Context.MODE_PRIVATE).edit()
            .putString("selected_printer_address", address)
            .apply()
    }

    fun saveUsbPrinter(context: Context, device: UsbDevice) {
        val displayName = UsbPrinterHelper.getDisplayName(device)
        prefs(context).edit()
            .putString(KEY_CONN_TYPE, PrinterConnectionType.USB.name)
            .putString(KEY_USB_NAME, displayName)
            .putInt(KEY_USB_VENDOR_ID, device.vendorId)
            .putInt(KEY_USB_PRODUCT_ID, device.productId)
            .apply()
    }

    fun getSavedUsbDevice(context: Context): UsbDevice? {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager ?: return null
        val targetVendor = prefs(context).getInt(KEY_USB_VENDOR_ID, -1)
        val targetProduct = prefs(context).getInt(KEY_USB_PRODUCT_ID, -1)
        val devices = usbManager.deviceList ?: return null

        if (targetVendor != -1 && targetProduct != -1) {
            val matched = devices.values.find { it.vendorId == targetVendor && it.productId == targetProduct }
            if (matched != null) return matched
        }

        // Fallback: first connected USB printer
        return devices.values.find { UsbPrinterHelper.isPrinterDevice(it) }
    }

    fun getPrinterStatusSummary(context: Context): String {
        return when (getConnectionType(context)) {
            PrinterConnectionType.BLUETOOTH -> {
                val name = getSavedBluetoothName(context) ?: "Bluetooth Printer"
                val addr = getSavedBluetoothAddress(context)
                if (addr != null) "🔵 BT: $name" else "🔵 Bluetooth (ရွေးချယ်ရန်လိုအပ်)"
            }
            PrinterConnectionType.USB -> {
                val usb = getSavedUsbDevice(context)
                if (usb != null) {
                    val name = prefs(context).getString(KEY_USB_NAME, UsbPrinterHelper.getDisplayName(usb))
                    "🔌 USB: $name"
                } else {
                    "🔌 USB (ချိတ်ဆက်ထားခြင်းမရှိ)"
                }
            }
            PrinterConnectionType.NONE -> "🖨️ Printer မရွေးရသေးပါ"
        }
    }

    /**
     * Unified print transaction receipt method. Automatically uses configured connection (Bluetooth or USB),
     * respect configured paper width and copies.
     */
    suspend fun printReceipt(
        context: Context,
        receipt: ReceiptData,
        profile: StoreProfile,
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val connType = getConnectionType(context)
        val paperWidth = getPaperWidth(context).dots
        val kickDrawer = isKickDrawerEnabled(context)
        val copies = getNumCopies(context)

        when (connType) {
            PrinterConnectionType.USB -> {
                val usbDevice = getSavedUsbDevice(context)
                    ?: return@withContext Pair(false, "ချိတ်ဆက်ထားသော USB Thermal Printer မတွေ့ပါ။ OTG ကြိုးနှင့် Printer ဖွင့်ထားခြင်း ရှိမရှိ စစ်ဆေးပါ။")

                if (!UsbPrinterHelper.hasPermission(context, usbDevice)) {
                    return@withContext Pair(false, "USB Printer အသုံးပြုခွင့် (Permission) တောင်းဆိုရန် လိုအပ်ပါသည်")
                }

                var lastResult = Pair(false, "")
                repeat(copies) {
                    lastResult = UsbPrinterHelper.printReceipt(
                        context = context,
                        device = usbDevice,
                        receipt = receipt,
                        profile = profile,
                        paperWidthDots = paperWidth,
                        kickDrawer = kickDrawer,
                    )
                    if (!lastResult.first) return@withContext lastResult
                }
                lastResult
            }

            PrinterConnectionType.BLUETOOTH -> {
                val address = getSavedBluetoothAddress(context)
                    ?: return@withContext Pair(false, "Bluetooth Printer ရွေးချယ်ထားခြင်း မရှိပါ။ Settings > Thermal Printer တွင် ရွေးပေးပါ။")

                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: return@withContext Pair(false, "ဤဖုန်းတွင် Bluetooth စနစ် မရရှိနိုင်ပါ")

                val device = try {
                    adapter.getRemoteDevice(address)
                } catch (e: Exception) {
                    null
                } ?: return@withContext Pair(false, "ရွေးချယ်ထားသော Bluetooth Printer ($address) ရှာမတွေ့ပါ")

                var lastResult = Pair(false, "")
                repeat(copies) {
                    lastResult = BluetoothPrinterHelper.printReceipt(
                        device = device,
                        receipt = receipt,
                        profile = profile,
                        paperWidthDots = paperWidth,
                        kickDrawer = kickDrawer,
                    )
                    if (!lastResult.first) return@withContext lastResult
                }
                lastResult
            }

            PrinterConnectionType.NONE -> {
                // If neither is explicitly configured, check if there's any paired Bluetooth or connected USB printer
                val usbPrinters = UsbPrinterHelper.getConnectedUsbPrinters(context)
                if (usbPrinters.isNotEmpty()) {
                    val usb = usbPrinters.first()
                    saveUsbPrinter(context, usb)
                    return@withContext printReceipt(context, receipt, profile)
                }

                val pairedBt = BluetoothPrinterHelper.getPairedDevices(context)
                if (pairedBt.isNotEmpty()) {
                    val bt = pairedBt.first()
                    saveBluetoothPrinter(context, bt.address, bt.name ?: "BT Printer")
                    return@withContext printReceipt(context, receipt, profile)
                }

                Pair(false, "Thermal Printer ချိတ်ဆက်ထားခြင်း မရှိပါ။ Bluetooth သို့မဟုတ် USB OTG ဖြင့် ချိတ်ဆက်ပေးပါ။")
            }
        }
    }

    /**
     * Unified test receipt print.
     */
    suspend fun printTestReceipt(
        context: Context,
        profile: StoreProfile,
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val connType = getConnectionType(context)
        val paperWidth = getPaperWidth(context).dots

        when (connType) {
            PrinterConnectionType.USB -> {
                val usbDevice = getSavedUsbDevice(context)
                    ?: return@withContext Pair(false, "USB Thermal Printer ချိတ်ဆက်မထားပါ")

                if (!UsbPrinterHelper.hasPermission(context, usbDevice)) {
                    return@withContext Pair(false, "USB Printer Permission ခွင့်ပြုချက် လိုအပ်ပါသည်")
                }

                val bitmap = BluetoothPrinterHelper.renderTestReceiptBitmap(profile, paperWidth)
                val raster = BluetoothPrinterHelper.convertBitmapToEscPos(bitmap)
                val feedAndCut = byteArrayOf(0x1B.toByte(), 0x64.toByte(), 0x03.toByte()) + EscPos.CUT
                val payload = EscPos.INIT + raster + feedAndCut
                UsbPrinterHelper.printRawBytes(context, usbDevice, payload)
            }

            PrinterConnectionType.BLUETOOTH -> {
                val address = getSavedBluetoothAddress(context)
                    ?: return@withContext Pair(false, "Bluetooth Printer ရွေးချယ်မထားပါ")
                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: return@withContext Pair(false, "Bluetooth မရှိပါ")
                val device = adapter.getRemoteDevice(address)
                BluetoothPrinterHelper.printTestReceipt(device, profile, paperWidth)
            }

            PrinterConnectionType.NONE -> {
                Pair(false, "စမ်းသပ်ပရင့်ထုတ်ရန် Bluetooth သို့မဟုတ် USB Printer တစ်ခုခုကို အရင်ရွေးချယ်ပါ")
            }
        }
    }
}
