package com.example.offlinepos.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.offlinepos.hardware.PaperWidth
import com.example.offlinepos.hardware.PrinterConnectionType
import com.example.offlinepos.hardware.ThermalPrinterManager
import kotlinx.coroutines.launch

@Composable
fun SettingsHardwareSubpage(
    viewModel: PosViewModel,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showPrinterModal by remember { mutableStateOf(false) }
    var autoPrintReceipt by remember { mutableStateOf(ThermalPrinterManager.isAutoPrintEnabled(context)) }
    var selectedPaperWidth by remember { mutableStateOf(ThermalPrinterManager.getPaperWidth(context)) }
    var isTestingPrint by remember { mutableStateOf(false) }
    var testPrintStatus by remember { mutableStateOf<String?>(null) }

    var bluetoothDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var hasBluetoothPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    fun refreshBluetoothPrinters() {
        if (!hasBluetoothPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) return
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter?.isEnabled == true) {
                val bonded = adapter.bondedDevices?.toList().orEmpty()
                bluetoothDevices = bonded.filter { dev ->
                    val name = try { dev.name?.lowercase().orEmpty() } catch (_: SecurityException) { "" }
                    name.contains("print") || name.contains("pos") || name.contains("rp") ||
                            name.contains("thermal") || name.contains("58") || name.contains("80") ||
                            name.contains("mpt") || name.contains("bt") || name.contains("inner")
                }.ifEmpty { bonded }
            }
        } catch (_: SecurityException) {
            hasBluetoothPermission = false
        }
    }

    LaunchedEffect(Unit) {
        refreshBluetoothPrinters()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBluetoothPermission = isGranted
        if (isGranted) refreshBluetoothPrinters()
    }

    val storeProfile by viewModel.storeProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Status Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🖨️ Thermal Printer Manager", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Bluetooth & USB ESC/POS ပြေစာပရင့်တာများ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showPrinterModal = true },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Setup Dialog", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = ThermalPrinterManager.getPrinterStatusSummary(context),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (ThermalPrinterManager.getConnectionType(context) == PrinterConnectionType.NONE) Color.Gray else Color(0xFF059669)
                            )
                            val details = when (ThermalPrinterManager.getConnectionType(context)) {
                                PrinterConnectionType.BLUETOOTH -> ThermalPrinterManager.getSavedBluetoothAddress(context) ?: ""
                                PrinterConnectionType.USB -> "USB OTG Connection"
                                PrinterConnectionType.NONE -> ""
                            }
                            if (details.isNotBlank()) {
                                Text(details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (ThermalPrinterManager.getConnectionType(context) == PrinterConnectionType.NONE) Color(0xFFF3F4F6) else Color(0xFFDCFCE7),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = if (selectedPaperWidth == PaperWidth.WIDTH_80MM) "80mm" else "58mm",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ThermalPrinterManager.getConnectionType(context) == PrinterConnectionType.NONE) Color.Gray else Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Auto Print & Paper Configuration
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⚙️ ပရင့်တာ သတ်မှတ်ချက်များ (Preferences)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("အရောင်းဘေလ်ဖွင့်ချိန် အလိုအလျောက် ပရင့်ထုတ်မည်", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("ငွေရှင်းပြီးသည်နှင့် ပရင့်တာသို့ ဘေလ်တိုက်ရိုက်ပို့ပေးပါသည်", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = autoPrintReceipt,
                        onCheckedChange = {
                            autoPrintReceipt = it
                            ThermalPrinterManager.setAutoPrintEnabled(context, it)
                        },
                        modifier = Modifier.testTag("auto_print_receipt_switch")
                    )
                }

                HorizontalDivider()

                Column {
                    Text("စက္ကူအရွယ်အစား (Paper Width):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(PaperWidth.WIDTH_80MM to "80mm (မျက်နှာပြည့် စံသတ်မှတ်ချက်)", PaperWidth.WIDTH_58MM to "58mm (အသေးစား ပရင့်တာ)")) { (width, label) ->
                            FilterChip(
                                selected = selectedPaperWidth == width,
                                onClick = {
                                    selectedPaperWidth = width
                                    ThermalPrinterManager.setPaperWidth(context, width)
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Test Print & Quick Actions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🧪 ပရင့်တာ အစမ်းစမ်းသပ်ခြင်း (Test Print)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "ချိတ်ဆက်ထားသော ပရင့်တာနှင့် မြန်မာယူနီကုဒ် ဖောင့်စနစ် ကောင်းမွန်စွာ အလုပ်လုပ်ခြင်း ရှိ/မရှိ စမ်းသပ်ရန် စာရွက်အစမ်းထုတ်ကြည့်ပါ:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isTestingPrint = true
                            testPrintStatus = null
                            coroutineScope.launch {
                                val result = ThermalPrinterManager.printTestReceipt(context, storeProfile)
                                isTestingPrint = false
                                testPrintStatus = if (result.first) {
                                    "✓ ပရင့်ထုတ်ခြင်း အောင်မြင်ပါသည်!"
                                } else {
                                    "✗ ပရင့်ထုတ်မရပါ: ${result.second}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("test_print_sample_button"),
                        enabled = !isTestingPrint,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        if (isTestingPrint) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ထုတ်နေသည်...", fontSize = 12.sp)
                        } else {
                            Text("🖨️ Sample Slip အစမ်းထုတ်မည်", fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            ThermalPrinterManager.setConnectionType(context, PrinterConnectionType.NONE)
                            Toast.makeText(context, "ပရင့်တာ ချိတ်ဆက်မှု ဖြုတ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("ဖြုတ်မည်", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }

                testPrintStatus?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (msg.contains("မအောင်မြင်") || msg.contains("မရပါ")) MaterialTheme.colorScheme.error else Color(0xFF059669),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        // Bluetooth Paired Devices Quick Connect
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📡 ချိတ်ဆက်ဖူးသော Bluetooth ပရင့်တာများ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = {
                            if (!hasBluetoothPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                refreshBluetoothPrinters()
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🔄 Scan / Refresh", fontSize = 11.sp)
                    }
                }

                if (!hasBluetoothPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bluetooth ခွင့်ပြုချက် ပေးမည်", fontSize = 12.sp)
                    }
                } else if (bluetoothDevices.isEmpty()) {
                    Text(
                        "ဖုန်း၏ Bluetooth Setting တွင် Thermal Printer နှင့် Pair (တွဲချိတ်) ပြုလုပ်ထားရန် လိုအပ်ပါသည်",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    bluetoothDevices.forEach { dev ->
                        val devName = try { dev.name ?: "Unknown Printer" } catch (_: SecurityException) { "Printer" }
                        val devMac = dev.address
                        val isCurrent = ThermalPrinterManager.getSavedBluetoothAddress(context) == devMac

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(devName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(devMac, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = {
                                        ThermalPrinterManager.saveBluetoothPrinter(context, devMac, devName)
                                        ThermalPrinterManager.setPaperWidth(context, selectedPaperWidth)
                                        Toast.makeText(context, "$devName ကို ပရင့်တာအဖြစ် သတ်မှတ်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(if (isCurrent) "✓ သတ်မှတ်ပြီး" else "ချိတ်မည်", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }

    if (showPrinterModal) {
        ThermalPrinterDialog(
            viewModel = viewModel,
            onDismiss = {
                showPrinterModal = false
                refreshBluetoothPrinters()
            }
        )
    }
}
