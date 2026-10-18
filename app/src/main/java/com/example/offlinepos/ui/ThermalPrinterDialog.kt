package com.example.offlinepos.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.offlinepos.hardware.BluetoothPrinterHelper
import com.example.offlinepos.hardware.PaperWidth
import com.example.offlinepos.hardware.PrinterConnectionType
import com.example.offlinepos.hardware.ThermalPrinterManager
import com.example.offlinepos.hardware.UsbPrinterHelper
import kotlinx.coroutines.launch

@Composable
fun ThermalPrinterDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Bluetooth, 1: USB, 2: Settings

    var currentConnType by remember { mutableStateOf(ThermalPrinterManager.getConnectionType(context)) }
    var currentPaperWidth by remember { mutableStateOf(ThermalPrinterManager.getPaperWidth(context)) }
    var autoPrintEnabled by remember { mutableStateOf(ThermalPrinterManager.isAutoPrintEnabled(context)) }
    var kickDrawerEnabled by remember { mutableStateOf(ThermalPrinterManager.isKickDrawerEnabled(context)) }
    var numCopies by remember { mutableIntStateOf(ThermalPrinterManager.getNumCopies(context)) }

    var savedBtAddress by remember { mutableStateOf(ThermalPrinterManager.getSavedBluetoothAddress(context)) }
    var savedUsbName by remember { mutableStateOf(ThermalPrinterManager.getSavedUsbDevice(context)?.let { UsbPrinterHelper.getDisplayName(it) }) }

    var pairedBtPrinters by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var connectedUsbPrinters by remember { mutableStateOf<List<UsbDevice>>(emptyList()) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    fun refreshLists() {
        pairedBtPrinters = BluetoothPrinterHelper.getPairedDevices(context)
        connectedUsbPrinters = UsbPrinterHelper.getConnectedUsbPrinters(context)
        currentConnType = ThermalPrinterManager.getConnectionType(context)
        savedBtAddress = ThermalPrinterManager.getSavedBluetoothAddress(context)
        savedUsbName = ThermalPrinterManager.getSavedUsbDevice(context)?.let { UsbPrinterHelper.getDisplayName(it) }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshLists()
    }

    fun checkAndRequestBtPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            bluetoothPermissionLauncher.launch(missing.toTypedArray())
        } else {
            refreshLists()
        }
    }

    LaunchedEffect(Unit) {
        checkAndRequestBtPermissions()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 700.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("thermal_printer_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Print,
                                contentDescription = "Printer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Thermal Printer စနစ်",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Bluetooth & USB OTG ပြေစာပရင့်တာ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentConnType != PrinterConnectionType.NONE) {
                            Color(0xFFECFDF5)
                        } else {
                            Color(0xFFFFFBEB)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = ThermalPrinterManager.getPrinterStatusSummary(context),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (currentConnType != PrinterConnectionType.NONE) Color(0xFF065F46) else Color(0xFF92400E)
                            )
                            Text(
                                text = "စက္ကူအရွယ်အစား: ${currentPaperWidth.label} | မိတ္တူ: $numCopies စောင်",
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        // Refresh button
                        IconButton(
                            onClick = { refreshLists() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs: Bluetooth, USB, Settings
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bluetooth", fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("USB OTG", fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Settings", fontSize = 12.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> {
                            // Bluetooth Tab
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isBtOn = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
                                    Text(
                                        text = if (isBtOn) "Bluetooth: ဖွင့်ထားပါသည် (ON)" else "⚠️ Bluetooth ပိတ်ထားပါသည်",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isBtOn) Color(0xFF059669) else MaterialTheme.colorScheme.error
                                    )
                                    OutlinedButton(
                                        onClick = { checkAndRequestBtPermissions() },
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("ရှာဖွေမည်", fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (pairedBtPrinters.isEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "⚠️ Pair လုပ်ထားသော Bluetooth Printer မတွေ့ပါ",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "ဖုန်း Settings > Bluetooth တွင် သင်၏ 58mm/80mm Thermal Printer နှင့် Pair ချိတ်ဆက်ပေးပါ။ ချိတ်ဆက်ပြီးပါက 'ရှာဖွေမည်' ကို နှိပ်ပါ။",
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Center,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(pairedBtPrinters) { device ->
                                            val address = runCatching { device.address }.getOrDefault("")
                                            val name = runCatching { device.name ?: "Unknown Printer" }.getOrDefault("Unknown Printer")
                                            val isSelected = currentConnType == PrinterConnectionType.BLUETOOTH && savedBtAddress == address

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                ),
                                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)) else null,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                            if (isSelected) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(Color(0xFF10B981))
                                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text("လက်ရှိသုံးထားသည်", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                        Text(address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }

                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        if (!isSelected) {
                                                            Button(
                                                                onClick = {
                                                                    ThermalPrinterManager.saveBluetoothPrinter(context, address, name)
                                                                    refreshLists()
                                                                    statusMessage = "$name ကို ပရင့်တာအဖြစ် ရွေးချယ်ပြီးပါပြီ"
                                                                },
                                                                modifier = Modifier.height(34.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                            ) {
                                                                Text("ရွေးမည်", fontSize = 11.sp)
                                                            }
                                                        }

                                                        OutlinedButton(
                                                            onClick = {
                                                                isTesting = true
                                                                statusMessage = "Bluetooth စမ်းသပ်ပရင့် ထုတ်နေပါသည်..."
                                                                coroutineScope.launch {
                                                                    val res = BluetoothPrinterHelper.printTestReceipt(
                                                                        device = device,
                                                                        profile = viewModel.storeProfile.value,
                                                                        paperWidthDots = currentPaperWidth.dots
                                                                    )
                                                                    isTesting = false
                                                                    statusMessage = res.second
                                                                }
                                                            },
                                                            modifier = Modifier.height(34.dp),
                                                            enabled = !isTesting
                                                        ) {
                                                            Text("စမ်းသပ်မည်", fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // USB OTG Tab
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "USB ချိတ်ဆက်မှု: ${connectedUsbPrinters.size} လုံး တွေ့ရှိ",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    OutlinedButton(
                                        onClick = { refreshLists() },
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("ရှာဖွေမည်", fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (connectedUsbPrinters.isEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "🔌 USB Thermal Printer မတွေ့ရှိပါ",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                "၁။ USB OTG Adapter ဖြင့် Thermal Printer ကြိုးကို ဖုန်းတွင် ထိုးပါ။\n၂။ Printer ၏ Power ခလုတ် ဖွင့်ထားပါ။\n၃။ အထက်ပါ 'ရှာဖွေမည်' ခလုတ်ကို နှိပ်ပါ။",
                                                style = MaterialTheme.typography.bodySmall,
                                                textAlign = TextAlign.Start,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(connectedUsbPrinters) { device ->
                                            val displayName = UsbPrinterHelper.getDisplayName(device)
                                            val hasPerm = UsbPrinterHelper.hasPermission(context, device)
                                            val isSelected = currentConnType == PrinterConnectionType.USB &&
                                                savedUsbName == displayName

                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                ),
                                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)) else null,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                                if (isSelected) {
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(4.dp))
                                                                            .background(Color(0xFF10B981))
                                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Text("လက်ရှိသုံးထားသည်", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                                    }
                                                                }
                                                            }
                                                            Text(
                                                                "Vendor: 0x${device.vendorId.toString(16)} | Product: 0x${device.productId.toString(16)}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }

                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (hasPerm) Color(0xFFDCFCE7) else Color(0xFFFEF3C7))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = if (hasPerm) "ခွင့်ပြုပြီး" else "Permission လိုသည်",
                                                                fontSize = 10.sp,
                                                                color = if (hasPerm) Color(0xFF166534) else Color(0xFF92400E),
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (!hasPerm) {
                                                            Button(
                                                                onClick = {
                                                                    UsbPrinterHelper.requestPermission(context, device) { granted ->
                                                                        refreshLists()
                                                                        statusMessage = if (granted) "USB Printer အသုံးပြုခွင့် ရရှိပါသည်" else "Permission ငြင်းပယ်ခံရပါသည်"
                                                                    }
                                                                },
                                                                modifier = Modifier.height(34.dp)
                                                            ) {
                                                                Text("Grant Permission", fontSize = 11.sp)
                                                            }
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                        }

                                                        if (!isSelected) {
                                                            Button(
                                                                onClick = {
                                                                    ThermalPrinterManager.saveUsbPrinter(context, device)
                                                                    refreshLists()
                                                                    statusMessage = "$displayName ကို ပရင့်တာအဖြစ် ရွေးချယ်ပြီးပါပြီ"
                                                                },
                                                                modifier = Modifier.height(34.dp),
                                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                            ) {
                                                                Text("ရွေးမည်", fontSize = 11.sp)
                                                            }
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                        }

                                                        OutlinedButton(
                                                            onClick = {
                                                                if (!hasPerm) {
                                                                    UsbPrinterHelper.requestPermission(context, device) { granted ->
                                                                        refreshLists()
                                                                    }
                                                                } else {
                                                                    isTesting = true
                                                                    statusMessage = "USB စမ်းသပ်ပရင့် ထုတ်နေပါသည်..."
                                                                    coroutineScope.launch {
                                                                        val bitmap = BluetoothPrinterHelper.renderTestReceiptBitmap(
                                                                            profile = viewModel.storeProfile.value,
                                                                            paperWidthDots = currentPaperWidth.dots
                                                                        )
                                                                        val raster = BluetoothPrinterHelper.convertBitmapToEscPos(bitmap)
                                                                        val feedCut = byteArrayOf(0x1B.toByte(), 0x64.toByte(), 0x03.toByte()) + com.example.offlinepos.hardware.EscPos.CUT
                                                                        val res = UsbPrinterHelper.printRawBytes(context, device, com.example.offlinepos.hardware.EscPos.INIT + raster + feedCut)
                                                                        isTesting = false
                                                                        statusMessage = res.second
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.height(34.dp),
                                                            enabled = !isTesting
                                                        ) {
                                                            Text("စမ်းသပ်မည်", fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Settings Tab
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Paper width
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("စက္ကူ အရွယ်အစား (Paper Width)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = currentPaperWidth == PaperWidth.WIDTH_80MM,
                                                onClick = {
                                                    currentPaperWidth = PaperWidth.WIDTH_80MM
                                                    ThermalPrinterManager.setPaperWidth(context, PaperWidth.WIDTH_80MM)
                                                },
                                                label = { Text("80mm (Standard)", fontSize = 11.sp) }
                                            )
                                            FilterChip(
                                                selected = currentPaperWidth == PaperWidth.WIDTH_58MM,
                                                onClick = {
                                                    currentPaperWidth = PaperWidth.WIDTH_58MM
                                                    ThermalPrinterManager.setPaperWidth(context, PaperWidth.WIDTH_58MM)
                                                },
                                                label = { Text("58mm (Compact)", fontSize = 11.sp) }
                                            )
                                        }
                                    }
                                }

                                // Auto print switch
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("အလိုအလျောက် ပရင့်ထုတ်ခြင်း (Auto Print)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("အရောင်းဘေလ် ရှင်းပြီးသည်နှင့် ပရင့်တာသို့ တိုက်ရိုက်ဒေတာပို့မည်", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = autoPrintEnabled,
                                            onCheckedChange = {
                                                autoPrintEnabled = it
                                                ThermalPrinterManager.setAutoPrintEnabled(context, it)
                                            }
                                        )
                                    }
                                }

                                // Kick drawer switch
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("ငွေသေတ္တာ အလိုအလျောက်ဖွင့်မည် (Cash Drawer)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("ပရင့်ထုတ်ပြီးချိန်တွင် RJ11 ငွေသေတ္တာအား အလိုအလျောက် ကန်ဖွင့်ပေးမည်", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = kickDrawerEnabled,
                                            onCheckedChange = {
                                                kickDrawerEnabled = it
                                                ThermalPrinterManager.setKickDrawerEnabled(context, it)
                                            }
                                        )
                                    }
                                }

                                // Print copies
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("ပရင့်မိတ္တူ အရေအတွက် (Copies)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("တစ်ကြိမ်လျှင် ထုတ်မည့် စောင်ရေ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    if (numCopies > 1) {
                                                        numCopies--
                                                        ThermalPrinterManager.setNumCopies(context, numCopies)
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Text("-", fontWeight = FontWeight.Bold)
                                            }
                                            Text("$numCopies စောင်", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            OutlinedButton(
                                                onClick = {
                                                    if (numCopies < 3) {
                                                        numCopies++
                                                        ThermalPrinterManager.setNumCopies(context, numCopies)
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Text("+", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Status message
                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.contains("မအောင်မြင်") || msg.contains("မတွေ့") || msg.contains("လိုအပ်") || msg.contains("ငြင်းပယ်")) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color(0xFF059669)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Bottom actions: Unified Test Print button & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isTesting = true
                            statusMessage = "လက်ရှိ ပရင့်တာသို့ စမ်းသပ်စာရွက် ပို့နေပါသည်..."
                            viewModel.printTestReceipt { success, msg ->
                                isTesting = false
                                statusMessage = msg
                            }
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .testTag("test_print_unified_button"),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ထုတ်နေသည်...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("စမ်းသပ်ပရင့်ထုတ်မည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.7f)
                            .height(44.dp)
                    ) {
                        Text("ပိတ်မည်", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
