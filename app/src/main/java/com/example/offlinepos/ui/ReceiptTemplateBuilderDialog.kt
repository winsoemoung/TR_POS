package com.example.offlinepos.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
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
import kotlinx.coroutines.launch

enum class BuilderTab(val label: String) {
    FONT_SPACING("🔤 Font/Spacing"),
    HEADER("🏷️ Header"),
    GRID_BODY("📊 Body"),
    FOOTER("📜 Footer"),
    PREVIEW("👁️ Preview"),
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
    var currentTab by remember { mutableStateOf(BuilderTab.FONT_SPACING) }

    // Active editing template
    val currentDbTemplate = templates.find { it.templateId == selectedSlot }
        ?: ReceiptTemplateEntity(templateId = selectedSlot, name = "Template $selectedSlot", paperWidthDots = 576)

    // State bindings
    var name by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.name) }
    var isDefault by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.isDefault) }
    // Default 80mm (576 dots)
    var paperWidthDots by remember(selectedSlot, currentDbTemplate) {
        mutableIntStateOf(if (currentDbTemplate.paperWidthDots > 0) currentDbTemplate.paperWidthDots else 576)
    }
    var itemLayoutStyle by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.itemLayoutStyle) }

    // Font & Spacing States
    var fontScalePercent by remember(selectedSlot, currentDbTemplate) { mutableIntStateOf(currentDbTemplate.fontScalePercent) }
    var lineSpacingDp by remember(selectedSlot, currentDbTemplate) { mutableIntStateOf(currentDbTemplate.lineSpacingDp) }
    var storeNameFontSize by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.storeNameFontSize) }
    var headerDetailsFontSize by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.headerDetailsFontSize) }
    var metaFontSize by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.metaFontSize) }
    var bodyFontSize by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.bodyFontSize) }
    var totalsFontSize by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.totalsFontSize) }
    var footerFontSize by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.footerFontSize) }

    // Header States
    var showLogo by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showLogo) }
    var customStoreName by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customStoreName) }
    var customSubtitle by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customSubtitle) }
    var customOwnerOrContact by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customOwnerOrContact) }
    var customPhone by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customPhone) }
    var customAddress by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customAddress) }
    var storeNameAlign by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.storeNameAlign) }
    var showAddress by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showAddress) }
    var showPhone by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showPhone) }
    var showHeaderNote by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showHeaderNote) }
    var customHeaderNote by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customHeaderNote) }
    var showCashierInfo by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showCashierInfo) }
    var showVoucherNumber by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showVoucherNumber) }
    var showCustomerInfo by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showCustomerInfo) }
    var customerLabel by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customerLabel) }

    // Body Grid States
    var nameWeight by remember(selectedSlot, currentDbTemplate) { mutableFloatStateOf(currentDbTemplate.itemNameWidthWeight) }
    var qtyWeight by remember(selectedSlot, currentDbTemplate) { mutableFloatStateOf(currentDbTemplate.qtyWidthWeight) }
    var priceWeight by remember(selectedSlot, currentDbTemplate) { mutableFloatStateOf(currentDbTemplate.priceWidthWeight) }
    var amountWeight by remember(selectedSlot, currentDbTemplate) { mutableFloatStateOf(currentDbTemplate.amountWidthWeight) }
    var rowPaddingDp by remember(selectedSlot, currentDbTemplate) { mutableIntStateOf(currentDbTemplate.rowPaddingDp) }
    var gridLineStyle by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.gridLineStyle) }

    // Footer States
    var showTotalsBreakdown by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showTotalsBreakdown) }
    var showPaymentMethod by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showPaymentMethod) }
    var footerMessage by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.footerMessage) }
    var returnPolicyText by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.returnPolicyText) }
    var extraNoticeText by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.extraNoticeText) }
    var showPrintTime by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showPrintTime) }
    var footerAlign by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.footerAlign) }

    // Sample receipt for live preview matching real data
    val sampleReceipt = remember {
        ReceiptData(
            saleId = "TR-SAMPLE-37416692",
            businessDate = "07/11/2026 10:23AM",
            items = listOf(
                SaleItemEntity(
                    lineId = 1,
                    saleId = "TR-SAMPLE-37416692",
                    productId = 1,
                    productNameSnapshot = "ကြံ့နီ ဘိလပ်မြေ",
                    quantity = 1,
                    unitPriceMinor = 31000L,
                    lineDiscountMinor = 0L,
                    lineTaxMinor = 0L,
                    lineTotalMinor = 31000L,
                    unitName = "အိတ်"
                ),
            ),
            totals = CartTotals(
                subtotalMinor = 31000L,
                discountMinor = 0L,
                taxMinor = 0L,
                totalMinor = 31000L,
                deliveryFeeMinor = 0L
            ),
            paymentMethod = "CASH",
            tenderedMinor = 31000L,
            changeMinor = 0L,
            timestampEpochMs = System.currentTimeMillis(),
            cashierId = "Admin",
            deviceId = "Device-01",
            customerName = "MK store",
            voucherNo = "A261107-37416692"
        )
    }

    // Dynamic Live Preview Template Entity
    val previewTemplate = remember(
        name, isDefault, paperWidthDots, itemLayoutStyle, fontScalePercent, lineSpacingDp,
        showLogo, customStoreName, customSubtitle, customOwnerOrContact, customPhone, customAddress,
        storeNameFontSize, storeNameAlign, headerDetailsFontSize, metaFontSize, showAddress, showPhone,
        showHeaderNote, customHeaderNote, showCashierInfo, showVoucherNumber, showCustomerInfo, customerLabel,
        nameWeight, qtyWeight, priceWeight, amountWeight, rowPaddingDp, gridLineStyle, bodyFontSize,
        totalsFontSize, showTotalsBreakdown, showPaymentMethod, footerMessage, returnPolicyText,
        extraNoticeText, showPrintTime, footerAlign, footerFontSize
    ) {
        ReceiptTemplateEntity(
            templateId = selectedSlot,
            name = name,
            isDefault = isDefault,
            paperWidthDots = paperWidthDots,
            itemLayoutStyle = itemLayoutStyle,
            fontScalePercent = fontScalePercent,
            lineSpacingDp = lineSpacingDp,
            showLogo = showLogo,
            customStoreName = customStoreName,
            customSubtitle = customSubtitle,
            customOwnerOrContact = customOwnerOrContact,
            customPhone = customPhone,
            customAddress = customAddress,
            storeNameFontSize = storeNameFontSize,
            storeNameAlign = storeNameAlign,
            headerDetailsFontSize = headerDetailsFontSize,
            metaFontSize = metaFontSize,
            showAddress = showAddress,
            showPhone = showPhone,
            showHeaderNote = showHeaderNote,
            customHeaderNote = customHeaderNote,
            showCashierInfo = showCashierInfo,
            showVoucherNumber = showVoucherNumber,
            showCustomerInfo = showCustomerInfo,
            customerLabel = customerLabel,
            itemNameWidthWeight = nameWeight,
            qtyWidthWeight = qtyWeight,
            priceWidthWeight = priceWeight,
            amountWidthWeight = amountWeight,
            rowPaddingDp = rowPaddingDp,
            gridLineStyle = gridLineStyle,
            bodyFontSize = bodyFontSize,
            totalsFontSize = totalsFontSize,
            showTotalsBreakdown = showTotalsBreakdown,
            showPaymentMethod = showPaymentMethod,
            footerMessage = footerMessage,
            returnPolicyText = returnPolicyText,
            extraNoticeText = extraNoticeText,
            showPrintTime = showPrintTime,
            footerAlign = footerAlign,
            footerFontSize = footerFontSize
        )
    }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(previewTemplate, storeProfile) {
        previewBitmap = CustomReceiptEscPosGenerator.renderReceiptBitmap(
            template = previewTemplate,
            receipt = sampleReceipt,
            storeProfile = storeProfile,
            targetWidthDots = previewTemplate.paperWidthDots
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
                            text = "📐 Receipt Template Builder (1 to 5)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Default 80mm စံနှုန်း | Font အကြီး/အသေး နှင့် Line Spacing စိတ်ကြိုက်ပြင်ဆင်နိုင်သည်",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Template Slot Selector (1 to 5)
                Text("🎨 ပြေစာပုံစံ အကွက် ရွေးပါ:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..5).forEach { slot ->
                        val isSelected = selectedSlot == slot
                        val tmpl = templates.find { it.templateId == slot }
                        val isDef = tmpl?.isDefault == true
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSlot = slot },
                            label = {
                                Text(
                                    text = if (isDef) "⭐ T$slot" else "T$slot",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Template Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ပုံစံအမည် (Template Name)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Paper Width Setting (Default 80mm for all templates)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🖨️ ပရင့်တာ စက္ကူအရွယ်အစား (Paper Width):",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (paperWidthDots >= 500) "80 mm (Default စံ)" else "58 mm",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = paperWidthDots >= 500,
                                onClick = { paperWidthDots = 576 },
                                label = { Text("80mm (Default စံနှုန်း - 576 dots)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1.3f)
                            )
                            FilterChip(
                                selected = paperWidthDots < 500,
                                onClick = { paperWidthDots = 384 },
                                label = { Text("58mm (384 dots)", fontSize = 11.sp) },
                                modifier = Modifier.weight(0.9f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = currentTab.ordinal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BuilderTab.values().forEach { tab ->
                        Tab(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            text = { Text(tab.label, fontSize = 10.5.sp, maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (currentTab) {
                    BuilderTab.FONT_SPACING -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 1. Overall Font Scaling
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🔍 ပြေစာတစ်ခုလုံး Font အကြီး/အသေး:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$fontScalePercent% ${if (fontScalePercent == 100) "(ပုံမှန်)" else if (fontScalePercent > 100) "(ပိုကြီး)" else "(ပိုသေး)"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = fontScalePercent.toFloat(),
                                        onValueChange = { fontScalePercent = it.toInt() },
                                        valueRange = 70f..160f,
                                        steps = 8
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(85 to "သေး", 100 to "ပုံမှန်", 115 to "ကြီး", 130 to "အလွန်ကြီး").forEach { (scale, lbl) ->
                                            FilterChip(
                                                selected = fontScalePercent == scale,
                                                onClick = { fontScalePercent = scale },
                                                label = { Text("$lbl ($scale%)", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Line Spacing
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📏 စာကြောင်းအကွာအဝေး (Line Spacing):",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val spacingLabel = when {
                                            lineSpacingDp <= 1 -> "ကျစ်လျစ်လွန်း"
                                            lineSpacingDp in 2..3 -> "ကျစ်လျစ်"
                                            lineSpacingDp in 4..5 -> "ပုံမှန် စံသတ်မှတ်ချက်"
                                            lineSpacingDp in 6..8 -> "သက်သောင့်သက်သာ"
                                            lineSpacingDp in 9..12 -> "ကျယ်ဝန်း"
                                            else -> "အထူးကျယ်"
                                        }
                                        Text(
                                            text = "$lineSpacingDp dp ($spacingLabel)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Slider(
                                        value = lineSpacingDp.toFloat(),
                                        onValueChange = { lineSpacingDp = it.toInt() },
                                        valueRange = 0f..16f,
                                        steps = 15
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(2 to "ကျစ်လျစ် (2dp)", 4 to "ပုံမှန် (4dp)", 7 to "သက်သာ (7dp)", 10 to "ကျယ် (10dp)").forEach { (spVal, lbl) ->
                                            FilterChip(
                                                selected = lineSpacingDp == spVal,
                                                onClick = { lineSpacingDp = spVal },
                                                label = { Text(lbl, fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Section by Section Font Sizes
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("🔠 အပိုင်းအလိုက် Font အရွယ်အစားများ သီးခြားရွေးချယ်ရန်:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)

                                    // Store Name Font Size
                                    FontSizeSelectorRow("ဆိုင်နာမည် (Store Name):", storeNameFontSize, listOf("SMALL", "MEDIUM", "LARGE", "XLARGE", "XXLARGE")) {
                                        storeNameFontSize = it
                                    }

                                    // Header Details Font Size
                                    FontSizeSelectorRow("ခေါင်းစဉ်အသေးစိတ် (Subtitle/Contact/Phone/Address):", headerDetailsFontSize) {
                                        headerDetailsFontSize = it
                                    }

                                    // Metadata Font Size
                                    FontSizeSelectorRow("ရက်စွဲ၊ ဘောင်ချာနှင့် ဝယ်သူ (Date/Voucher/Customer):", metaFontSize) {
                                        metaFontSize = it
                                    }

                                    // Body Items Font Size
                                    FontSizeSelectorRow("ကုန်ပစ္စည်းစာရင်း (Items/Body Table):", bodyFontSize) {
                                        bodyFontSize = it
                                    }

                                    // Totals Font Size
                                    FontSizeSelectorRow("ကျသင့်ငွေနှင့် စုစုပေါင်း (Totals & Payment):", totalsFontSize) {
                                        totalsFontSize = it
                                    }

                                    // Footer Font Size
                                    FontSizeSelectorRow("အောက်ခြေစာသား (Footer & Notices):", footerFontSize) {
                                        footerFontSize = it
                                    }
                                }
                            }
                        }
                    }

                    BuilderTab.HEADER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Logo toggle
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showLogo, onCheckedChange = { showLogo = it })
                                Text("ဆိုင် Logo အမှတ်တံဆိပ် ပြသမည်", fontSize = 12.sp)
                            }

                            // Store Name Customization
                            OutlinedTextField(
                                value = customStoreName,
                                onValueChange = { customStoreName = it },
                                label = { Text("ဆိုင်နာမည် (Store Name)") },
                                placeholder = { Text("ဥပမာ - ဆန်းသစ်") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Subtitle
                            OutlinedTextField(
                                value = customSubtitle,
                                onValueChange = { customSubtitle = it },
                                label = { Text("ဆိုင်အမျိုးအစား (Subtitle)") },
                                placeholder = { Text("ဥပမာ - အိမ်ဆောက်ပစ္စည်း အရောင်းဆိုင်") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Owner / Contact
                            OutlinedTextField(
                                value = customOwnerOrContact,
                                onValueChange = { customOwnerOrContact = it },
                                label = { Text("ဆိုင်ရှင် / ဆက်သွယ်ရန် (Owner / Contact)") },
                                placeholder = { Text("ဥပမာ - ကိုပါပီး+မသီရိကလျာ") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Phone
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showPhone, onCheckedChange = { showPhone = it })
                                Text("ဖုန်းနံပါတ် ပြသမည်", fontSize = 12.sp)
                            }
                            if (showPhone) {
                                OutlinedTextField(
                                    value = customPhone,
                                    onValueChange = { customPhone = it },
                                    label = { Text("ဖုန်းနံပါတ်များ (Phone Numbers)") },
                                    placeholder = { Text("09678473426 / 09943070451...") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Address
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showAddress, onCheckedChange = { showAddress = it })
                                Text("ဆိုင်လိပ်စာ ပြသမည်", fontSize = 12.sp)
                            }
                            if (showAddress) {
                                OutlinedTextField(
                                    value = customAddress,
                                    onValueChange = { customAddress = it },
                                    label = { Text("ဆိုင်လိပ်စာ (Store Address)") },
                                    placeholder = { Text("3/3 ဗိုလ်ချုပ်လမ်း / စမ်းမြို့ရပ်ကွက် / ကလေးမြို့") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Customer Label & Toggle
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showCustomerInfo, onCheckedChange = { showCustomerInfo = it })
                                Text("ဈေးဝယ်သူ အချက်အလက် ပြသမည်", fontSize = 12.sp)
                            }
                            if (showCustomerInfo) {
                                OutlinedTextField(
                                    value = customerLabel,
                                    onValueChange = { customerLabel = it },
                                    label = { Text("ဈေးဝယ်သူ စာသားခေါင်းစဉ် (Customer Label)") },
                                    placeholder = { Text("ဥပမာ - ဈေးဝယ်သူ သို့မဟုတ် ဝယ်သူအမည်") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Additional Options
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showVoucherNumber, onCheckedChange = { showVoucherNumber = it })
                                    Text("ပြေစာအမှတ်", fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showCashierInfo, onCheckedChange = { showCashierInfo = it })
                                    Text("တာဝန်ခံ/အရောင်းစာရေး", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    BuilderTab.GRID_BODY -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Layout Style Selector (2-Line vs 4-Column)
                            Text("📋 ကုန်ပစ္စည်း ပြသမှု ပုံစံ (Item Layout Style):", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = itemLayoutStyle == "STANDARD_2LINE",
                                    onClick = { itemLayoutStyle = "STANDARD_2LINE" },
                                    label = { Text("၂ ကြောင်းစံ (Template 1 ပုံစံ)", fontSize = 10.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = itemLayoutStyle == "COLUMNS_4",
                                    onClick = { itemLayoutStyle = "COLUMNS_4" },
                                    label = { Text("၄ ကော်လံ ဇယား (Template 2 ပုံစံ)", fontSize = 10.5.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Grid Line Style
                            Text("📏 မျဉ်းစတိုင် (Grid Line Style):", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("NONE" to "မျဉ်းမပါ (None)", "DASHED" to "အစက်မျဉ်း (Dashed)", "SOLID" to "မျဉ်းဖြောင့်", "DOUBLE" to "မျဉ်းနှစ်ထပ်").forEach { (style, label) ->
                                    FilterChip(
                                        selected = gridLineStyle.uppercase() == style,
                                        onClick = { gridLineStyle = style },
                                        label = { Text(label, fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Row Padding
                            Text("အကွက် အကွာအဝေး (Row Padding): $rowPaddingDp dp", fontSize = 11.sp)
                            Slider(
                                value = rowPaddingDp.toFloat(),
                                onValueChange = { rowPaddingDp = it.toInt() },
                                valueRange = 1f..10f,
                                steps = 8
                            )
                        }
                    }

                    BuilderTab.FOOTER -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = returnPolicyText,
                                onValueChange = { returnPolicyText = it },
                                label = { Text("ပစ္စည်းလဲလှယ်ခြင်း စည်းကမ်းချက် (Return Policy)") },
                                placeholder = { Text("ဥပမာ - ဝယ်ယူပြီး ပစ္စည်း များကို ပြန်လဲမပေးပါ") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = footerMessage,
                                onValueChange = { footerMessage = it },
                                label = { Text("အောက်ခြေ ကျေးဇူးတင်လွှာ (Footer Message)") },
                                placeholder = { Text("ဥပမာ - ဝယ်ယူအားပေးမှုကိုကျေးဇူးအထူးတင်ရှိပါသည်") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = extraNoticeText,
                                onValueChange = { extraNoticeText = it },
                                label = { Text("ဆိုင်အထူးကြော်ငြာ / အသိပေးချက် (Extra Notice)") },
                                placeholder = { Text("သုံးမူး၊ ငါးမူး၊ ခြောက်မူး သံပင် များ ၊\nသွပ် အစားထိုး polystar များ ရပါသည် ။") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showPrintTime, onCheckedChange = { showPrintTime = it })
                                Text("Print Time အချိန် ပြသမည်", fontSize = 12.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showPaymentMethod, onCheckedChange = { showPaymentMethod = it })
                                Text("ငွေပေးချေမှုအမျိုးအစား (Cash/KBZPay) ပြသမည်", fontSize = 12.sp)
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
                                Text(
                                    text = "စက္ကူ: ${if (paperWidthDots >= 500) "80mm Wide (Default)" else "58mm Compact"} | Font: $fontScalePercent% | Spacing: ${lineSpacingDp}dp",
                                    fontSize = 10.5.sp,
                                    color = Color.Gray
                                )
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
                                profile = storeProfile,
                                paperWidthDots = previewTemplate.paperWidthDots,
                                cutMode = ThermalPrinterManager.getCutMode(context),
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
                            viewModel.saveReceiptTemplate(previewTemplate) { success, _ ->
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

@Composable
private fun FontSizeSelectorRow(
    title: String,
    currentValue: String,
    options: List<String> = listOf("SMALL", "MEDIUM", "LARGE", "XLARGE"),
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { opt ->
                val isSelected = currentValue.uppercase() == opt.uppercase()
                val label = when (opt.uppercase()) {
                    "SMALL" -> "သေး"
                    "MEDIUM" -> "ပုံမှန်"
                    "LARGE" -> "ကြီး"
                    "XLARGE" -> "ပိုကြီး"
                    "XXLARGE" -> "အလွန်ကြီး"
                    else -> opt
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(opt) },
                    label = { Text(label, fontSize = 9.5.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
