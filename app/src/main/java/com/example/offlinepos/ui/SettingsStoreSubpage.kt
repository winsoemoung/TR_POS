package com.example.offlinepos.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsStoreSubpage(
    viewModel: PosViewModel,
) {
    val storeProfile by viewModel.storeProfile.collectAsState()
    val scrollState = rememberScrollState()

    var storeName by remember(storeProfile) { mutableStateOf(storeProfile.storeName) }
    var address by remember(storeProfile) { mutableStateOf(storeProfile.address) }
    var phone by remember(storeProfile) { mutableStateOf(storeProfile.phone) }
    var headerNote by remember(storeProfile) { mutableStateOf(storeProfile.headerNote) }
    var footerMessage by remember(storeProfile) { mutableStateOf(storeProfile.footerMessage) }
    var backupSchedule by remember(storeProfile) { mutableStateOf(storeProfile.autoBackupSchedule) }
    var headerStyle by remember(storeProfile) { mutableStateOf(storeProfile.receiptHeaderStyle) }
    var headerSize by remember(storeProfile) { mutableStateOf(storeProfile.receiptHeaderSize) }
    var headerAlign by remember(storeProfile) { mutableStateOf(storeProfile.receiptHeaderAlign) }
    var footerStyle by remember(storeProfile) { mutableStateOf(storeProfile.receiptFooterStyle) }
    var footerSize by remember(storeProfile) { mutableStateOf(storeProfile.receiptFooterSize) }
    var footerAlign by remember(storeProfile) { mutableStateOf(storeProfile.receiptFooterAlign) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.saveStoreLogo(uri) { success, msg ->
                viewModel.setBanner(msg)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Store Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    StoreLogoAvatar(
                        customLogoPath = storeProfile.customLogoPath,
                        contentDescription = "Store Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        onClick = {
                            try {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } catch (e: Exception) {
                                viewModel.setBanner("ပုံရွေးချယ်မှု မအောင်မြင်ပါ: ${e.localizedMessage}")
                            }
                        }
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                        shadowElevation = 2.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📷", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = storeProfile.storeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "ဆိုင်ရှင် / မန်နေဂျာ (Store Profile)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Logo ပုံကို နှိပ်၍ ဆိုင်တံဆိပ် အသစ် ပြောင်းလဲနိုင်ပါသည်",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!storeProfile.customLogoPath.isNullOrBlank()) {
                        androidx.compose.material3.TextButton(
                            onClick = { viewModel.resetStoreLogo() },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(28.dp),
                        ) {
                            Text("🗑️ Mūla Logo သို့ ပြန်ထားမည်", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Store Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🏪 ဆိုင်အချက်အလက်များ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("ဆိုင်အမည် (Store Name)") },
                    modifier = Modifier.fillMaxWidth().testTag("store_name_field"),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("ဖုန်းနံပါတ် (Phone Number)") },
                    modifier = Modifier.fillMaxWidth().testTag("store_phone_field"),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("လိပ်စာ (Address)") },
                    modifier = Modifier.fillMaxWidth().testTag("store_address_field"),
                    maxLines = 2,
                )
            }
        }

        // Receipt Customization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🧾 ပြေစာ ပုံစံနှင့် ဒီဇိုင်း သတ်မှတ်ချက်", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = headerNote,
                    onValueChange = { headerNote = it },
                    label = { Text("ပြေစာ ထိပ်စီးမှတ်ချက် (Header Note)") },
                    modifier = Modifier.fillMaxWidth().testTag("store_header_field"),
                    placeholder = { Text("မင်္ဂလာပါ၊ ပြန်လည်ကြိုဆိုပါသည်") },
                )

                ReceiptStyleSelectorHelper(
                    label = "ထိပ်စီးစာသား အနေအထား (Header Alignment):",
                    selected = headerAlign,
                    options = listOf("CENTER" to "အလယ်", "LEFT" to "ဘယ်", "RIGHT" to "ညာ"),
                    onSelected = { headerAlign = it },
                )

                ReceiptStyleSelectorHelper(
                    label = "ထိပ်စီးစာလုံး အရွယ်အစား (Header Font Size):",
                    selected = headerSize,
                    options = listOf("NORMAL" to "ပုံမှန်", "LARGE" to "ကြီး", "SMALL" to "သေး"),
                    onSelected = { headerSize = it },
                )

                ReceiptStyleSelectorHelper(
                    label = "ထိပ်စီးစာလုံး ပုံစံ (Header Style):",
                    selected = headerStyle,
                    options = listOf("BOLD" to "စာလုံးမည်း", "NORMAL" to "သာမန်", "DOUBLE_WIDTH" to "မျက်နှာပြည့်"),
                    onSelected = { headerStyle = it },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                OutlinedTextField(
                    value = footerMessage,
                    onValueChange = { footerMessage = it },
                    label = { Text("ပြေစာ အောက်ခြေမှတ်ချက် (Footer Message)") },
                    modifier = Modifier.fillMaxWidth().testTag("store_footer_field"),
                    placeholder = { Text("ဝယ်ယူအားပေးမှုကို အထူးကျေးဇူးတင်ပါသည်") },
                )

                ReceiptStyleSelectorHelper(
                    label = "အောက်ခြေစာသား အနေအထား (Footer Alignment):",
                    selected = footerAlign,
                    options = listOf("CENTER" to "အလယ်", "LEFT" to "ဘယ်", "RIGHT" to "ညာ"),
                    onSelected = { footerAlign = it },
                )

                ReceiptStyleSelectorHelper(
                    label = "အောက်ခြေစာလုံး အရွယ်အစား (Footer Font Size):",
                    selected = footerSize,
                    options = listOf("SMALL" to "သေး", "NORMAL" to "ပုံမှန်", "LARGE" to "ကြီး"),
                    onSelected = { footerSize = it },
                )

                ReceiptStyleSelectorHelper(
                    label = "အောက်ခြေစာလုံး ပုံစံ (Footer Style):",
                    selected = footerStyle,
                    options = listOf("NORMAL" to "သာမန်", "BOLD" to "စာလုံးမည်း"),
                    onSelected = { footerStyle = it },
                )
            }
        }

        // Live Receipt Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = when (headerAlign) {
                    "LEFT" -> Alignment.Start
                    "RIGHT" -> Alignment.End
                    else -> Alignment.CenterHorizontally
                }
            ) {
                Text(
                    text = "📄 ပြေစာနမူနာ အစမ်းကြည့် (Live Preview)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = storeName.ifBlank { "ဆိုင်အမည်" },
                    fontWeight = if (headerStyle == "BOLD") FontWeight.Bold else FontWeight.Normal,
                    fontSize = if (headerSize == "LARGE") 16.sp else if (headerSize == "SMALL") 12.sp else 14.sp,
                )
                if (phone.isNotBlank()) {
                    Text(text = "ဖုန်း: $phone", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (address.isNotBlank()) {
                    Text(text = address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (headerNote.isNotBlank()) {
                    Text(text = headerNote, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "--------------------------------", fontSize = 10.sp, color = Color.Gray)
                Text(text = "၁ x ပစ္စည်းအမည်              ၅,၀၀၀ Ks", fontSize = 11.sp)
                Text(text = "--------------------------------", fontSize = 10.sp, color = Color.Gray)
                Text(text = "စုစုပေါင်း:                   ၅,၀၀၀ Ks", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                if (footerMessage.isNotBlank()) {
                    Text(
                        text = footerMessage,
                        fontSize = if (footerSize == "LARGE") 12.sp else if (footerSize == "SMALL") 9.sp else 10.sp,
                        fontWeight = if (footerStyle == "BOLD") FontWeight.Bold else FontWeight.Normal,
                        textAlign = when (footerAlign) {
                            "LEFT" -> TextAlign.Start
                            "RIGHT" -> TextAlign.End
                            else -> TextAlign.Center
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                viewModel.updateStoreProfile(
                    storeProfile.copy(
                        storeName = storeName.trim(),
                        address = address.trim(),
                        phone = phone.trim(),
                        headerNote = headerNote.trim(),
                        footerMessage = footerMessage.trim(),
                        autoBackupSchedule = backupSchedule,
                        receiptHeaderStyle = headerStyle,
                        receiptHeaderSize = headerSize,
                        receiptHeaderAlign = headerAlign,
                        receiptFooterStyle = footerStyle,
                        receiptFooterSize = footerSize,
                        receiptFooterAlign = footerAlign,
                    )
                )
                viewModel.setBanner("ဆိုင်အချက်အလက်နှင့် ပြေစာဒီဇိုင်းကို အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ")
            },
            modifier = Modifier.fillMaxWidth().testTag("save_store_profile_button"),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text("💾 ဆိုင်အချက်အလက် သိမ်းဆည်းမည်", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun ReceiptStyleSelectorHelper(
    label: String,
    selected: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(options) { (value, title) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(title, fontSize = 11.sp) },
                )
            }
        }
    }
}
