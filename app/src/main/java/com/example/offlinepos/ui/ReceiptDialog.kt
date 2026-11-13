package com.example.offlinepos.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.example.offlinepos.data.local.effectiveQuantity
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
import androidx.compose.material3.TextButton
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptDialog(
    receipt: ReceiptData,
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val storeProfile by viewModel.storeProfile.collectAsState()
    val templates by viewModel.receiptTemplates.collectAsState()
    val defaultTemplate by viewModel.defaultReceiptTemplate.collectAsState()

    var selectedTemplateId by remember(defaultTemplate) { mutableStateOf(defaultTemplate?.templateId ?: 1) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showTemplateBuilder by remember { mutableStateOf(false) }
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
                    val currentTemplate = templates.find { it.templateId == selectedTemplateId } ?: defaultTemplate
                    val fontMultiplier = ((currentTemplate?.fontScalePercent ?: 100).coerceIn(60, 200) / 100f)
                    val lineSpacingExtra = ((currentTemplate?.lineSpacingDp ?: 4).coerceIn(0, 24)).dp

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (currentTemplate?.showLogo == true) {
                            StoreLogoAvatar(
                                customLogoPath = storeProfile.customLogoPath,
                                contentDescription = "Store Owner Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Store Name
                        val activeStoreName = currentTemplate?.customStoreName?.ifBlank {
                            storeProfile.storeName.ifBlank { "ဆန်းသစ်" }
                        } ?: storeProfile.storeName.ifBlank { "ဆန်းသစ်" }

                        Text(
                            text = activeStoreName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )

                        // Subtitle
                        val activeSubtitle = currentTemplate?.customSubtitle.orEmpty()
                        if (activeSubtitle.isNotBlank()) {
                            Text(
                                text = activeSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Owner / Contact
                        val activeOwner = currentTemplate?.customOwnerOrContact.orEmpty()
                        if (activeOwner.isNotBlank()) {
                            Text(
                                text = activeOwner,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF334155),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Phone Numbers
                        val activePhone = if (currentTemplate?.showPhone != false) {
                            currentTemplate?.customPhone?.ifBlank { storeProfile.phone } ?: storeProfile.phone
                        } else ""
                        if (activePhone.isNotBlank()) {
                            Text(
                                text = activePhone,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Address
                        val activeAddress = if (currentTemplate?.showAddress != false) {
                            currentTemplate?.customAddress?.ifBlank { storeProfile.address } ?: storeProfile.address
                        } else ""
                        if (activeAddress.isNotBlank()) {
                            Text(
                                text = activeAddress,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Header Note
                        val activeNote = currentTemplate?.customHeaderNote?.ifBlank { storeProfile.headerNote }.orEmpty()
                        if (currentTemplate?.showHeaderNote == true && activeNote.isNotBlank()) {
                            Text(
                                text = activeNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val gridStyle = currentTemplate?.gridLineStyle ?: "DASHED"
                        when (gridStyle) {
                            "DASHED" -> Text("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            "SOLID" -> HorizontalDivider(color = Color(0xFFCBD5E1))
                            "DOUBLE" -> {
                                HorizontalDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(2.dp))
                                HorizontalDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                            }
                            else -> Spacer(modifier = Modifier.height(2.dp))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Metadata: Date & Cashier
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ရက်စွဲ: ${receipt.businessDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569),
                                fontFamily = FontFamily.Monospace,
                            )
                            if (currentTemplate?.showCashierInfo == true) {
                                Text(
                                    text = "တာဝန်ခံ: ${receipt.cashierId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                )
                            }
                        }

                        // Customer Line
                        if (currentTemplate?.showCustomerInfo != false) {
                            val cLabel = currentTemplate?.customerLabel?.ifBlank { "ဈေးဝယ်သူ" } ?: "ဈေးဝယ်သူ"
                            val cName = receipt.customerName?.ifBlank { null } ?: "MK store"
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Text(
                                    text = "$cLabel: $cName",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        when (gridStyle) {
                            "DASHED" -> Text("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            "SOLID" -> HorizontalDivider(color = Color(0xFFCBD5E1))
                            "DOUBLE" -> {
                                HorizontalDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(2.dp))
                                HorizontalDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                            }
                            else -> Spacer(modifier = Modifier.height(2.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Body Items
                        if (currentTemplate?.itemLayoutStyle == "COLUMNS_4") {
                            // 4-Column Table (Template 2)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("နာမည်", modifier = Modifier.weight(0.42f), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                Text("ဦးရေ", modifier = Modifier.weight(0.16f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                Text("နှုန်း", modifier = Modifier.weight(0.20f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                Text("သင့်ငွေ", modifier = Modifier.weight(0.22f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }
                            receipt.items.forEach { line ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(line.productNameSnapshot, modifier = Modifier.weight(0.42f), fontSize = 11.sp)
                                    Text(com.example.offlinepos.domain.Money.formatQty(line.effectiveQuantity()), modifier = Modifier.weight(0.16f), textAlign = TextAlign.Center, fontSize = 11.sp)
                                    Text("%,d".format(line.unitPriceMinor), modifier = Modifier.weight(0.20f), textAlign = TextAlign.End, fontSize = 11.sp)
                                    Text("%,d".format(line.lineTotalMinor), modifier = Modifier.weight(0.22f), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                }
                            }
                        } else {
                            // 2-Line Sample Layout (Template 1)
                            receipt.items.forEach { line ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = line.productNameSnapshot,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                        )
                                        Text(
                                            text = "%,d Ks".format(line.lineTotalMinor),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                    Text(
                                        text = "${com.example.offlinepos.domain.Money.formatQty(line.effectiveQuantity())} ${line.unitName.ifBlank { "ခု" }} x %,d Ks".format(line.unitPriceMinor),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475569),
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        when (gridStyle) {
                            "DASHED" -> Text("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            "SOLID" -> HorizontalDivider(color = Color(0xFFCBD5E1))
                            "DOUBLE" -> {
                                HorizontalDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(2.dp))
                                HorizontalDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                            }
                            else -> Spacer(modifier = Modifier.height(2.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Totals Breakdown
                        ReceiptRow("ကျသင့်ငွေ", "%,d Ks".format(receipt.totals.subtotalMinor))
                        if (receipt.totals.discountMinor > 0) {
                            ReceiptRow("လျှော့ငွေ (Discount)", "-%,d Ks".format(receipt.totals.discountMinor), isNegative = true)
                        }
                        if (receipt.deliveryFeeMinor > 0) {
                            ReceiptRow("ပို့ဆောင်ခ (Delivery)", "%,d Ks".format(receipt.deliveryFeeMinor))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "စုစုပေါင်း",
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

                        if (currentTemplate?.showPaymentMethod == true) {
                            val methodText = when (receipt.paymentMethod) {
                                "CASH" -> "ငွေသား (Cash)"
                                "KPAY" -> "📱 KPay"
                                "WAVEPAY" -> "🟡 WavePay"
                                "CREDIT" -> "📝 အကြွေး (Credit)"
                                "SPLIT" -> "⚡ Split (ငွေသား + Digital)"
                                else -> receipt.paymentMethod
                            }
                            ReceiptRow("ပေးချေမှုပုံစံ", methodText)
                            if (receipt.paymentMethod == "CASH") {
                                ReceiptRow("ပေးငွေ", "%,d Ks".format(receipt.tenderedMinor))
                                if (receipt.changeMinor > 0) {
                                    ReceiptRow("ပြန်အမ်းငွေ", "%,d Ks".format(receipt.changeMinor), isHighlight = true)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        when (gridStyle) {
                            "DASHED" -> Text("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", color = Color(0xFF94A3B8), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            "SOLID" -> HorizontalDivider(color = Color(0xFFCBD5E1))
                            "DOUBLE" -> {
                                HorizontalDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(2.dp))
                                HorizontalDivider(color = Color(0xFF94A3B8), thickness = 1.dp)
                            }
                            else -> Spacer(modifier = Modifier.height(2.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // Voucher Number
                        if (currentTemplate?.showVoucherNumber != false) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                Text(
                                    text = "ပြေစာအမှတ်: ${receipt.displayVoucherNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155),
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Return Policy Text
                        val activeReturnPolicy = currentTemplate?.returnPolicyText.orEmpty()
                        if (activeReturnPolicy.isNotBlank()) {
                            Text(
                                text = activeReturnPolicy,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Footer Thank You Message
                        val activeFooterMsg = currentTemplate?.footerMessage?.ifBlank { storeProfile.footerMessage } ?: storeProfile.footerMessage
                        if (activeFooterMsg.isNotBlank()) {
                            Text(
                                text = activeFooterMsg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Extra Notice Text
                        val activeNotice = currentTemplate?.extraNoticeText.orEmpty()
                        if (activeNotice.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = activeNotice,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Print Time
                        if (currentTemplate?.showPrintTime != false) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val printTimeStr = "Print Time: ${SimpleDateFormat("dd/MM/yyyy hh:mma", Locale.ENGLISH).format(Date(receipt.timestampEpochMs))}"
                            Text(
                                text = printTimeStr,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        }
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

                // Receipt Template Selector (Slots 1 to 5)
                if (templates.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎨 ပြေစာ ပုံစံရွေးချယ်ရန် (Template)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(
                                onClick = { showTemplateBuilder = true },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("📐 ဒီဇိုင်းပြင်မည်", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(templates) { tmpl ->
                                val isSelected = tmpl.templateId == selectedTemplateId
                                androidx.compose.material3.FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedTemplateId = tmpl.templateId },
                                    label = {
                                        val pWidth = if (tmpl.paperWidthDots >= 500) "80mm" else "58mm"
                                        Text(
                                            text = if (tmpl.isDefault) "⭐ ${tmpl.name} [$pWidth]" else "${tmpl.name} [$pWidth]",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                                val chosenTmpl = templates.find { it.templateId == selectedTemplateId }
                                if (chosenTmpl != null) {
                                    viewModel.printCustomReceiptUnified(receipt, chosenTmpl) { success, msg ->
                                        isPrinting = false
                                        printStatus = msg
                                    }
                                } else {
                                    viewModel.printReceiptUnified(receipt) { success, msg ->
                                        isPrinting = false
                                        printStatus = msg
                                    }
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

    if (showPrinterDialog) {
        ThermalPrinterDialog(
            viewModel = viewModel,
            onDismiss = { showPrinterDialog = false }
        )
    }

    if (showTemplateBuilder) {
        ReceiptTemplateBuilderDialog(
            viewModel = viewModel,
            onDismiss = { showTemplateBuilder = false }
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
