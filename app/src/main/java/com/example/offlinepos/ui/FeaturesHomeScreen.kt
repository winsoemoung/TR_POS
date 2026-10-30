package com.example.offlinepos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlinepos.data.local.ProductEntity
import com.example.offlinepos.hardware.PrinterConnectionType
import com.example.offlinepos.hardware.ThermalPrinterManager

/**
 * AIMS POS Features Hub Screen (features_home_screen.dart)
 * Provides centralized 1-tap access to all POS modules:
 * Products, Stock-In, AI Stock Prediction, Customers & Debt,
 * Shift Report, Thermal Printer Setup, Wi-Fi Server, and DB Maintenance.
 */
import androidx.compose.material3.LinearProgressIndicator

data class AimFeatureTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTint: Color = Color.White,
    val badgeText: String? = null,
    val badgeColor: Color = Color(0xFFDC2626),
    val showProgressBar: Boolean = false,
    val onClick: () -> Unit,
)

data class AimFeatureGroup(
    val groupTitle: String,
    val tiles: List<AimFeatureTile>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesHomeScreen(
    viewModel: PosViewModel,
    onNavigateToInventory: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAiCopilot: () -> Unit = {},
) {
    val context = LocalContext.current
    val storeProfile by viewModel.storeProfile.collectAsState()
    val actor by viewModel.actor.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val predictions by viewModel.stockPredictions.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val googleAccount by viewModel.googleAccount.collectAsState()
    val outboxPendingCount by viewModel.outboxPendingCount.collectAsState()

    var showCustomerDialog by remember { mutableStateOf(false) }
    var showShiftReportDialog by remember { mutableStateOf(false) }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var showServerDialog by remember { mutableStateOf(false) }
    var showStockPredictionDialog by remember { mutableStateOf(false) }
    var showStockInDialog by remember { mutableStateOf(false) }
    var stockInPreselectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var stockInSuggestedQty by remember { mutableStateOf(10L) }

    val urgentPredictions = predictions.filter { (it.daysRemaining <= 7 && it.currentStock > 0) || it.currentStock <= 0 }
    val urgentCount = urgentPredictions.size
    val debtorsCount = customers.count { it.currentDebtMinor > 0 }
    val totalDebtAmount = customers.sumOf { it.currentDebtMinor }

    val featureGroups = remember(urgentCount, debtorsCount, isServerRunning, googleAccount, allProducts.size, outboxPendingCount) {
        listOf(
            AimFeatureGroup(
                groupTitle = "📦 ကုန်ပစ္စည်းနှင့် စတော့ (Products & Inventory)",
                tiles = listOf(
                    AimFeatureTile(
                        id = "feat_products",
                        title = "ကုန်ပစ္စည်းစာရင်း",
                        subtitle = "${allProducts.size} မျိုး ရှိသည်",
                        icon = Icons.Default.Inventory,
                        iconBgColor = Color(0xFF10315A),
                        onClick = onNavigateToInventory,
                    ),
                    AimFeatureTile(
                        id = "feat_stock_in",
                        title = "အဝင်စတော့ သွင်းမည်",
                        subtitle = "Supplier & Invoice",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconBgColor = Color(0xFF16A34A),
                        onClick = {
                            stockInPreselectedProduct = null
                            stockInSuggestedQty = 10L
                            showStockInDialog = true
                        },
                    ),
                    AimFeatureTile(
                        id = "feat_prediction",
                        title = "AI စတော့ ခန့်မှန်းချက်",
                        subtitle = if (urgentCount > 0) "$urgentCount မျိုး ပြတ်လပ်နိုင်" else "ပုံမှန်လည်ပတ်နေသည်",
                        icon = Icons.Default.AutoAwesome,
                        iconBgColor = if (urgentCount > 0) Color(0xFFEA580C) else Color(0xFF4338CA),
                        badgeText = if (urgentCount > 0) "$urgentCount" else null,
                        badgeColor = Color(0xFFDC2626),
                        onClick = { showStockPredictionDialog = true },
                    ),
                    AimFeatureTile(
                        id = "feat_ai_assistant",
                        title = "AI အကူ (Gemini Copilot)",
                        subtitle = "POS စီမံခန့်ခွဲမှု & အမေးအဖြေ",
                        icon = Icons.Default.AutoAwesome,
                        iconBgColor = Color(0xFF4F46E5),
                        onClick = onNavigateToAiCopilot,
                    ),
                    AimFeatureTile(
                        id = "feat_categories",
                        title = "အုပ်စုနှင့် ယူနစ်များ",
                        subtitle = "လက်လီ / လက်ကား စျေး",
                        icon = Icons.Default.Category,
                        iconBgColor = Color(0xFF0284C7),
                        onClick = onNavigateToInventory,
                    ),
                )
            ),
            AimFeatureGroup(
                groupTitle = "👥 ဝယ်သူနှင့် အကြွေးစာရင်း (CRM & Credit)",
                tiles = listOf(
                    AimFeatureTile(
                        id = "feat_customers",
                        title = "ဝယ်သူ စာရင်းများ",
                        subtitle = "${customers.size} ဦး စာရင်းသွင်းထား",
                        icon = Icons.Default.People,
                        iconBgColor = Color(0xFF7C3AED),
                        onClick = { showCustomerDialog = true },
                    ),
                    AimFeatureTile(
                        id = "feat_debt_ledger",
                        title = "အကြွေးစာရင်းနှင့် ပြန်ဆပ်ငွေ",
                        subtitle = if (totalDebtAmount > 0) "%,d Ks လက်ကျန်".format(totalDebtAmount) else "အကြွေးမရှိပါ",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconBgColor = Color(0xFFDC2626),
                        badgeText = if (debtorsCount > 0) "$debtorsCount ဦး" else null,
                        badgeColor = Color(0xFFB91C1C),
                        onClick = { showCustomerDialog = true },
                    ),
                )
            ),
            AimFeatureGroup(
                groupTitle = "📊 အရောင်းမှတ်တမ်းနှင့် အစီရင်ခံစာ (Sales & Reports)",
                tiles = listOf(
                    AimFeatureTile(
                        id = "feat_receipts",
                        title = "ဘောက်ချာမှတ်တမ်း",
                        subtitle = "အရောင်းစာရင်းချုပ် & ပြက္ခဒိန်",
                        icon = Icons.Default.Receipt,
                        iconBgColor = Color(0xFF0F766E),
                        onClick = onNavigateToSales,
                    ),
                    AimFeatureTile(
                        id = "feat_shift_closing",
                        title = "နေ့ချုပ် ရှင်းတမ်း (Z-Report)",
                        subtitle = "ငွေသားနှင့် စာရင်းချုပ်",
                        icon = Icons.Default.Assessment,
                        iconBgColor = Color(0xFFB45309),
                        onClick = { showShiftReportDialog = true },
                    ),
                )
            ),
            AimFeatureGroup(
                groupTitle = "⚙️ စနစ်ဆက်တင်နှင့် ချိတ်ဆက်မှုများ (Hardware & System)",
                tiles = listOf(
                    AimFeatureTile(
                        id = "feat_local_db_sync",
                        title = "Local Database Sync",
                        subtitle = if (outboxPendingCount > 0) "Pending Queue: $outboxPendingCount" else "✓ Local DB Synced",
                        icon = Icons.Default.CloudSync,
                        iconBgColor = if (outboxPendingCount > 0) Color(0xFFD97706) else Color(0xFF15803D),
                        badgeText = if (outboxPendingCount > 0) "$outboxPendingCount pending" else null,
                        badgeColor = if (outboxPendingCount > 0) Color(0xFFB45309) else Color(0xFF15803D),
                        showProgressBar = outboxPendingCount > 0,
                        onClick = onNavigateToSettings,
                    ),
                    AimFeatureTile(
                        id = "feat_printer",
                        title = "Thermal Printer ချိတ်မည်",
                        subtitle = "Bluetooth & USB 58/80mm",
                        icon = Icons.Default.Print,
                        iconBgColor = Color(0xFF1E3A8A),
                        onClick = { showPrinterDialog = true },
                    ),
                    AimFeatureTile(
                        id = "feat_server",
                        title = "Staff Client ဆာဗာ",
                        subtitle = if (isServerRunning) "🟢 ဆာဗာ အလုပ်လုပ်နေသည်" else "⚪ ပိတ်ထားပါသည်",
                        icon = Icons.Default.Wifi,
                        iconBgColor = if (isServerRunning) Color(0xFF15803D) else Color(0xFF64748B),
                        onClick = { showServerDialog = true },
                    ),
                    AimFeatureTile(
                        id = "feat_vacuum",
                        title = "Database Optimize",
                        subtitle = "နေရာလွတ် ပြန်သိမ်းမည်",
                        icon = Icons.Default.Storage,
                        iconBgColor = Color(0xFF475569),
                        onClick = {
                            viewModel.runDatabaseVacuumMaintenance { }
                        },
                    ),
                    AimFeatureTile(
                        id = "feat_settings",
                        title = "စနစ် ဆက်တင်များ",
                        subtitle = "Logo, ဘေလ်စာသား, Backup",
                        icon = Icons.Default.Tune,
                        iconBgColor = Color(0xFF334155),
                        onClick = onNavigateToSettings,
                    ),
                )
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("TR", fontWeight = FontWeight.Black, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "လုပ်ဆောင်ချက်များ (Features Hub)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${storeProfile.storeName} • TR-POS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showPrinterDialog = true },
                        modifier = Modifier.testTag("features_printer_button")
                    ) {
                        val hasPrinter = ThermalPrinterManager.getConnectionType(context) != PrinterConnectionType.NONE
                        Icon(
                            Icons.Default.Print,
                            contentDescription = "Thermal Printer",
                            tint = if (hasPrinter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 80.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("aims_features_grid")
        ) {
            // AIMS Hero Banner Card
            item(span = { GridItemSpan(2) }) {
                AimsFeatureHeroCard(
                    storeName = storeProfile.storeName,
                    cashierName = actor.userId,
                    urgentRunouts = urgentCount,
                    totalProducts = allProducts.size,
                    onOpenPrediction = { showStockPredictionDialog = true }
                )
            }

            featureGroups.forEach { group ->
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = group.groupTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                items(group.tiles, key = { it.id }) { tile ->
                    AimsFeatureTileCard(tile = tile)
                }
            }
        }
    }

    if (showCustomerDialog) {
        CustomerManagementDialog(
            viewModel = viewModel,
            onDismiss = { showCustomerDialog = false }
        )
    }

    if (showShiftReportDialog) {
        ShiftReportDialog(
            viewModel = viewModel,
            onDismiss = { showShiftReportDialog = false }
        )
    }

    if (showPrinterDialog) {
        ThermalPrinterDialog(
            viewModel = viewModel,
            onDismiss = { showPrinterDialog = false }
        )
    }

    if (showServerDialog) {
        ServerStatusDialog(
            viewModel = viewModel,
            onDismiss = { showServerDialog = false },
            onOpenStaffClient = { viewModel.setStaffClientMode(true) }
        )
    }

    if (showStockPredictionDialog) {
        StockPredictionDialog(
            viewModel = viewModel,
            onOpenStockIn = { prod, qty ->
                showStockPredictionDialog = false
                stockInPreselectedProduct = prod
                stockInSuggestedQty = qty
                showStockInDialog = true
            },
            onDismiss = { showStockPredictionDialog = false }
        )
    }

    if (showStockInDialog) {
        StockInDialog(
            viewModel = viewModel,
            preselectedProduct = stockInPreselectedProduct,
            suggestedQty = stockInSuggestedQty,
            onDismiss = {
                showStockInDialog = false
                stockInPreselectedProduct = null
            }
        )
    }
}

@Composable
private fun AimsFeatureHeroCard(
    storeName: String,
    cashierName: String,
    urgentRunouts: Int,
    totalProducts: Int,
    onOpenPrediction: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF10315A),
                            Color(0xFF1E3A8A),
                            Color(0xFF4338CA),
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                        Text(
                            text = "TR-POS Point of Sale • All-In-One Hub",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF93C5FD),
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f),
                    ) {
                        Text(
                            text = "Cashier: $cashierName",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ပစ္စည်း စုစုပေါင်း", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                            Text("$totalProducts မျိုး", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenPrediction),
                        shape = RoundedCornerShape(12.dp),
                        color = if (urgentRunouts > 0) Color(0xFFDC2626).copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("AI ကြိုတင်သတိပေးချက်", fontSize = 10.sp, color = Color(0xFFE2E8F0))
                            Text(
                                text = if (urgentRunouts > 0) "⚠️ $urgentRunouts မျိုး ပြတ်မည်" else "✅ အားလုံး ပုံမှန်",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (urgentRunouts > 0) Color(0xFFFECA57) else Color(0xFF86EFAC),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AimsFeatureTileCard(
    tile: AimFeatureTile,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = tile.onClick)
            .testTag(tile.id),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = tile.iconBgColor,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = tile.icon,
                            contentDescription = tile.title,
                            tint = tile.iconTint,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                if (tile.badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = tile.badgeColor,
                    ) {
                        Text(
                            text = tile.badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Column {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tile.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (tile.showProgressBar) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFFD97706),
                    trackColor = Color(0xFFFDE68A),
                )
            }
        }
    }
}
