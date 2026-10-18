package com.example.offlinepos.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.offlinepos.R
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import com.example.offlinepos.hardware.BluetoothPrinterHelper
import com.example.offlinepos.hardware.ThermalPrinterManager
import com.example.offlinepos.hardware.PrinterConnectionType
import com.example.offlinepos.hardware.UsbPrinterHelper
import androidx.compose.ui.window.Dialog

@Composable
fun ReceiptDialog(
    receipt: ReceiptData,
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val storeProfile by viewModel.storeProfile.collectAsState()

    var showPrinterDialog by remember { mutableStateOf(false) }
    var printStatus by remember { mutableStateOf<String?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .clip(RoundedCornerShape(16.dp))
                .testTag("receipt_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "အရောင်းပြေစာ (Receipt)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Paper receipt card styling with Myanmar text & custom Store Profile
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), // Warm paper yellow
                    border = CardDefaults.outlinedCardBorder(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        StoreLogoAvatar(
                            customLogoPath = storeProfile.customLogoPath,
                            contentDescription = "Store Owner Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = storeProfile.storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                        if (storeProfile.headerNote.isNotBlank()) {
                            Text(
                                text = storeProfile.headerNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (storeProfile.address.isNotBlank()) {
                            Text(
                                text = storeProfile.address,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (storeProfile.phone.isNotBlank()) {
                            Text(
                                text = "ဖုန်း: ${storeProfile.phone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "ဘေလ်အမှတ်: #${receipt.saleId.take(8).uppercase()} | နေ့စွဲ: ${receipt.businessDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = "အရောင်းတာဝန်ခံ: ${receipt.cashierId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                        )
                        if (!receipt.customerName.isNullOrBlank()) {
                            Text(
                                text = "ဝယ်သူအမည်: ${receipt.customerName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A),
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Items
                        receipt.items.forEach { line ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = line.productNameSnapshot,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0F172A),
                                    )
                                    Text(
                                        text = "${line.quantity} ခု/ချပ် × %,d Ks".format(line.unitPriceMinor),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B),
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                                Text(
                                    text = "%,d Ks".format(line.lineTotalMinor),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFCBD5E1))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Breakdown
                        ReceiptRow("ကျသင့်ငွေ (Subtotal)", "%,d Ks".format(receipt.totals.subtotalMinor))
                        if (receipt.totals.discountMinor > 0) {
                            ReceiptRow("လျှော့စျေး (Discount)", "-%,d Ks".format(receipt.totals.discountMinor), isNegative = true)
                        }
                        if (receipt.deliveryFeeMinor > 0) {
                            ReceiptRow("သယ်ယူ ပို့ဆောင်ခ", "%,d Ks".format(receipt.deliveryFeeMinor))
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = Color(0xFF94A3B8), thickness = 1.5.dp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "စုစုပေါင်း (TOTAL)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                            )
                            Text(
                                text = "%,d Ks".format(receipt.totals.totalMinor),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                fontFamily = FontFamily.Monospace,
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        val methodText = when (receipt.paymentMethod) {
                            "CASH" -> "ငွေသား (Cash)"
                            "KPAY" -> "📱 KPay"
                            "WAVEPAY" -> "🟡 WavePay"
                            "CREDIT" -> "📝 အကြွေး (Credit)"
                            "SPLIT" -> "⚡ Split (ငွေသား + Digital)"
                            else -> receipt.paymentMethod
                        }
                        ReceiptRow("ပေးချေမှုပုံစံ", methodText)
                        if (receipt.paymentMethod == "SPLIT") {
                            ReceiptRow("• ငွေသား (Cash):", "%,d Ks".format(receipt.splitCashMinor))
                            ReceiptRow("• ဒစ်ဂျစ်တယ် (Digital):", "%,d Ks".format(receipt.splitDigitalMinor))
                        } else if (receipt.paymentMethod == "CASH") {
                            ReceiptRow("ပေးငွေ (Tendered)", "%,d Ks".format(receipt.tenderedMinor))
                            ReceiptRow("ပြန်အမ်းငွေ (Change)", "%,d Ks".format(receipt.changeMinor), isHighlight = true)
                        } else if (receipt.paymentMethod == "CREDIT") {
                            ReceiptRow("အကြွေးမှတ်တမ်းတင်ငွေ", "%,d Ks".format(receipt.totals.totalMinor), isHighlight = true)
                        }

                        // QR code removed as requested

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = storeProfile.footerMessage.ifBlank { "ဝယ်ယူအားပေးမှုကို အထူးပင် ကျေးဇူးတင်ရှိပါသည်" },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF475569),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Printer summary banner & quick configuration
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { showPrinterDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ThermalPrinterManager.getPrinterStatusSummary(context),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "⚙️ ပြောင်းမည်",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions: PDF Share, Thermal Print, Settings & Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.sharePdfReceipt(receipt) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_pdf_button"),
                    ) {
                        Text("📄 PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (ThermalPrinterManager.getConnectionType(context) == PrinterConnectionType.NONE &&
                                BluetoothPrinterHelper.getPairedDevices(context).isEmpty() &&
                                UsbPrinterHelper.getConnectedUsbPrinters(context).isEmpty()
                            ) {
                                showPrinterDialog = true
                            } else {
                                isPrinting = true
                                printStatus = "Printer သို့ ဒေတာ ပို့နေပါသည်..."
                                viewModel.printReceiptUnified(receipt) { success, msg ->
                                    isPrinting = false
                                    printStatus = msg
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("thermal_print_button"),
                        enabled = !isPrinting
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ထုတ်နေသည်...", fontSize = 11.sp)
                        } else {
                            Text("🖨️ ပရင့်ထုတ်မည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = { showPrinterDialog = true },
                        modifier = Modifier.testTag("printer_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Printer Settings", modifier = Modifier.size(20.dp))
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.7f)
                            .testTag("dismiss_receipt_button"),
                    ) {
                        Text("ပိတ်မည်", fontSize = 11.sp)
                    }
                }

                printStatus?.let { status ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (status.contains("မအောင်မြင်") || status.contains("မတွေ့") || status.contains("လိုအပ်")) Color(0xFFDC2626) else Color(0xFF059669),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    // Thermal Printer Chooser & Config Dialog
    if (showPrinterDialog) {
        ThermalPrinterDialog(
            viewModel = viewModel,
            onDismiss = { showPrinterDialog = false }
        )
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    isNegative: Boolean = false,
    isHighlight: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isHighlight) Color(0xFF0F172A) else Color(0xFF475569),
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = when {
                isNegative -> Color(0xFFDC2626)
                isHighlight -> Color(0xFF0D9488)
                else -> Color(0xFF0F172A)
            },
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
