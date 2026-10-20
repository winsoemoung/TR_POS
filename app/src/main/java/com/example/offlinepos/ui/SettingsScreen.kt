package com.example.offlinepos.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlinepos.Sync.GoogleOAuthManager
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
    var currentSubpage by remember { mutableStateOf(SettingsSubpage.HUB) }

    // System Back Button returns to Settings Hub
    BackHandler(enabled = currentSubpage != SettingsSubpage.HUB) {
        currentSubpage = SettingsSubpage.HUB
    }

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
                    ModernSettingsDashboard(
                        viewModel = viewModel,
                        onNavigate = { currentSubpage = it }
                    )
                }
                SettingsSubpage.STORE -> SettingsStoreSubpage(viewModel = viewModel)
                SettingsSubpage.HARDWARE -> SettingsHardwareSubpage(viewModel = viewModel)
                SettingsSubpage.BACKUP -> SettingsBackupSubpage(viewModel = viewModel)
                SettingsSubpage.NOTIFICATIONS -> SettingsNotificationSubpage(viewModel = viewModel)
                SettingsSubpage.SERVER -> SettingsServerSubpage(viewModel = viewModel)
                SettingsSubpage.BUSINESS -> SettingsBusinessSubpage(viewModel = viewModel)
                SettingsSubpage.SECURITY -> SettingsSecuritySubpage(viewModel = viewModel)
            }
        }
    }
}

/**
 * Clean, modern, categorized Settings Screen Layout.
 * Built with Material 3 Cards, soft icon containers, and clear visual hierarchy.
 */
@Composable
private fun ModernSettingsDashboard(
    viewModel: PosViewModel,
    onNavigate: (SettingsSubpage) -> Unit,
) {
    val context = LocalContext.current
    val storeProfile by viewModel.storeProfile.collectAsState()
    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val googleAccount by viewModel.googleAccount.collectAsState()
    val isDriveSyncing by viewModel.isDriveSyncing.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val lowStockAlert by viewModel.isLowStockAlertEnabled.collectAsState()
    val passcodeLock by viewModel.isPasscodeLockEnabled.collectAsState()

    val printerConnected = ThermalPrinterManager.getConnectionType(context) != PrinterConnectionType.NONE
    val storeConfig = remember { StoreConfig(context) }
    val botActive = storeConfig.isTelegramEnabled() || storeConfig.isViberEnabled()

    // Interactive UI states for demo / quick toggles
    var cashDrawerTriggered by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_lazy_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 1. Business & Store Overview Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(SettingsSubpage.STORE) }
                    .testTag("card_store_overview"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
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
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = storeProfile.storeName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            ) {
                                Text(
                                    text = "POS Verified",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = storeProfile.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ဖုန်း: ${storeProfile.phone}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Edit Store Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // 2. Business & Store Info Category Card
        item {
            SettingCard {
                SettingSectionHeader(
                    title = "ဆိုင်အချက်အလက်နှင့် ပြေစာဒီဇိုင်း",
                    subtitle = "Business & Store Info",
                    icon = Icons.Default.Store,
                    accentColor = Color(0xFF2563EB)
                )

                SettingRowItem(
                    icon = Icons.Default.Edit,
                    iconTint = Color(0xFF2563EB),
                    iconBgColor = Color(0xFFDBEAFE),
                    title = "ဆိုင်အမည်နှင့် တည်နေရာ",
                    subtitle = "${storeProfile.storeName} • ${storeProfile.phone}",
                    onClick = { onNavigate(SettingsSubpage.STORE) },
                    actionWidget = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                )

                SettingDivider()

                SettingRowItem(
                    icon = Icons.Default.Receipt,
                    iconTint = Color(0xFF2563EB),
                    iconBgColor = Color(0xFFDBEAFE),
                    title = "ပြေစာ Header & Footer စာသားများ",
                    subtitle = storeProfile.headerNote,
                    onClick = { onNavigate(SettingsSubpage.STORE) },
                    actionWidget = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                )
            }
        }

        // 3. Sales & Inventory Preferences Category Card
        item {
            SettingCard {
                SettingSectionHeader(
                    title = "အရောင်းနှင့် ကုန်ပစ္စည်း သတ်မှတ်ချက်များ",
                    subtitle = "Sales & Inventory Preferences",
                    icon = Icons.Default.PointOfSale,
                    accentColor = Color(0xFF059669)
                )

                // Currency Symbol Selector
                SettingRowItem(
                    icon = Icons.Default.CurrencyExchange,
                    iconTint = Color(0xFF059669),
                    iconBgColor = Color(0xFFD1FAE5),
                    title = "ငွေကြေး သင်္ကေတ (Currency Symbol)",
                    subtitle = "လက်ရှိ သုံးစွဲနေသော ငွေကြေး: $currencySymbol",
                    actionWidget = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Ks", "MMK", "$").forEach { sym ->
                                FilterChip(
                                    selected = currencySymbol == sym,
                                    onClick = { viewModel.setCurrencySymbol(sym) },
                                    label = { Text(sym, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                )

                SettingDivider()

                // Low Stock Alert Toggle
                SettingRowItem(
                    icon = Icons.Default.Inventory2,
                    iconTint = Color(0xFF059669),
                    iconBgColor = Color(0xFFD1FAE5),
                    title = "ကုန်ပစ္စည်း လက်ကျန်နည်း သတိပေးချက်",
                    subtitle = if (lowStockAlert) "လက်ကျန်နည်းပါက အရောင်း tab တွင် အနီရောင်ပြပါမည်" else "သတိပေးချက် ပိတ်ထားပါသည်",
                    actionWidget = {
                        Switch(
                            checked = lowStockAlert,
                            onCheckedChange = { viewModel.setLowStockAlertEnabled(it) }
                        )
                    }
                )

                SettingDivider()

                // VIP Promo Codes & Business Rules
                SettingRowItem(
                    icon = Icons.Default.ConfirmationNumber,
                    iconTint = Color(0xFF059669),
                    iconBgColor = Color(0xFFD1FAE5),
                    title = "VIP Promo Codes & လျှော့ဈေး သတ်မှတ်ချက်",
                    subtitle = "787VIP (10%) • WELCOME (2,000 Ks)",
                    onClick = { onNavigate(SettingsSubpage.BUSINESS) },
                    actionWidget = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                )
            }
        }

        // 4. Hardware Configuration Category Card
        item {
            SettingCard {
                SettingSectionHeader(
                    title = "ပရင့်တာနှင့် စက်ကိရိယာများ",
                    subtitle = "Hardware Configuration",
                    icon = Icons.Default.Print,
                    accentColor = Color(0xFFB45309)
                )

                // Receipt Thermal Printer Connection
                SettingRowItem(
                    icon = Icons.Default.Print,
                    iconTint = Color(0xFFB45309),
                    iconBgColor = Color(0xFFFEF3C7),
                    title = "Thermal Receipt Printer",
                    subtitle = "Bluetooth & USB ပရင့်တာ (80mm/58mm)",
                    onClick = { onNavigate(SettingsSubpage.HARDWARE) },
                    actionWidget = {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (printerConnected) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        ) {
                            Text(
                                text = if (printerConnected) "✓ ချိတ်ဆက်ပြီး" else "ချိတ်ဆက်ရန်",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (printerConnected) Color(0xFF15803D) else Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                )

                SettingDivider()

                // Barcode Scanner Status
                SettingRowItem(
                    icon = Icons.Default.QrCodeScanner,
                    iconTint = Color(0xFFB45309),
                    iconBgColor = Color(0xFFFEF3C7),
                    title = "Barcode / QR Scanner",
                    subtitle = "Camera Scanner & USB/Bluetooth Gun Reader အသင့်ရှိပါသည်",
                    actionWidget = {
                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                            Text("Ready", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                )

                SettingDivider()

                // Cash Drawer Trigger
                SettingRowItem(
                    icon = Icons.Default.PointOfSale,
                    iconTint = Color(0xFFB45309),
                    iconBgColor = Color(0xFFFEF3C7),
                    title = "ငွေသိမ်းသေတ္တာ (Cash Drawer Auto-kick)",
                    subtitle = "ဘေလ်ဖြတ်ပြီးပါက သေတ္တာအလိုအလျောက်ပွင့်ခြင်း",
                    actionWidget = {
                        Switch(
                            checked = cashDrawerTriggered,
                            onCheckedChange = {
                                cashDrawerTriggered = it
                                viewModel.setBanner(if (it) "Cash Drawer Auto-open ဖွင့်ထားပါသည်" else "Cash Drawer ပိတ်ထားပါသည်")
                            }
                        )
                    }
                )
            }
        }

        // 5. Data Management Category Card
        item {
            SettingCard {
                SettingSectionHeader(
                    title = "ဒေတာ အရန်သိမ်းခြင်းနှင့် Cloud Sync",
                    subtitle = "Data Management",
                    icon = Icons.Default.CloudSync,
                    accentColor = Color(0xFF7C3AED)
                )

                // Google Drive Sync with OAuth
                SettingRowItem(
                    icon = Icons.Default.CloudDone,
                    iconTint = Color(0xFF7C3AED),
                    iconBgColor = Color(0xFFEDE9FE),
                    title = "Google Drive Auto-Sync (pos-sync-4)",
                    subtitle = if (!googleAccount.isNullOrBlank()) "$googleAccount • OAuth 2.0 Active" else "Google Account ချိတ်ဆက်ပြီး အလိုအလျောက် sync မည်",
                    onClick = { onNavigate(SettingsSubpage.BACKUP) },
                    actionWidget = {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                isDriveSyncing -> Color(0xFFDBEAFE)
                                !googleAccount.isNullOrBlank() -> Color(0xFFDCFCE7)
                                else -> Color(0xFFFEF3C7)
                            }
                        ) {
                            Text(
                                text = when {
                                    isDriveSyncing -> "⚡ Syncing"
                                    !googleAccount.isNullOrBlank() -> "✓ Connected"
                                    else -> "ချိတ်ရန်"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isDriveSyncing -> Color(0xFF2563EB)
                                    !googleAccount.isNullOrBlank() -> Color(0xFF15803D)
                                    else -> Color(0xFFD97706)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                )

                SettingDivider()

                // Local Database Backup & Restore (Full receipts history included)
                SettingRowItem(
                    icon = Icons.Default.Save,
                    iconTint = Color(0xFF7C3AED),
                    iconBgColor = Color(0xFFEDE9FE),
                    title = "Local Database Backup & Restore",
                    subtitle = "ဖွင့်ပြီးသမျှ ဘောင်ချာမှတ်တမ်း (Receipts) နှင့် ပစ္စည်းစာရင်းအားလုံး ပါဝင်ပါသည်",
                    onClick = { onNavigate(SettingsSubpage.BACKUP) },
                    actionWidget = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                )
            }
        }

        // 6. App Appearance & Security Category Card
        item {
            SettingCard {
                SettingSectionHeader(
                    title = "ဒီဇိုင်းနှင့် စနစ်လုံခြုံရေး",
                    subtitle = "App Appearance & Security",
                    icon = Icons.Default.Security,
                    accentColor = Color(0xFF4F46E5)
                )

                // Dark / Light Mode Segmented Toggle (Persisted in SharedPreferences)
                SettingRowItem(
                    icon = Icons.Default.Brightness4,
                    iconTint = Color(0xFF4F46E5),
                    iconBgColor = Color(0xFFE0E7FF),
                    title = "အလင်း / အမှောင် ဒီဇိုင်း (Dark/Light Theme)",
                    subtitle = "လက်ရှိ ရွေးချယ်ထားမှု: ${themeMode.label}",
                    actionWidget = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            StoreConfig.ThemeMode.values().forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(mode.icon, fontSize = 13.sp) }
                                )
                            }
                        }
                    }
                )

                SettingDivider()

                // Passcode / PIN Lock Toggle
                SettingRowItem(
                    icon = Icons.Default.Lock,
                    iconTint = Color(0xFF4F46E5),
                    iconBgColor = Color(0xFFE0E7FF),
                    title = "Passcode / PIN Lock စနစ်",
                    subtitle = if (passcodeLock) "PIN: 1234 ဖြင့် လုံခြုံရေးကာကွယ်ထားပါသည်" else "စနစ်ဖွင့်ချိန်တွင် PIN တောင်းဆိုမှု ပိတ်ထားပါသည်",
                    onClick = { onNavigate(SettingsSubpage.SECURITY) },
                    actionWidget = {
                        Switch(
                            checked = passcodeLock,
                            onCheckedChange = { viewModel.setPasscodeLockEnabled(it) }
                        )
                    }
                )
            }
        }

        // 7. Automated Notifications & Local Wi-Fi Server Quick Tiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Telegram & Viber Bot Tile
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(SettingsSubpage.NOTIFICATIONS) }
                        .testTag("tile_quick_notifications"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                }
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = if (botActive) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)) {
                                Text(if (botActive) "Active" else "Setup", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (botActive) Color(0xFF15803D) else Color(0xFFD97706), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Telegram / Viber Bot", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("နေ့ချုပ်ရောင်းအား အလိုအလျောက် သတင်းပို့စနစ်", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }

                // Wi-Fi Staff Server Tile
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(SettingsSubpage.SERVER) }
                        .testTag("tile_quick_server"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = Color(0xFFECFDF5), modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                                }
                            }
                            Surface(shape = RoundedCornerShape(4.dp), color = if (isServerRunning) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)) {
                                Text(if (isServerRunning) "ON" else "OFF", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isServerRunning) Color(0xFF15803D) else Color(0xFFB91C1C), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Wi-Fi Staff Server", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("ဝန်ထမ်းဖုန်းများမှ Web Portal ဖြင့် ချိတ်ဆက်ခြင်း", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * Reusable Card Container for Settings Grouping.
 */
@Composable
fun SettingCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            content = content
        )
    }
}

/**
 * Reusable Section Header inside a Category Card.
 */
@Composable
fun SettingSectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        action?.invoke()
    }
}

/**
 * Reusable Row Item inside a Category Card.
 */
@Composable
fun SettingRowItem(
    icon: ImageVector,
    iconTint: Color,
    iconBgColor: Color,
    title: String,
    subtitle: String? = null,
    actionWidget: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
) {
    val clickableModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp)
    }

    Row(
        modifier = clickableModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconBgColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        actionWidget()
    }
}

/**
 * Clean Divider between Setting Rows.
 */
@Composable
fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}
