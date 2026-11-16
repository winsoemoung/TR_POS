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
    val allSaleItems by viewModel.allSaleItems.collectAsState()

    val displayReceipt = remember(receipt, allSaleItems) {
        val resolvedItems = if (receipt.items.isNotEmpty()) {
            receipt.items
        } else {
            allSaleItems.filter { it.saleId == receipt.saleId }
        }
        if (resolvedItems === receipt.items) receipt else receipt.copy(items = resolvedItems)
    }

    var selectedTemplateId by remember(defaultTemplate) { mutableStateOf(defaultTemplate?.templateId ?: 1) }
    var showThermalPreview by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showTemplateBuilder by remember { mutableStateOf(false) }
    var printStatus by remember { mutableStateOf<String?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    val activeTemplate = remember(templates, defaultTemplate, selectedTemplateId) {
        templates.find { it.templateId == selectedTemplateId }
            ?: defaultTemplate
            ?: com.example.offlinepos.data.local.ReceiptTemplateEntity(
                templateId = selectedTemplateId,
                name = "Template $selectedTemplateId",
                paperWidthDots = 576
            )
    }

    val previewBitmap = remember(showThermalPreview, activeTemplate, displayReceipt, storeProfile) {
        if (!showThermalPreview) {
            null
        } else {
            runCatching {
                com.example.offlinepos.hardware.CustomReceiptEscPosGenerator.renderReceiptBitmap(
                    template = activeTemplate,
                    receipt = displayReceipt,
                    storeProfile = storeProfile,
                    targetWidthDots = if (activeTemplate.paperWidthDots > 0) activeTemplate.paperWidthDots else 576,
                )
            }.getOrNull()
        }
    }

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (displayReceipt.paymentMethod == "CREDIT") "အကြွေးပြေစာ (Credit Receipt)" else "အရောင်းပြေစာ (Receipt)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "ပစ္စည်း ${displayReceipt.items.size} မျိုး • %,d Ks".format(displayReceipt.totals.totalMinor),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Switch between Receipt Card & Pre-Print Thermal Bitmap Preview
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = !showThermalPreview,
                        onClick = { showThermalPreview = false },
                        label = { Text("📄 ပြေစာပုံစံ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f).testTag("receipt_mode_standard_chip"),
                    )
                    androidx.compose.material3.FilterChip(
                        selected = showThermalPreview,
                        onClick = { showThermalPreview = true },
                        label = { Text("🖨️ Print Preview (အစမ်း)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f).testTag("receipt_mode_thermal_preview_chip"),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (showThermalPreview && previewBitmap != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            val paperMmLabel = if (activeTemplate.paperWidthDots >= 500) "80mm" else "58mm"
                            Text(
                                text = "Pre-Print Thermal Preview ($paperMmLabel • ${activeTemplate.name})",
                                fontSize = 10.5.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Thermal Receipt Pre-Print Preview",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                    .testTag("thermal_preprint_preview_image"),
                            )
                        }
                    }
                } else {
                    // Paper receipt card styling with Myanmar text & custom Store Profile
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), // Warm paper yellow
                        border = CardDefaults.outlinedCardBorder(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        val currentTemplate = activeTemplate
                        val fontMultiplier = ((currentTemplate.fontScalePercent).coerceIn(60, 200) / 100f)
                        val lineSpacingExtra = ((currentTemplate.lineSpacingDp).coerceIn(0, 24)).dp

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (currentTemplate.showLogo) {
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
                            val activeStoreName = currentTemplate.customStoreName.ifBlank {
                                storeProfile.storeName.ifBlank { "ဆန်းသစ်" }
                            }

                            Text(
                                text = activeStoreName,
                                fontSize = (17f * fontMultiplier).sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )

                            // Subtitle
                            val activeSubtitle = currentTemplate.customSubtitle
                            if (activeSubtitle.isNotBlank()) {
                                Text(
                                    text = activeSubtitle,
                                    fontSize = (13f * fontMultiplier).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            // Owner / Contact
                            val activeOwner = currentTemplate.customOwnerOrContact
                            if (activeOwner.isNotBlank()) {
                                Text(
                                    text = activeOwner,
                                    fontSize = (12f * fontMultiplier).sp,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            // Phone Numbers
                            val activePhone = if (currentTemplate.showPhone) {
                                currentTemplate.customPhone.ifBlank { storeProfile.phone }
                            } else ""
                            if (activePhone.isNotBlank()) {
                                Text(
                                    text = activePhone,
                                    fontSize = (11f * fontMultiplier).sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            // Address
                            val activeAddress = if (currentTemplate.showAddress) {
                                currentTemplate.customAddress.ifBlank { storeProfile.address }
                            } else ""
                            if (activeAddress.isNotBlank()) {
                                Text(
                                    text = activeAddress,
                                    fontSize = (11f * fontMultiplier).sp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            // Header Note
                            val activeNote = currentTemplate.customHeaderNote.ifBlank { storeProfile.headerNote }
                            if (currentTemplate.showHeaderNote && activeNote.isNotBlank()) {
                                Text(
                                    text = activeNote,
                                    fontSize = (11.5f * fontMultiplier).sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            val gridStyle = currentTemplate.gridLineStyle
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
                                    text = "ရက်စွဲ: ${displayReceipt.businessDate}",
                                    fontSize = (11f * fontMultiplier).sp,
                                    color = Color(0xFF475569),
                                    fontFamily = FontFamily.Monospace,
                                )
                                if (currentTemplate.showCashierInfo) {
                                    Text(
                                        text = "တာဝန်ခံ: ${displayReceipt.cashierId}",
                                        fontSize = (11f * fontMultiplier).sp,
                                        color = Color(0xFF64748B),
                                    )
                                }
                            }

                            // Customer Line
                            if (currentTemplate.showCustomerInfo) {
                                val cLabel = currentTemplate.customerLabel.ifBlank { "ဈေးဝယ်သူ" }
                                val cName = displayReceipt.customerName?.ifBlank { null } ?: "MK store"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = "$cLabel: $cName",
                                        fontSize = (12f * fontMultiplier).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                    )
                                }
                            }

                            if (displayReceipt.paymentMethod == "CREDIT") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "📝 အကြွေးဝယ်ယူမှု ပြေစာ (CREDIT RECEIPT)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 3.dp, horizontal = 6.dp),
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

                            // Body Items — always high-contrast dark ink on warm paper regardless of Light/Dark theme
                            if (displayReceipt.items.isEmpty()) {
                                Surface(
                                    color = Color(0xFFFEE2E2),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "⚠️ ဝယ်ယူထားသော ပစ္စည်းစာရင်း မှတ်တမ်း မတွေ့ပါ (Ref: #${displayReceipt.saleId.take(8).uppercase()})",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF991B1B),
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(8.dp),
                                    )
                                }
                            } else if (currentTemplate.itemLayoutStyle == "COLUMNS_4") {
                                // 4-Column Table (Templates 2-5)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("နာမည်", modifier = Modifier.weight(0.42f), fontWeight = FontWeight.Bold, fontSize = (11.5f * fontMultiplier).sp, color = Color(0xFF0F172A))
                                    Text("ဦးရေ", modifier = Modifier.weight(0.16f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = (11.5f * fontMultiplier).sp, color = Color(0xFF0F172A))
                                    Text("နှုန်း", modifier = Modifier.weight(0.20f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = (11.5f * fontMultiplier).sp, color = Color(0xFF0F172A))
                                    Text("သင့်ငွေ", modifier = Modifier.weight(0.22f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = (11.5f * fontMultiplier).sp, color = Color(0xFF0F172A))
                                }
                                displayReceipt.items.forEach { line ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = (lineSpacingExtra.value.coerceAtLeast(2f) * 0.7f).dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = line.productNameSnapshot,
                                            modifier = Modifier.weight(0.42f),
                                            fontSize = (11.5f * fontMultiplier).sp,
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            text = com.example.offlinepos.domain.Money.formatQty(line.effectiveQuantity()),
                                            modifier = Modifier.weight(0.16f),
                                            textAlign = TextAlign.Center,
                                            fontSize = (11.5f * fontMultiplier).sp,
                                            color = Color(0xFF1E293B),
                                        )
                                        Text(
                                            text = "%,d".format(line.unitPriceMinor),
                                            modifier = Modifier.weight(0.20f),
                                            textAlign = TextAlign.End,
                                            fontSize = (11.5f * fontMultiplier).sp,
                                            color = Color(0xFF334155),
                                            fontFamily = FontFamily.Monospace,
                                        )
                                        Text(
                                            text = "%,d".format(line.lineTotalMinor),
                                            modifier = Modifier.weight(0.22f),
                                            textAlign = TextAlign.End,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = (11.5f * fontMultiplier).sp,
                                            color = Color(0xFF0F172A),
                                            fontFamily = FontFamily.Monospace,
                                        )
                                    }
                                }
                            } else {
                                // 2-Line Sample Layout (Template 1)
                                displayReceipt.items.forEach { line ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = (lineSpacingExtra.value.coerceAtLeast(2f) * 0.6f).dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = line.productNameSnapshot,
                                                fontSize = (13f * fontMultiplier).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.weight(1f),
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "%,d Ks".format(line.lineTotalMinor),
                                                fontSize = (13f * fontMultiplier).sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                fontFamily = FontFamily.Monospace,
                                            )
                                        }
                                        Text(
                                            text = "${com.example.offlinepos.domain.Money.formatQty(line.effectiveQuantity())} ${line.unitName.ifBlank { "ခု" }} x %,d Ks".format(line.unitPriceMinor),
                                            fontSize = (11.5f * fontMultiplier).sp,
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
                            ReceiptRow("ကျသင့်ငွေ", "%,d Ks".format(displayReceipt.totals.subtotalMinor))
                            if (displayReceipt.totals.discountMinor > 0) {
                                ReceiptRow("လျှော့ငွေ (Discount)", "-%,d Ks".format(displayReceipt.totals.discountMinor), isNegative = true)
                            }
                            if (displayReceipt.deliveryFeeMinor > 0) {
                                ReceiptRow("ပို့ဆောင်ခ (Delivery)", "%,d Ks".format(displayReceipt.deliveryFeeMinor))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "စုစုပေါင်း",
                                    fontSize = (15f * fontMultiplier).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                )
                                Text(
                                    text = "%,d Ks".format(displayReceipt.totals.totalMinor),
                                    fontSize = (15f * fontMultiplier).sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A),
                                    fontFamily = FontFamily.Monospace,
                                )
                            }

                            if (currentTemplate.showPaymentMethod || displayReceipt.paymentMethod == "CREDIT") {
                                val methodText = when (displayReceipt.paymentMethod) {
                                    "CASH" -> "ငွေသား (Cash)"
                                    "KPAY" -> "📱 KPay"
                                    "WAVEPAY" -> "🟡 WavePay"
                                    "CREDIT" -> "📝 အကြွေး (Credit)"
                                    "SPLIT" -> "⚡ Split (ငွေသား + Digital)"
                                    else -> displayReceipt.paymentMethod
                                }
                                ReceiptRow("ပေးချေမှုပုံစံ", methodText)
                                if (displayReceipt.paymentMethod == "CREDIT") {
                                    val creditAmt = (displayReceipt.totals.totalMinor - displayReceipt.tenderedMinor).coerceAtLeast(0L)
                                    ReceiptRow("အကြွေးမှတ်တမ်းတင်ငွေ", "%,d Ks".format(creditAmt), isNegative = true)
                                } else if (displayReceipt.paymentMethod == "CASH") {
                                    ReceiptRow("ပေးငွေ", "%,d Ks".format(displayReceipt.tenderedMinor))
                                    if (displayReceipt.changeMinor > 0) {
                                        ReceiptRow("ပြန်အမ်းငွေ", "%,d Ks".format(displayReceipt.changeMinor), isHighlight = true)
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
                            if (currentTemplate.showVoucherNumber) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                                    Text(
                                        text = "ပြေစာအမှတ်: ${displayReceipt.displayVoucherNumber}",
                                        fontSize = (11f * fontMultiplier).sp,
                                        color = Color(0xFF334155),
                                        fontFamily = FontFamily.Monospace,
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Return Policy Text
                            val activeReturnPolicy = currentTemplate.returnPolicyText
                            if (activeReturnPolicy.isNotBlank()) {
                                Text(
                                    text = activeReturnPolicy,
                                    fontSize = (10.5f * fontMultiplier).sp,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            // Footer Thank You Message
                            val activeFooterMsg = currentTemplate.footerMessage.ifBlank { storeProfile.footerMessage }
                            if (activeFooterMsg.isNotBlank()) {
                                Text(
                                    text = activeFooterMsg,
                                    fontSize = (11.5f * fontMultiplier).sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            // Extra Notice Text
                            val activeNotice = currentTemplate.extraNoticeText
                            if (activeNotice.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = activeNotice,
                                    fontSize = (11.5f * fontMultiplier).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }

                            // Print Time
                            if (currentTemplate.showPrintTime) {
                                Spacer(modifier = Modifier.height(4.dp))
                                val printTimeStr = "Print Time: ${SimpleDateFormat("dd/MM/yyyy hh:mma", Locale.ENGLISH).format(Date(displayReceipt.timestampEpochMs))}"
                                Text(
                                    text = printTimeStr,
                                    color = Color(0xFF64748B),
                                    fontSize = (10f * fontMultiplier).sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                )
                            }
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
                        onClick = { viewModel.sharePdfReceipt(displayReceipt) },
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
                                    viewModel.printCustomReceiptUnified(displayReceipt, chosenTmpl) { _, msg ->
                                        isPrinting = false
                                        printStatus = msg
                                    }
                                } else {
                                    viewModel.printReceiptUnified(displayReceipt) { _, msg ->
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
