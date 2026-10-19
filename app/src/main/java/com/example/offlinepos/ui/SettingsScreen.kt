package com.example.offlinepos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlinepos.hardware.PrinterConnectionType
import com.example.offlinepos.hardware.ThermalPrinterManager

/**
 * Settings Sub-page Categories for clean, decluttered navigation.
 */
enum class SettingsSubpage(val title: String, val icon: String) {
    HUB("ဆက်တင် ပင်မစာမျက်နှာ", "⚙️"),
    STORE("ဆိုင်အချက်အလက် & ပြေစာ", "🏪"),
    HARDWARE("ပရင့်တာနှင့် စက်ကိရိယာများ", "🖨️"),
    BACKUP("ဒေတာ အရန်သိမ်းခြင်း & Sync", "☁️"),
    NOTIFICATIONS("Telegram & Viber သတင်းပို့", "🤖"),
    SERVER("ဝန်ထမ်းချိတ်ဆက် ဆာဗာ (Wi-Fi)", "🌐"),
    BUSINESS("အရောင်းနှင့် လုပ်ငန်း သတ်မှတ်ချက်", "💼"),
    SECURITY("အကောင့်နှင့် လုံခြုံရေး", "🔒"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: PosViewModel,
) {
    val context = LocalContext.current
    var currentSubpage by remember { mutableStateOf(SettingsSubpage.HUB) }

    // System Back Button returns to Settings Hub
    BackHandler(enabled = currentSubpage != SettingsSubpage.HUB) {
        currentSubpage = SettingsSubpage.HUB
    }

    val storeProfile by viewModel.storeProfile.collectAsState()
    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val googleAccount by viewModel.googleAccount.collectAsState()
    val isDriveSyncing by viewModel.isDriveSyncing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentSubpage != SettingsSubpage.HUB) {
                            Text(currentSubpage.icon)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (currentSubpage == SettingsSubpage.HUB) "စနစ် သတ်မှတ်ချက်များ (Settings)" else currentSubpage.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                    }
                },
                navigationIcon = {
                    if (currentSubpage != SettingsSubpage.HUB) {
                        IconButton(
                            onClick = { currentSubpage = SettingsSubpage.HUB },
                            modifier = Modifier.testTag("settings_back_to_hub")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings Hub")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Quick Horizontal Sub-page Chips for fast hopping
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(SettingsSubpage.values()) { page ->
                    val isSelected = currentSubpage == page
                    FilterChip(
                        selected = isSelected,
                        onClick = { currentSubpage = page },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(page.icon, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (page == SettingsSubpage.HUB) "အားလုံး (Hub)" else page.title.take(12), fontSize = 11.sp)
                            }
                        },
                        modifier = Modifier.testTag("settings_tab_${page.name.lowercase()}")
                    )
                }
            }

            // Render selected Sub-page
            when (currentSubpage) {
                SettingsSubpage.HUB -> {
                    SettingsMainHub(
                        viewModel = viewModel,
                        onNavigate = { currentSubpage = it }
                    )
                }
                SettingsSubpage.STORE -> {
                    SettingsStoreSubpage(viewModel = viewModel)
                }
                SettingsSubpage.HARDWARE -> {
                    SettingsHardwareSubpage(viewModel = viewModel)
                }
                SettingsSubpage.BACKUP -> {
                    SettingsBackupSubpage(viewModel = viewModel)
                }
                SettingsSubpage.NOTIFICATIONS -> {
                    SettingsNotificationSubpage(viewModel = viewModel)
                }
                SettingsSubpage.SERVER -> {
                    SettingsServerSubpage(viewModel = viewModel)
                }
                SettingsSubpage.BUSINESS -> {
                    SettingsBusinessSubpage(viewModel = viewModel)
                }
                SettingsSubpage.SECURITY -> {
                    SettingsSecuritySubpage(viewModel = viewModel)
                }
            }
        }
    }
}

/**
 * Clean, organized Settings Main Hub Menu.
 */
@Composable
private fun SettingsMainHub(
    viewModel: PosViewModel,
    onNavigate: (SettingsSubpage) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val googleAccount by viewModel.googleAccount.collectAsState()
    val isDriveSyncing by viewModel.isDriveSyncing.collectAsState()

    val printerConnected = ThermalPrinterManager.getConnectionType(context) != PrinterConnectionType.NONE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Store Manager Overview Header
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
                StoreLogoAvatar(
                    customLogoPath = storeProfile.customLogoPath,
                    contentDescription = "Store Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    onClick = { onNavigate(SettingsSubpage.STORE) }
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = storeProfile.storeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "ဆိုင်အချက်အလက်၊ ပရင့်တာ၊ Cloud နှင့် လုံခြုံရေး သတ်မှတ်ချက်များ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Text(
            text = "ဆက်တင် ကဏ္ဍများ (Settings Categories)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        // 1. Store Profile & Receipts Subpage Tile
        SettingsCategoryTile(
            icon = "🏪",
            title = "ဆိုင်အချက်အလက်နှင့် ပြေစာဒီဇိုင်း",
            subtitle = "ဆိုင်အမည်၊ လိပ်စာ၊ ဖုန်း၊ Logo တံဆိပ်၊ ပြေစာ Header/Footer နှင့် စာလုံးစတိုင်များ",
            badgeText = null,
            badgeColor = null,
            onClick = { onNavigate(SettingsSubpage.STORE) },
            testTag = "tile_settings_store"
        )

        // 2. Hardware & Thermal Printers Subpage Tile
        SettingsCategoryTile(
            icon = "🖨️",
            title = "ပရင့်တာနှင့် စက်ကိရိယာများ",
            subtitle = "Thermal Printer (Bluetooth & USB)၊ Auto-Print ဘေလ်ထုတ်စနစ်၊ 80mm/58mm စက္ကူနှင့် စမ်းသပ်ပရင့်",
            badgeText = if (printerConnected) "✓ Connected" else "Setup",
            badgeColor = if (printerConnected) Color(0xFF15803D) else Color(0xFFD97706),
            onClick = { onNavigate(SettingsSubpage.HARDWARE) },
            testTag = "tile_settings_hardware"
        )

        // 3. Backup & Real-time Cloud Sync Subpage Tile
        SettingsCategoryTile(
            icon = "☁️",
            title = "ဒေတာ အရန်သိမ်းခြင်းနှင့် Cloud Sync",
            subtitle = "Real-time Google Drive Auto-sync၊ Two-way Auto-merge စနစ်၊ Database Backup & Restore",
            badgeText = when {
                isDriveSyncing -> "⚡ Syncing"
                googleAccount != null -> "✓ Real-time Sync"
                else -> "အကောင့်ချိတ်ရန်"
            },
            badgeColor = when {
                isDriveSyncing -> Color(0xFF2563EB)
                googleAccount != null -> Color(0xFF15803D)
                else -> Color(0xFFD97706)
            },
            onClick = { onNavigate(SettingsSubpage.BACKUP) },
            testTag = "tile_settings_backup"
        )

        // 4. Automated Telegram & Viber Bot Notification Subpage Tile
        val storeConfig = remember { StoreConfig(context) }
        val botActive = storeConfig.isTelegramEnabled() || storeConfig.isViberEnabled()
        SettingsCategoryTile(
            icon = "🤖",
            title = "အလိုအလျောက် သတင်းပို့စနစ် (Telegram & Viber)",
            subtitle = "နေ့စဉ် ဆိုင်ပိတ်ချိန်/နေ့ချုပ်တွင် ရောင်းအား၊ အမြတ်ငွေနှင့် ကုန်ပစ္စည်းလက်ကျန်နည်းစာရင်းကို Bot သို့ အလိုအလျောက် ပို့ပေးခြင်း",
            badgeText = if (botActive) "✓ Bot Active" else "Setup",
            badgeColor = if (botActive) Color(0xFF15803D) else Color(0xFFD97706),
            onClick = { onNavigate(SettingsSubpage.NOTIFICATIONS) },
            testTag = "tile_settings_notifications"
        )

        // 5. Local POS Wi-Fi Staff Server Subpage Tile
        SettingsCategoryTile(
            icon = "🌐",
            title = "ဝန်ထမ်းချိတ်ဆက် ဆာဗာ (Wi-Fi Server)",
            subtitle = "Direct HTML5 Web Portal၊ QR Code ဖြင့် ဖုန်းမှ ကုန်ပစ္စည်းလှမ်းထည့်ခြင်းနှင့် Staff Sync",
            badgeText = if (isServerRunning) "🟢 Server ON" else "🔴 OFF",
            badgeColor = if (isServerRunning) Color(0xFF15803D) else Color(0xFFB91C1C),
            onClick = { onNavigate(SettingsSubpage.SERVER) },
            testTag = "tile_settings_server"
        )

        // 5. Business & Promo Codes Subpage Tile
        SettingsCategoryTile(
            icon = "💼",
            title = "အရောင်းနှင့် လုပ်ငန်း သတ်မှတ်ချက်များ",
            subtitle = "Candlestick အရောင်းဇယား၊ VIP Promo Codes (787VIP)၊ ဝယ်သူ & အကြွေး၊ နေ့ချုပ် Z-Report",
            badgeText = null,
            badgeColor = null,
            onClick = { onNavigate(SettingsSubpage.BUSINESS) },
            testTag = "tile_settings_business"
        )

        // 6. Security & Audit Log Subpage Tile
        SettingsCategoryTile(
            icon = "🔒",
            title = "အကောင့်၊ လုံခြုံရေးနှင့် စနစ်အချက်အလက်",
            subtitle = "AI Superuser အခွင့်အရေး၊ Audit Log စနစ်မှတ်တမ်းများ၊ Terminal နည်းပညာ သတ်မှတ်ချက်",
            badgeText = if (viewModel.isAiSuperuserGranted()) "AI Superuser" else null,
            badgeColor = Color(0xFF15803D),
            onClick = { onNavigate(SettingsSubpage.SECURITY) },
            testTag = "tile_settings_security"
        )

        Spacer(modifier = Modifier.height(64.dp))
    }
}

/**
 * Visual Navigation Card Tile for each Sub-page.
 */
@Composable
private fun SettingsCategoryTile(
    icon: String,
    title: String,
    subtitle: String,
    badgeText: String?,
    badgeColor: Color?,
    onClick: () -> Unit,
    testTag: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(icon, fontSize = 22.sp)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    badgeText?.let {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = (badgeColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
                            modifier = Modifier.padding(start = 6.dp)
                        ) {
                            Text(
                                text = it,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp,
                    maxLines = 2,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
