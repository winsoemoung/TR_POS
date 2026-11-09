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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BuilderTab(val label: String) {
    HEADER("🏷️ Header ခေါင်းစဉ်"),
    GRID_BODY("📊 Body ကုန်ပစ္စည်း"),
    FOOTER("📜 Footer အောက်ခြေ"),
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
    var currentTab by remember { mutableStateOf(BuilderTab.PREVIEW) }

    // Active editing template
    val currentDbTemplate = templates.find { it.templateId == selectedSlot }
        ?: ReceiptTemplateEntity(templateId = selectedSlot, name = "Template $selectedSlot")

    // State bindings
    var name by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.name) }
    var isDefault by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.isDefault) }
    var paperWidthDots by remember(selectedSlot, currentDbTemplate) { mutableIntStateOf(currentDbTemplate.paperWidthDots) }
    var itemLayoutStyle by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.itemLayoutStyle) }

    // Header States
    var showLogo by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.showLogo) }
    var customStoreName by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customStoreName) }
    var customSubtitle by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customSubtitle) }
    var customOwnerOrContact by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customOwnerOrContact) }
    var customPhone by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customPhone) }
    var customAddress by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.customAddress) }
    var storeNameFontSize by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.storeNameFontSize) }
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
    var bodyFontSize by remember(selectedSlot, currentDbTemplate) { mutableStateOf(currentDbTemplate.bodyFontSize) }

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
        name, isDefault, paperWidthDots, itemLayoutStyle, showLogo, customStoreName, customSubtitle,
        customOwnerOrContact, customPhone, customAddress, storeNameFontSize, storeNameAlign,
        showAddress, showPhone, showHeaderNote, customHeaderNote, showCashierInfo, showVoucherNumber,
        showCustomerInfo, customerLabel, nameWeight, qtyWeight, priceWeight, amountWeight,
        rowPaddingDp, gridLineStyle, bodyFontSize, showTotalsBreakdown, showPaymentMethod,
        footerMessage, returnPolicyText, extraNoticeText, showPrintTime, footerAlign
    ) {
        ReceiptTemplateEntity(
            templateId = selectedSlot,
            name = name,
            isDefault = isDefault,
            paperWidthDots = paperWidthDots,
            itemLayoutStyle = itemLayoutStyle,
            showLogo = showLogo,
            customStoreName = customStoreName,
            customSubtitle = customSubtitle,
            customOwnerOrContact = customOwnerOrContact,
            customPhone = customPhone,
            customAddress = customAddress,
            storeNameFontSize = storeNameFontSize,
            storeNameAlign = storeNameAlign,
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
            showTotalsBreakdown = showTotalsBreakdown,
            showPaymentMethod = showPaymentMethod,
            footerMessage = footerMessage,
            returnPolicyText = returnPolicyText,
            extraNoticeText = extraNoticeText,
            showPrintTime = showPrintTime,
            footerAlign = footerAlign
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
                            text = "ပြေစာပုံစံ ဖန်တီးခန်း (Custom Template Builder)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Template 1 (နမူနာစံ) နှင့် Template 2 (မျဉ်းမပါ) စိတ်ကြိုက်ပြင်နိုင်ပါသည်",
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
                Text("ပြင်ဆင်မည့် Template ရွေးချယ်ပါ (Slots 1 to 5):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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

                                // Paper Size
                                Text("စက္ကူအကျယ် အရွယ်အစား (Paper Width):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = paperWidthDots <= 420,
                                        onClick = { paperWidthDots = 384 },
                                        label = { Text("58mm (ဘောပင်အရွယ် စံနှုန်း)", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = paperWidthDots > 420,
                                        onClick = { paperWidthDots = 576 },
                                        label = { Text("80mm (အကျယ်ကြီး)", fontSize = 11.sp) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Custom Store Name
                                OutlinedTextField(
                                    value = customStoreName,
                                    onValueChange = { customStoreName = it },
                                    label = { Text("ဆိုင်နာမည် (ဥပမာ- ဆန်းသစ်)") },
                                    placeholder = { Text(storeProfile.storeName.ifBlank { "ဆန်းသစ်" }) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Custom Subtitle
                                OutlinedTextField(
                                    value = customSubtitle,
                                    onValueChange = { customSubtitle = it },
                                    label = { Text("ဆိုင်အမျိုးအစား / ခေါင်းစဉ်ခွဲ (ဥပမာ- အိမ်ဆောက်ပစ္စည်း အရောင်းဆိုင်)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Custom Owner / Contact
                                OutlinedTextField(
                                    value = customOwnerOrContact,
                                    onValueChange = { customOwnerOrContact = it },
                                    label = { Text("ပိုင်ရှင် / တာဝန်ခံ (ဥပမာ- ကိုပါပီး+မသီရိကလျာ)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Custom Phone
                                OutlinedTextField(
                                    value = customPhone,
                                    onValueChange = { customPhone = it },
                                    label = { Text("ဖုန်းနံပါတ်များ (ဥပမာ- 09678473426/09943070451/...)") },
                                    placeholder = { Text(storeProfile.phone) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Custom Address
                                OutlinedTextField(
                                    value = customAddress,
                                    onValueChange = { customAddress = it },
                                    label = { Text("ဆိုင်လိပ်စာ (ဥပမာ- 3/3 ဗိုလ်ချုပ်လမ်း / စမ်းမြို့ရပ်ကွက် / ကလေးမြို့)") },
                                    placeholder = { Text(storeProfile.address) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Customer name inclusion & custom label
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showCustomerInfo, onCheckedChange = { showCustomerInfo = it })
                                    Text("ဈေးဝယ်သူအမည် ထည့်သွင်းဖော်ပြမည်", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                if (showCustomerInfo) {
                                    OutlinedTextField(
                                        value = customerLabel,
                                        onValueChange = { customerLabel = it },
                                        label = { Text("ဈေးဝယ်သူ ခေါင်းစဉ်စာသား (ဥပမာ- ဈေးဝယ်သူ သို့မဟုတ် ဝယ်သူအမည်)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showAddress, onCheckedChange = { showAddress = it })
                                    Text("ဆိုင်လိပ်စာ ဖော်ပြမည်", fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showPhone, onCheckedChange = { showPhone = it })
                                    Text("ဆိုင်ဖုန်းနံပါတ် ဖော်ပြမည်", fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showVoucherNumber, onCheckedChange = { showVoucherNumber = it })
                                    Text("ပြေစာအမှတ် ဖော်ပြမည်", fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showCashierInfo, onCheckedChange = { showCashierInfo = it })
                                    Text("အရောင်းတာဝန်ခံ ဖော်ပြမည်", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("ဆိုင်နာမည် စာလုံးအရွယ်အစား:", fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("SMALL" to "သေး", "MEDIUM" to "အလယ်", "LARGE" to "ကြီး", "XLARGE" to "အကြီးဆုံး").forEach { (sz, lbl) ->
                                        FilterChip(
                                            selected = storeNameFontSize == sz,
                                            onClick = { storeNameFontSize = sz },
                                            label = { Text(lbl, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
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
                                Text("📊 Body ကုန်ပစ္စည်းပြသမှု ပုံစံ (Item Layout Style)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Item Layout Style: 2-Line Sample vs 4-Column Table
                                Text("ပုံစံ အမျိုးအစား ရွေးချယ်ပါ:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = itemLayoutStyle == "STANDARD_2LINE",
                                        onClick = {
                                            itemLayoutStyle = "STANDARD_2LINE"
                                            gridLineStyle = "DASHED"
                                        },
                                        label = { Text("နမူနာ ၂ ကြောင်းစံ (Template 1)", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = itemLayoutStyle == "COLUMNS_4",
                                        onClick = {
                                            itemLayoutStyle = "COLUMNS_4"
                                            gridLineStyle = "NONE"
                                        },
                                        label = { Text("၄ ကော်လံ ဇယား (Template 2)", fontSize = 11.sp) }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("ဇယားကွက် မျဉ်းကြောင်းပုံစံ (Grid Line Style):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "NONE" to "မျဉ်းမပါ (Template 2)",
                                        "DASHED" to "အစက်မျဉ်း (- - - Template 1)",
                                        "SOLID" to "မျဉ်းဖြောင့် (───)",
                                        "DOUBLE" to "မျဉ်းနှစ်ထပ် (═══)"
                                    ).forEach { (st, lbl) ->
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

                                if (itemLayoutStyle == "COLUMNS_4") {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("၄ ကော်လံ အကျယ်အဝန်း အချိုးအစား (Column Widths):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Text("၁။ ပစ္စည်းအမည် (Name): ${(nameWeight * 100).toInt()}%", fontSize = 11.sp)
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
                                    Text("၂။ အရေအတွက် (Qty): ${(qtyWeight * 100).toInt()}% | ၃။ နှုန်း: ${(priceWeight * 100).toInt()}% | ၄။ သင့်ငွေ: ${(amountWeight * 100).toInt()}%", fontSize = 11.sp, color = Color.DarkGray)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = showPrintTime, onCheckedChange = { showPrintTime = it })
                                    Text("Print Time ထည့်သွင်းဖော်ပြမည်", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = returnPolicyText,
                                    onValueChange = { returnPolicyText = it },
                                    label = { Text("ပစ္စည်း ပြန်လဲခြင်း စည်းကမ်းချက် (ဥပမာ- ဝယ်ယူပြီး ပစ္စည်း များကို ပြန်လဲမပေးပါ)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = footerMessage,
                                    onValueChange = { footerMessage = it },
                                    label = { Text("နှုတ်ခွန်းဆက် စကား (ဥပမာ- ဝယ်ယူအားပေးမှုကိုကျေးဇူးအထူးတင်ရှိပါသည် သို့မဟုတ် ကျေးဇူးတင်ပါသည်)") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = extraNoticeText,
                                    onValueChange = { extraNoticeText = it },
                                    label = { Text("အပိုဆောင်း ကြော်ငြာ/အသိပေးချက် (ဥပမာ- သုံးမူး၊ ငါးမူး၊ ခြောက်မူး သံပင် များ ၊ ...)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
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
                                Text("စက္ကူအရွယ်: ${if (paperWidthDots <= 420) "58mm Compact" else "80mm Wide"} | ပုံစံ: ${if (itemLayoutStyle == "STANDARD_2LINE") "၂ ကြောင်းစံ" else "၄ ကော်လံ"}", fontSize = 10.5.sp, color = Color.Gray)
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
                                paperWidthDots = previewTemplate.paperWidthDots
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
