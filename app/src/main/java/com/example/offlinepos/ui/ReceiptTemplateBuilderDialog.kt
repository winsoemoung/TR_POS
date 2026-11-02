package com.example.offlinepos.ui

import android.graphics.Bitmap
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.offlinepos.data.local.ReceiptTemplateEntity
import com.example.offlinepos.data.local.SaleItemEntity
import com.example.offlinepos.domain.CartTotals
import com.example.offlinepos.hardware.CustomReceiptEscPosGenerator
import com.example.offlinepos.hardware.ThermalPrinterManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BuilderTab(val label: String) {
    HEADER("🏷️ ခေါင်းစဉ် (Header)"),
    GRID_BODY("📊 ဇယားကွက် (Grid Body)"),
    FOOTER("📜 အောက်ခြေ (Footer)"),
    PREVIEW("👁️ Preview ပုံစံ"),
}

@Composable
fun ReceiptTemplateBuilderDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val templates by viewModel.receiptTemplates.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()

    var selectedSlot by remember { mutableIntStateOf(1) }
    var currentTab by remember { mutableStateOf(BuilderTab.GRID_BODY) }

    // Active editing template
    val currentDbTemplate = templates.find { it.templateId == selectedSlot }
        ?: ReceiptTemplateEntity(templateId = selectedSlot, name = "Template $selectedSlot")

    // State bindings
    var name by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.name) }
    var isDefault by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.isDefault) }

    // Header States
    var showLogo by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showLogo) }
    var storeNameFontSize by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.storeNameFontSize) }
    var storeNameAlign by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.storeNameAlign) }
    var showAddress by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showAddress) }
    var showPhone by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showPhone) }
    var showHeaderNote by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showHeaderNote) }
    var customHeaderNote by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customHeaderNote) }

    // Body Grid States
    var nameWeight by remember(selectedSlot, currentDbTemplate) { mutableFloatStateOf(currentDbTemplate.itemNameWidthWeight) }
    var qtyWeight by remember(selectedSlot, currentDbTemplate) { mutableFloatStateOf(currentDbTemplate.qtyWidthWeight) }
    var priceWeight by remember(selectedSlot, currentDbTemplate) { mutableFloatStateOf(currentDbTemplate.priceWidthWeight) }
    var amountWeight by remember(selectedSlot, currentDbTemplate) { mutableFloatStateOf(currentDbTemplate.amountWidthWeight) }
    var rowPaddingDp by remember(selectedSlot, currentDbTemplate) { mutableIntStateOf(currentDbTemplate.rowPaddingDp) }
    var gridLineStyle by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.gridLineStyle) }
    var bodyFontSize by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.bodyFontSize) }

    // Footer States
    var showTotalsBreakdown by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showTotalsBreakdown) }
    var showPaymentMethod by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showPaymentMethod) }
    var footerMessage by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.footerMessage) }
    var returnPolicyText by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.returnPolicyText) }
    var footerAlign by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.footerAlign) }

    // Sample receipt for live preview
    val sampleReceipt = remember {
        ReceiptData(
            saleId = "TR-SAMPLE-8899",
            businessDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
            items = listOf(
                SaleItemEntity(lineId = 1, saleId = "TR-SAMPLE-8899", productId = 1, productNameSnapshot = "ဘိလပ်မြေ (ဆင်တံဆိပ်)", quantity = 5, unitPriceMinor = 14500L, lineDiscountMinor = 0L, lineTaxMinor = 0L, lineTotalMinor = 72500L),
                SaleItemEntity(lineId = 2, saleId = "TR-SAMPLE-8899", productId = 2, productNameSnapshot = "သွပ်ပြား (အပြာရောင် 10 ပေ)", quantity = 10, unitPriceMinor = 8200L, lineDiscountMinor = 0L, lineTaxMinor = 0L, lineTotalMinor = 82000L),
                SaleItemEntity(lineId = 3, saleId = "TR-SAMPLE-8899", productId = 3, productNameSnapshot = "သံပိုက် 1.5 လက်မ", quantity = 2, unitPriceMinor = 12000L, lineDiscountMinor = 0L, lineTaxMinor = 0L, lineTotalMinor = 24000L),
            ),
            totals = CartTotals(
                subtotalMinor = 178500L,
                discountMinor = 3500L,
                taxMinor = 0L,
                totalMinor = 175000L,
                deliveryFeeMinor = 0L
            ),
            paymentMethod = "CASH",
            tenderedMinor = 180000L,
            changeMinor = 5000L,
            timestampEpochMs = System.currentTimeMillis(),
            cashierId = "Admin",
            deviceId = "Device-01",
            customerName = "ဦးကျော်သူ (09-4500123)",
            voucherNo = "TR-261101-0001"
        )
    }

    // Dynamic Live Preview Bitmap
    val previewTemplate = remember(
        name, isDefault, showLogo, storeNameFontSize, storeNameAlign, showAddress, showPhone,
        showHeaderNote, customHeaderNote, nameWeight, qtyWeight, priceWeight, amountWeight,
        rowPaddingDp, gridLineStyle, bodyFontSize, showTotalsBreakdown, showPaymentMethod,
        footerMessage, returnPolicyText, footerAlign
    ) {
        ReceiptTemplateEntity(
            templateId = selectedSlot,
            name = name,
            isDefault = isDefault,
            showLogo = showLogo,
            storeNameFontSize = storeNameFontSize,
            storeNameAlign = storeNameAlign,
            showAddress = showAddress,
            showPhone = showPhone,
            showHeaderNote = showHeaderNote,
            customHeaderNote = customHeaderNote,
            itemNameWidthWeight = nameWeight,
            qtyWidthWeight = qtyWeight,
            priceWidthWeight = priceWeight,
            amountWidthWeight = amountWeight,
            rowPaddingDp = rowPaddingDp,
            gridLineStyle = gridLineStyle,
            bodyFontSize = bodyFontSize,
            showTotalsBreakdown = showTotalsBreakdown,
            showPaymentMethod = showPaymentMethod,
            footerMessage = footerMessage,
            returnPolicyText = returnPolicyText,
            footerAlign = footerAlign
        )
    }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(previewTemplate, storeProfile) {
        previewBitmap = CustomReceiptEscPosGenerator.renderReceiptBitmap(
            template = previewTemplate,
            receipt = sampleReceipt,
            storeProfile = storeProfile,
            targetWidthDots = 576
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("receipt_builder_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ပြေစာပုံစံ ဖန်တီးခန်း (Receipt Template Builder)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ပုံစံ ၅ မျိုးအထိ စိတ်ကြိုက် Slot ခွဲခြား သိမ်းဆည်းနိုင်ပါသည်",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5 Slots Selector Chips
                Text("Template ရွေးချယ်ပါ (Slots 1 to 5):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..5).forEach { slotNum ->
                        val existing = templates.find { it.templateId == slotNum }
                        val isSel = selectedSlot == slotNum
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedSlot = slotNum },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (existing?.isDefault == true) {
                                        Icon(Icons.Default.Star, contentDescription = "Default", tint = Color(0xFFEAB308), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Text("ပုံစံ $slotNum", fontSize = 11.sp)
                                }
                            },
                            modifier = Modifier.testTag("template_slot_chip_$slotNum")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Template Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ပုံစံအမည် (Template Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("template_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Section Tabs: Header, Grid Body, Footer, Preview
                TabRow(
                    selectedTabIndex = currentTab.ordinal,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    BuilderTab.values().forEach { tab ->
                        Tab(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            text = { Text(tab.label, fontSize = 10.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section Contents
                when (currentTab) {
                    BuilderTab.HEADER -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("🏷️ ခေါင်းစဉ်အပိုင်း ပြင်ဆင်ရန် (Header Section)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showLogo, onCheckedChange = { showLogo = it })
                                    Text("ဆိုင်တံဆိပ် (Logo) ထည့်သွင်းမည်", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ဆိုင်နာမည် စာလုံးအရွယ်အစား (Font Size):", fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("SMALL" to "သေး", "MEDIUM" to "အလယ်", "LARGE" to "ကြီး", "XLARGE" to "အကြီးဆုံး").forEach { (sz, lbl) ->
                                        FilterChip(
                                            selected = storeNameFontSize == sz,
                                            onClick = { storeNameFontSize = sz },
                                            label = { Text(lbl, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("ဆိုင်နာမည် ချိန်ညှိမှု (Alignment):", fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("LEFT" to "ဘယ်", "CENTER" to "အလယ်", "RIGHT" to "ညာ").forEach { (al, lbl) ->
                                        FilterChip(
                                            selected = storeNameAlign == al,
                                            onClick = { storeNameAlign = al },
                                            label = { Text(lbl, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showAddress, onCheckedChange = { showAddress = it })
                                    Text("ဆိုင်လိပ်စာ ဖော်ပြမည်", fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showPhone, onCheckedChange = { showPhone = it })
                                    Text("ဆိုင်ဖုန်းနံပါတ် ဖော်ပြမည်", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = customHeaderNote,
                                    onValueChange = { customHeaderNote = it },
                                    label = { Text("အပိုဆောင်း ခေါင်းစဉ်မှတ်ချက် (Optional Note)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    BuilderTab.GRID_BODY -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("📊 Excel ပုံစံ ဇယားကွက် သတ်မှတ်ချက် (Body Grid & Columns)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("ကော်လံ အကျယ်အဝန်း အချိုးအစား (Column Width Weights):", fontSize = 11.sp, color = Color.DarkGray)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Item Name Width Slider
                                Text("၁။ ပစ္စည်းအမည် အကျယ် (Name Column): ${(nameWeight * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = nameWeight,
                                    onValueChange = {
                                        nameWeight = it
                                        val remaining = (1.0f - nameWeight).coerceAtLeast(0.2f)
                                        qtyWeight = remaining * 0.28f
                                        priceWeight = remaining * 0.34f
                                        amountWeight = remaining * 0.38f
                                    },
                                    valueRange = 0.25f..0.60f
                                )

                                // Qty Width
                                Text("၂။ အရေအတွက် အကျယ် (Qty Column): ${(qtyWeight * 100).toInt()}%", fontSize = 11.sp)
                                // Price Width
                                Text("၃။ နှုန်းထား အကျယ် (Price Column): ${(priceWeight * 100).toInt()}%", fontSize = 11.sp)
                                // Amount Width
                                Text("၄။ သင့်ငွေ အကျယ် (Amount Column): ${(amountWeight * 100).toInt()}%", fontSize = 11.sp)

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Row အကွာအဝေး (Padding / Height):", fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(2 to "ကျစ်လျစ် (Compact)", 4 to "ပုံမှန် (Normal)", 8 to "ကျယ်ပြန့် (Spacious)").forEach { (pad, lbl) ->
                                        FilterChip(
                                            selected = rowPaddingDp == pad,
                                            onClick = { rowPaddingDp = pad },
                                            label = { Text(lbl, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("ဇယားကွက် မျဉ်းကြောင်းပုံစံ (Grid Divider Lines):", fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("DASHED" to "ပြတ်မျဉ်း (- - -)", "SOLID" to "မျဉ်းဖြောင့် (───)", "DOUBLE" to "မျဉ်းနှစ်ထပ် (═══)", "NONE" to "မျဉ်းမပါ").forEach { (st, lbl) ->
                                        FilterChip(
                                            selected = gridLineStyle == st,
                                            onClick = { gridLineStyle = st },
                                            label = { Text(lbl, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("စာလုံး အရွယ်အစား (Body Font Size):", fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("SMALL" to "သေး (Small)", "MEDIUM" to "အလယ် (Medium)", "LARGE" to "ကြီး (Large)").forEach { (sz, lbl) ->
                                        FilterChip(
                                            selected = bodyFontSize == sz,
                                            onClick = { bodyFontSize = sz },
                                            label = { Text(lbl, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    BuilderTab.FOOTER -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("📜 အောက်ခြေအပိုင်း ပြင်ဆင်ရန် (Footer Section)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showTotalsBreakdown, onCheckedChange = { showTotalsBreakdown = it })
                                    Text("ကျသင့်ငွေ အသေးစိတ်ရှင်းတမ်း ဖော်ပြမည်", fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showPaymentMethod, onCheckedChange = { showPaymentMethod = it })
                                    Text("ပေးငွေနှင့် ပြန်အမ်းငွေ ဖော်ပြမည်", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = footerMessage,
                                    onValueChange = { footerMessage = it },
                                    label = { Text("နှုတ်ခွန်းဆက် စကား (Thank You Message)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = returnPolicyText,
                                    onValueChange = { returnPolicyText = it },
                                    label = { Text("ပစ္စည်း ပြန်လဲခြင်း စည်းကမ်းချက် (Return Policy)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("စာသား အထားအသို (Alignment):", fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("LEFT" to "ဘယ်", "CENTER" to "အလယ်", "RIGHT" to "ညာ").forEach { (al, lbl) ->
                                        FilterChip(
                                            selected = footerAlign == al,
                                            onClick = { footerAlign = al },
                                            label = { Text(lbl, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    BuilderTab.PREVIEW -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🖨️ Thermal Paper Live Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                                Spacer(modifier = Modifier.height(8.dp))

                                previewBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Receipt Template Live Preview",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Default Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        modifier = Modifier.testTag("set_default_checkbox")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "အရောင်းပြီးတိုင်း အသုံးပြုမည့် အဓိကပုံစံ (Set as Default Template)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Test Print & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val bytes = CustomReceiptEscPosGenerator.generateEscPosForTemplate(
                                template = previewTemplate,
                                receipt = sampleReceipt,
                                profile = storeProfile
                            )
                            scope.launch {
                                val result = ThermalPrinterManager.printRawBytes(context, bytes)
                                viewModel.setBanner(if (result.first) "စမ်းသပ်ပရင့် အောင်မြင်ပါသည်" else "ပရင့် အမှား: ${result.second}")
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("စမ်းသပ်ပရင့်", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.saveReceiptTemplate(previewTemplate) { success, msg ->
                                if (success && isDefault) {
                                    viewModel.setDefaultReceiptTemplate(selectedSlot)
                                }
                                viewModel.setBanner("Template $selectedSlot အား အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ")
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_receipt_template_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ပုံစံ သိမ်းဆည်းမည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
