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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.offlinepos.data.local.PurchaseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BrandRed = Color(0xFFDC2626)
private val BrandSalmon = Color(0xFFF3C7C7)
private val BrandSalmonText = Color(0xFF4A1515)
private val ScreenBg = Color(0xFFF8FAFC)
private val TextDark = Color(0xFF1E293B)
private val TextMuted = Color(0xFF64748B)
private val BorderLight = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PosViewModel,
    onOpenCart: () -> Unit,
    onNavigateToInventory: () -> Unit = {},
    onNavigateToSales: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAiCopilot: () -> Unit = {},
) {
    val salesHistory by viewModel.salesHistory.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val totals by viewModel.totals.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val purchases by viewModel.purchases.collectAsState()

    // Dialog state variables
    var showVoucherRegister by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    var showCustomerLedger by remember { mutableStateOf(false) }
    var showStockInDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showAiTipsDialog by remember { mutableStateOf(false) }
    var showDrawerMenu by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showUnitsDialog by remember { mutableStateOf(false) }
    var showSuppliersDialog by remember { mutableStateOf(false) }
    var showPaymentMethodsDialog by remember { mutableStateOf(false) }
    var showPurchasesListDialog by remember { mutableStateOf(false) }
    var showMonthlyPurchasesDialog by remember { mutableStateOf(false) }
    var showSupplierDebtDialog by remember { mutableStateOf(false) }
    var showExpensesDialog by remember { mutableStateOf(false) }
    var showExpenseCategoriesDialog by remember { mutableStateOf(false) }
    var showNewExpenseDialog by remember { mutableStateOf(false) }

    // Report Dialog State (Title, type)
    var activeReportType by remember { mutableStateOf<String?>(null) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todaySales = salesHistory.filter { it.businessDate == todayStr && it.status == "COMPLETED" }
    val todayVouchersCount = todaySales.size
    val totalCartItems = cart.sumOf { it.quantity }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top App Bar (☰ AIMS POS  💡 ? 👤)
            TopAppBar(
                title = {
                    Text(
                        text = "TR-POS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDark,
                        modifier = Modifier.testTag("app_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { showDrawerMenu = true },
                        modifier = Modifier.testTag("top_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = TextDark
                        )
                    }
                },
                actions = {
                    // Lightbulb (AI tips)
                    IconButton(
                        onClick = { showAiTipsDialog = true },
                        modifier = Modifier.testTag("top_lightbulb_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "AI Tips",
                            tint = Color(0xFFF59E0B)
                        )
                    }
                    // Question mark (Help)
                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.testTag("top_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Help",
                            tint = TextDark
                        )
                    }
                    // Profile avatar
                    IconButton(
                        onClick = { showProfileDialog = true },
                        modifier = Modifier.testTag("top_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )

            // Scrollable Content Hub
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 2. Banner: ယနေ့ဘောက်ချာများ [ 🧾 VIEW ]
                TodayVouchersBanner(
                    voucherCount = todayVouchersCount,
                    onViewClick = onNavigateToSales
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3. အခြေခံအချက်အလက် (Master Data)
                SectionTitle("အခြေခံအချက်အလက်")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            HubGridTile(
                                title = "ကုန်စည်များ",
                                icon = Icons.Default.Category,
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateToInventory
                            )
                            HubGridTile(
                                title = "ကုန်အုပ်စုများ",
                                icon = Icons.Default.GridView,
                                modifier = Modifier.weight(1f),
                                onClick = { showCategoriesDialog = true }
                            )
                            HubGridTile(
                                title = "ယူနစ်များ",
                                icon = Icons.Default.Straighten,
                                modifier = Modifier.weight(1f),
                                onClick = { showUnitsDialog = true }
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            HubGridTile(
                                title = "စျေးဝယ်သူများ",
                                icon = Icons.Default.People,
                                modifier = Modifier.weight(1f),
                                onClick = { showCustomerLedger = true }
                            )
                            HubGridTile(
                                title = "ကုန်သွင်းသူများ",
                                icon = Icons.Default.LocalShipping,
                                modifier = Modifier.weight(1f),
                                onClick = { showSuppliersDialog = true }
                            )
                            HubGridTile(
                                title = "ငွေပေးချေမှုများ",
                                icon = Icons.Default.Payments,
                                modifier = Modifier.weight(1f),
                                onClick = { showPaymentMethodsDialog = true }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. အရောင်းကဏ္ဍ (Sales Section)
                SectionTitle("အရောင်းကဏ္ဍ")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        HubGridTile(
                            title = "ဘောက်ချာဖွင့်မည်",
                            icon = Icons.Default.AddShoppingCart,
                            modifier = Modifier.weight(1f),
                            onClick = { showVoucherRegister = true }
                        )
                        HubGridTile(
                            title = "အရောင်းစာရင်း",
                            icon = Icons.Default.ReceiptLong,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToSales
                        )
                        HubGridTile(
                            title = "အရောင်းပြက္ခဒိန်",
                            icon = Icons.Default.CalendarMonth,
                            modifier = Modifier.weight(1f),
                            onClick = { activeReportType = "SALES_CALENDAR" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5. အဝယ်ကဏ္ဍ (Purchase Section)
                SectionTitle("အဝယ်ကဏ္ဍ")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        HubGridTile(
                            title = "အဝယ်",
                            icon = Icons.Default.ShoppingBag,
                            modifier = Modifier.weight(1f),
                            onClick = { showStockInDialog = true }
                        )
                        HubGridTile(
                            title = "အဝယ်စာရင်း",
                            icon = Icons.Default.Description,
                            modifier = Modifier.weight(1f),
                            onClick = { showPurchasesListDialog = true }
                        )
                        HubGridTile(
                            title = "လအလိုက်အဝယ်...",
                            icon = Icons.Default.CalendarToday,
                            modifier = Modifier.weight(1f),
                            onClick = { showMonthlyPurchasesDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 6. အကြွေးစာရင်း (Credit / Debt Ledger)
                SectionTitle("အကြွေးစာရင်း")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        HubGridTile(
                            title = "စျေးဝယ်သူ အကြွေးစာရင်း",
                            icon = Icons.Default.AccountBalanceWallet,
                            modifier = Modifier.weight(1f),
                            onClick = { showCustomerLedger = true }
                        )
                        HubGridTile(
                            title = "ကုန်သွင်းသူ အကြွေးစာရင်း",
                            icon = Icons.Default.LocalShipping,
                            modifier = Modifier.weight(1f),
                            onClick = { showSupplierDebtDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 7. လက်ကျန်အစီရင်ခံစာများ (Stock Reports)
                SectionTitle("လက်ကျန်အစီရင်ခံစာများ")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ReportListItem(
                            title = "ကုန်ပစ္စည်းလက်ကျန်",
                            icon = Icons.Default.Inventory,
                            onClick = { activeReportType = "STOCK_BALANCE" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "ကုန်ပစ္စည်းလက်ကျန်အဝယ်တန်ဖိုး",
                            icon = Icons.Default.MonetizationOn,
                            onClick = { activeReportType = "STOCK_COST_VALUE" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "ကုန်ပစ္စည်းလက်ကျန်အရောင်းတန်ဖိုး",
                            icon = Icons.Default.PriceCheck,
                            onClick = { activeReportType = "STOCK_RETAIL_VALUE" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "လက်ကျန်နည်းနေသောကုန်ပစ္စည်းများ",
                            icon = Icons.Default.WarningAmber,
                            onClick = { activeReportType = "LOW_STOCK" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "ကုန်ပစ္စည်းအဝင်/အထွက်မှတ်တမ်း",
                            icon = Icons.Default.SwapVert,
                            onClick = { activeReportType = "STOCK_IN_OUT_LOG" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "အရောင်းမှတ်တမ်း",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            onClick = onNavigateToSales
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "အဝယ်မှတ်တမ်း",
                            icon = Icons.Default.LocalShipping,
                            onClick = { showPurchasesListDialog = true }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "လက်ကျန်လွှဲပြောင်းခြင်းမှတ်တမ်း",
                            icon = Icons.Default.SwapVert,
                            onClick = { activeReportType = "STOCK_TRANSFER" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "လက်ကျန်ပြုပြင်ခြင်းမှတ်တမ်း",
                            icon = Icons.Default.Tune,
                            onClick = { activeReportType = "STOCK_ADJUSTMENT" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 8. အစီရင်ခံစာများ (Financial & Sales Reports)
                SectionTitle("အစီရင်ခံစာများ")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ReportListItem(
                            title = "နေ့စဉ်အရောင်းစာရင်း",
                            icon = Icons.Default.Today,
                            onClick = { activeReportType = "DAILY_SALES" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "အရောင်းပြက္ခဒိန်",
                            icon = Icons.Default.DateRange,
                            onClick = { activeReportType = "SALES_CALENDAR" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "လအလိုက်အရောင်းစာရင်း",
                            icon = Icons.Default.CalendarMonth,
                            onClick = { activeReportType = "MONTHLY_SALES" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "ကုန်ပစ္စည်းအလိုက်စုစုပေါင်းအရောင်း(Total)",
                            icon = Icons.Default.Leaderboard,
                            onClick = { activeReportType = "PRODUCT_TOTAL_REVENUE" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "ကုန်ပစ္စည်းအလိုက်စုစုပေါင်းအရောင်း(Quantity)",
                            icon = Icons.Default.BarChart,
                            onClick = { activeReportType = "PRODUCT_TOTAL_QTY" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "Customer အလိုက်စုစုပေါင်းဝယ်ယူမှု",
                            icon = Icons.Default.Group,
                            onClick = { activeReportType = "CUSTOMER_PURCHASES" }
                        )
                        ReportListDivider()
                        ReportListItem(
                            title = "အမြတ်စာရင်း",
                            icon = Icons.Default.Savings,
                            onClick = { activeReportType = "GROSS_PROFIT" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 9. ကုန်ကျစရိတ်များ (Expenses)
                SectionTitle("ကုန်ကျစရိတ်များ")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        HubGridTile(
                            title = "ကုန်ကျစရိတ်အသစ်...",
                            icon = Icons.Default.AddCircleOutline,
                            modifier = Modifier.weight(1f),
                            onClick = { showNewExpenseDialog = true }
                        )
                        HubGridTile(
                            title = "ကုန်ကျစရိတ်များ",
                            icon = Icons.Default.Receipt,
                            modifier = Modifier.weight(1f),
                            onClick = { showExpensesDialog = true }
                        )
                        HubGridTile(
                            title = "ကုန်ကျစရိတ်ခေါင်းစဉ်...",
                            icon = Icons.Default.Label,
                            modifier = Modifier.weight(1f),
                            onClick = { showExpenseCategoriesDialog = true }
                        )
                    }
                }

                // Bottom padding for comfortable scrolling
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Cart Bar (if items in cart)
        if (totalCartItems > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
                    .clickable { onOpenCart() },
                shape = RoundedCornerShape(28.dp),
                color = BrandRed,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ဘောက်ချာတွင် $totalCartItems ခု ရှိနေပါသည်",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "%,d Ks".format(totals.totalMinor),
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Button(
                        onClick = onOpenCart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = BrandRed
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("ငွေရှင်းမည်", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // DIALOGS & OVERLAYS
    // ----------------------------------------------------

    // Quick Voucher / Sale Register Dialog
    if (showVoucherRegister) {
        VoucherSaleRegisterDialog(
            viewModel = viewModel,
            onDismiss = { showVoucherRegister = false },
            onOpenFullCart = {
                showVoucherRegister = false
                onOpenCart()
            }
        )
    }

    // Barcode Scanner
    if (showScanner) {
        CameraBarcodeScannerDialog(
            onDismiss = { showScanner = false },
            onBarcodeScanned = { barcode ->
                viewModel.handleScannedBarcode(barcode)
                showScanner = false
                showVoucherRegister = true
            }
        )
    }

    // Customers CRM & Customer Debt
    if (showCustomerLedger) {
        CustomerManagementDialog(
            viewModel = viewModel,
            onDismiss = { showCustomerLedger = false }
        )
    }

    // Stock In (Purchase)
    if (showStockInDialog) {
        StockInDialog(
            viewModel = viewModel,
            preselectedProduct = null,
            suggestedQty = 10L,
            onDismiss = { showStockInDialog = false }
        )
    }

    // AI Tips Dialog
    if (showAiTipsDialog) {
        AiTipsModalDialog(
            viewModel = viewModel,
            onDismiss = { showAiTipsDialog = false },
            onOpenAiCopilot = {
                showAiTipsDialog = false
                onNavigateToAiCopilot()
            }
        )
    }

    // Help Dialog
    if (showHelpDialog) {
        PosHelpGuideDialog(onDismiss = { showHelpDialog = false })
    }

    // Cashier Profile Dialog
    if (showProfileDialog) {
        ProfileInfoDialog(
            viewModel = viewModel,
            onDismiss = { showProfileDialog = false },
            onOpenSettings = {
                showProfileDialog = false
                onNavigateToSettings()
            }
        )
    }

    // Drawer Menu
    if (showDrawerMenu) {
        SideNavDrawerDialog(
            viewModel = viewModel,
            onDismiss = { showDrawerMenu = false },
            onNavigateToInventory = onNavigateToInventory,
            onNavigateToSales = onNavigateToSales,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToAiCopilot = onNavigateToAiCopilot,
        )
    }

    // Categories Dialog
    if (showCategoriesDialog) {
        CategoriesManagerDialog(
            viewModel = viewModel,
            onDismiss = { showCategoriesDialog = false }
        )
    }

    // Units Dialog
    if (showUnitsDialog) {
        UnitsManagerDialog(onDismiss = { showUnitsDialog = false })
    }

    // Suppliers Dialog
    if (showSuppliersDialog) {
        SuppliersManagerDialog(
            purchases = purchases,
            onDismiss = { showSuppliersDialog = false }
        )
    }

    // Supplier Debt Dialog
    if (showSupplierDebtDialog) {
        SupplierDebtDialog(
            purchases = purchases,
            onDismiss = { showSupplierDebtDialog = false }
        )
    }

    // Payment Methods Dialog
    if (showPaymentMethodsDialog) {
        PaymentMethodsManagerDialog(onDismiss = { showPaymentMethodsDialog = false })
    }

    // Purchases List Dialog
    if (showPurchasesListDialog) {
        PurchasesListDialog(
            purchases = purchases,
            onDismiss = { showPurchasesListDialog = false }
        )
    }

    // Monthly Purchases Dialog
    if (showMonthlyPurchasesDialog) {
        MonthlyPurchasesReportDialog(
            purchases = purchases,
            onDismiss = { showMonthlyPurchasesDialog = false }
        )
    }

    // Expenses Tracker Dialogs
    if (showExpensesDialog) {
        ExpensesManagerDialog(onDismiss = { showExpensesDialog = false })
    }
    if (showNewExpenseDialog) {
        NewExpenseEntryDialog(
            onDismiss = { showNewExpenseDialog = false },
            onSaved = { showNewExpenseDialog = false }
        )
    }
    if (showExpenseCategoriesDialog) {
        ExpenseCategoriesDialog(onDismiss = { showExpenseCategoriesDialog = false })
    }

    // Unified Reports Viewer for the Stock & Financial report items
    if (activeReportType != null) {
        DynamicReportViewerDialog(
            reportType = activeReportType!!,
            viewModel = viewModel,
            onDismiss = { activeReportType = null }
        )
    }
}

// -----------------------------------------------------------------------------
// UI SUB-COMPONENTS
// -----------------------------------------------------------------------------

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = TextDark,
        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
    )
}

@Composable
private fun TodayVouchersBanner(
    voucherCount: Int,
    onViewClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BrandSalmon,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Receipt",
                    tint = BrandSalmonText,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "ယနေ့ဘောက်ချာများ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandSalmonText
                    )
                    Text(
                        text = "စုစုပေါင်း ($voucherCount) စောင်",
                        fontSize = 12.sp,
                        color = BrandSalmonText.copy(alpha = 0.8f)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onViewClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "View",
                        tint = BrandSalmonText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VIEW",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandSalmonText
                    )
                }
            }
        }
    }
}

@Composable
private fun HubGridTile(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Red squircle with white icon
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReportListItem(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red squircle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandRed),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextDark
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Details",
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ReportListDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 68.dp, end = 14.dp),
        thickness = 0.8.dp,
        color = BorderLight
    )
}

// -----------------------------------------------------------------------------
// SALE & VOUCHER QUICK REGISTER DIALOG
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherSaleRegisterDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onOpenFullCart: () -> Unit
) {
    val products by viewModel.filteredProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val totals by viewModel.totals.collectAsState()
    var showScanDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ScreenBg
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = {
                        Text("ဘောက်ချာဖွင့်ခြင်း (POS Register)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showScanDialog = true }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = BrandRed)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    textStyle = TextStyle(color = Color(0xFF1E293B), fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("ကုန်ပစ္စည်းအမည် (သို့) ဘားကုဒ် ရှာရန်...", color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF1E293B),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFFDC2626),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        cursorColor = Color(0xFFDC2626),
                    )
                )

                // Categories Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "အားလုံး (All)",
                            onClick = { viewModel.setSelectedCategory("အားလုံး (All)") },
                            label = { Text("အားလုံး") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandSalmon,
                                selectedLabelColor = BrandSalmonText
                            )
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.setSelectedCategory(cat) },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandSalmon,
                                selectedLabelColor = BrandSalmonText
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Product items list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products) { prod ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            tint = BrandRed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = prod.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "%,d Ks • လက်ကျန်: %d".format(prod.sellPriceMinor, prod.stockQuantity),
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Button(
                                    onClick = { viewModel.addToCart(prod) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ထည့်မည်", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Bottom Cart Summary & Checkout button
                if (cart.isNotEmpty()) {
                    Surface(
                        color = Color.White,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ရွေးချယ်ထားသော ပစ္စည်း: ${cart.size} မျိုး",
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "စုစုပေါင်း: %,d Ks".format(totals.totalMinor),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = BrandRed
                                    )
                                }

                                Button(
                                    onClick = onOpenFullCart,
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Text("ငွေရှင်းမည် / ဘောက်ချာ", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showScanDialog) {
        CameraBarcodeScannerDialog(
            onDismiss = { showScanDialog = false },
            onBarcodeScanned = { barcode ->
                viewModel.handleScannedBarcode(barcode)
                showScanDialog = false
            }
        )
    }
}

// -----------------------------------------------------------------------------
// AUXILIARY DIALOGS: UNITS, CATEGORIES, SUPPLIERS, PAYMENTS, EXPENSES
// -----------------------------------------------------------------------------

@Composable
fun CategoriesManagerDialog(viewModel: PosViewModel, onDismiss: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ကုန်အုပ်စုများ (Categories)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandSalmon),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = BrandSalmonText, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat, fontSize = 14.sp, color = TextDark)
                        }
                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

@Composable
fun UnitsManagerDialog(onDismiss: () -> Unit) {
    val defaultUnits = remember {
        listOf(
            "ခု (Pcs)" to "အခြေခံရေတွက်ယူနစ်",
            "ထုပ် (Pack)" to "အထုပ်လိုက် အရောင်းအဝယ်",
            "ဗူး (Box / Can)" to "ဗူးလိုက် အရောင်းအဝယ်",
            "ကတ် (Card / Strip)" to "ဆေးဝါးနှင့် ကုန်ခြောက်",
            "ပိဿာ (Viss)" to "အလေးချိန်ယူနစ် (မြန်မာ)",
            "ကီလို (Kg)" to "နိုင်ငံတကာစံချိန်အလေးချိန်",
            "ပုလင်း (Bottle)" to "အရည်အသွေးသောက်စရာ",
            "ဖာ (Carton)" to "လက်ကား အရောင်းအဝယ်",
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ယူနစ်များ (Measurement Units)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(defaultUnits) { (unitName, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF2F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Straighten, contentDescription = null, tint = BrandRed, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(unitName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextDark)
                                Text(desc, fontSize = 12.sp, color = TextMuted)
                            }
                        }
                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

@Composable
fun SuppliersManagerDialog(purchases: List<PurchaseEntity>, onDismiss: () -> Unit) {
    val suppliers = remember(purchases) {
        purchases.map { it.supplierName }.distinct().ifEmpty {
            listOf("ရွှေမြန်မာ ကုန်မာလက်ကား", "အောင်သမာဓိ ဖြန့်ချိရေး", "ဒေါ်စန်းစန်း စားသောက်ကုန်", "Asia General Distribution")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ကုန်သွင်းသူများ (Suppliers)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(suppliers) { sup ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF2F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = BrandRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(sup, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextDark)
                                Text("တရားဝင်လက်ကားဖြန့်ချိသူ", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

@Composable
fun SupplierDebtDialog(purchases: List<PurchaseEntity>, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ကုန်သွင်းသူ အကြွေးစာရင်း (Supplier Debt)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("လက်ရှိ ကုန်သွင်းသူများထံ ပေးရန်ကျန်ငွေ မှတ်တမ်း", fontSize = 12.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("စုစုပေါင်း ပေးရန်ကျန်ငွေ", fontSize = 12.sp, color = BrandRed)
                        Text("0 Ks", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandRed)
                        Text("လက်ရှိတွင် ကုန်သွင်းသူများသို့ ပေးရန်ကျန်ငွေ မရှိသေးပါ။", fontSize = 11.sp, color = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

@Composable
fun PaymentMethodsManagerDialog(onDismiss: () -> Unit) {
    val methods = remember {
        listOf(
            "ငွေသား (Cash)" to "တိုက်ရိုက် လက်ခံငွေသား",
            "KBZPay" to "09-xxx-xxx-xxx • KBZPay QR",
            "WavePay" to "09-xxx-xxx-xxx • Wave QR",
            "CB Pay" to "Digital Banking",
            "AYA Pay" to "Digital Banking",
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ငွေပေးချေမှုများ (Payment Methods)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(methods) { (name, note) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFEF2F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = BrandRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextDark)
                                Text(note, fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

@Composable
fun PurchasesListDialog(purchases: List<PurchaseEntity>, onDismiss: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("အဝယ်စာရင်း (Purchase History)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                if (purchases.isEmpty()) {
                    Box(modifier = Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("အဝယ်မှတ်တမ်း မရှိသေးပါ။", color = TextMuted)
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(purchases) { p ->
                            val dateStr = dateFormat.format(Date(p.createdAtEpochMs))
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(p.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${p.quantity} ခု • ကျသင့်ငွေ: %,d Ks".format(p.totalCostMinor), fontSize = 12.sp, color = BrandRed)
                                Text("ကုန်သွင်းသူ: ${p.supplierName} • $dateStr", fontSize = 11.sp, color = TextMuted)
                            }
                            HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

@Composable
fun MonthlyPurchasesReportDialog(purchases: List<PurchaseEntity>, onDismiss: () -> Unit) {
    val totalPurchasesCost = purchases.sumOf { it.totalCostMinor }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("လအလိုက် အဝယ်စာရင်း", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("ယခုလ စုစုပေါင်း အဝယ်တန်ဖိုး", fontSize = 12.sp, color = BrandRed)
                        Text("%,d Ks".format(totalPurchasesCost), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandRed)
                        Text("အကြိမ်ရေ: ${purchases.size} ကြိမ်", fontSize = 11.sp, color = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// EXPENSES DIALOGS
// -----------------------------------------------------------------------------

private val SampleExpenses = mutableStateListOf(
    Triple("ဆိုင်ခန်းငှားရမ်းခ", 150000L, "2026-10-01"),
    Triple("မီတာခနှင့် လျှပ်စစ်စရိတ်", 45000L, "2026-10-15"),
    Triple("ကုန်ပစ္စည်း သယ်ယူပို့ဆောင်ခ", 25000L, "2026-10-20"),
    Triple("ဆိုင်သုံး ပစ္စည်းများနှင့် ထုပ်ပိုးအိတ်", 12000L, "2026-10-22")
)

@Composable
fun ExpensesManagerDialog(onDismiss: () -> Unit) {
    val totalExpense = SampleExpenses.sumOf { it.second }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ကုန်ကျစရိတ်များ (Expenses)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("စုစုပေါင်း ကုန်ကျစရိတ်: %,d Ks".format(totalExpense), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BrandRed)
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(SampleExpenses) { exp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(exp.first, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextDark)
                                Text(exp.third, fontSize = 11.sp, color = TextMuted)
                            }
                            Text("%,d Ks".format(exp.second), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandRed)
                        }
                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

@Composable
fun NewExpenseEntryDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ကုန်ကျစရိတ်အသစ် ထည့်သွင်းခြင်း", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("အသုံးစရိတ် ခေါင်းစဉ် (ဥပမာ - မီးဖိုး)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("ကျသင့်ငွေ (Ks)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("မလုပ်တော့ပါ")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amount.toLongOrNull() ?: 0L
                            if (title.isNotBlank() && amt > 0) {
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                SampleExpenses.add(Triple(title, amt, today))
                                onSaved()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                    ) {
                        Text("သိမ်းဆည်းမည်")
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCategoriesDialog(onDismiss: () -> Unit) {
    val categories = remember {
        listOf("ဆိုင်ခန်းငှားရမ်းခ", "လျှပ်စစ်နှင့် မီတာခ", "သယ်ယူပို့ဆောင်ခ", "ထုပ်ပိုးပစ္စည်းနှင့် စာရေးကိရိယာ", "ဝန်ထမ်းလစာ", "အထွေထွေ အသုံးစရိတ်")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ကုန်ကျစရိတ် ခေါင်းစဉ်များ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(categories) { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Label, contentDescription = null, tint = BrandRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(cat, fontSize = 14.sp, color = TextDark)
                        }
                        HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// DYNAMIC REPORT VIEWER (Stock and Financial reports)
// -----------------------------------------------------------------------------

@Composable
fun DynamicReportViewerDialog(
    reportType: String,
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val products by viewModel.allProducts.collectAsState()
    val sales by viewModel.salesHistory.collectAsState()
    val customers by viewModel.customers.collectAsState()

    val totalCostValue = products.fold(0L) { acc, p -> acc + (p.costPriceMinor * p.stockQuantity) }
    val totalRetailValue = products.fold(0L) { acc, p -> acc + (p.sellPriceMinor * p.stockQuantity) }
    val totalGrossProfitEst = totalRetailValue - totalCostValue

    val (reportTitle, reportDesc) = when (reportType) {
        "STOCK_BALANCE" -> "ကုန်ပစ္စည်းလက်ကျန် (Stock Balance)" to "ပစ္စည်းအလိုက် လက်ရှိကျန်ရှိသော အရေအတွက်"
        "STOCK_COST_VALUE" -> "ကုန်ပစ္စည်းလက်ကျန် အဝယ်တန်ဖိုး" to "လက်ကျန်ပစ္စည်းများ၏ အရင်းတန်ဖိုး စုစုပေါင်း"
        "STOCK_RETAIL_VALUE" -> "ကုန်ပစ္စည်းလက်ကျန် အရောင်းတန်ဖိုး" to "လက်ကျန်ပစ္စည်းများ၏ ရောင်းစျေးတန်ဖိုး စုစုပေါင်း"
        "LOW_STOCK" -> "လက်ကျန်နည်းနေသော ကုန်ပစ္စည်းများ" to "သတ်မှတ်ချက်အောက် ရောက်နေသော ပစ္စည်းများ"
        "STOCK_IN_OUT_LOG" -> "ကုန်ပစ္စည်း အဝင်/အထွက် မှတ်တမ်း" to "ရောင်းချမှုနှင့် အဝယ်မှတ်တမ်းများ"
        "STOCK_TRANSFER" -> "လက်ကျန်လွှဲပြောင်းခြင်း မှတ်တမ်း" to "ဂိုဒေါင်နှင့် ဆိုင်ခွဲများသို့ လွှဲပြောင်းမှု"
        "STOCK_ADJUSTMENT" -> "လက်ကျန်ပြုပြင်ခြင်း မှတ်တမ်း" to "စာရင်းစစ်ဆေး၍ လက်ကျန်ပြင်ဆင်မှုများ"
        "DAILY_SALES" -> "နေ့စဉ်အရောင်းစာရင်း (Daily Z-Report)" to "ယနေ့ အရောင်းရငွေနှင့် ဘောက်ချာများ"
        "SALES_CALENDAR" -> "အရောင်းပြက္ခဒိန် (Sales Calendar)" to "ရက်စွဲအလိုက် အရောင်းပမာဏ ပြက္ခဒိန်"
        "MONTHLY_SALES" -> "လအလိုက် အရောင်းစာရင်း" to "လအလိုက် စုစုပေါင်း အရောင်းမှတ်တမ်း"
        "PRODUCT_TOTAL_REVENUE" -> "ကုန်ပစ္စည်းအလိုက် စုစုပေါင်းအရောင်း (Total)" to "ပစ္စည်းအလိုက် ရောင်းအားအများဆုံး စာရင်း"
        "PRODUCT_TOTAL_QTY" -> "ကုန်ပစ္စည်းအလိုက် စုစုပေါင်းအရောင်း (Quantity)" to "ပစ္စည်းအလိုက် ရောင်းအားအကောင်းဆုံး အရေအတွက်"
        "CUSTOMER_PURCHASES" -> "Customer အလိုက် စုစုပေါင်းဝယ်ယူမှု" to "ဖောက်သည်များ၏ စုစုပေါင်း ဝယ်ယူမှုပမာဏ"
        "GROSS_PROFIT" -> "အမြတ်စာရင်း (Gross Profit)" to "အရောင်းဝင်ငွေနှင့် အရင်းနုတ်ပြီး အမြတ်စုစုပေါင်း"
        else -> "အစီရင်ခံစာ" to ""
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(reportTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Text(reportDesc, fontSize = 12.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(14.dp))

                // Summary Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        when (reportType) {
                            "STOCK_COST_VALUE" -> {
                                Text("စုစုပေါင်း အဝယ်အရင်းတန်ဖိုး", fontSize = 12.sp, color = BrandRed)
                                Text("%,d Ks".format(totalCostValue), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandRed)
                            }
                            "STOCK_RETAIL_VALUE" -> {
                                Text("စုစုပေါင်း ရောင်းစျေးတန်ဖိုး", fontSize = 12.sp, color = BrandRed)
                                Text("%,d Ks".format(totalRetailValue), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandRed)
                            }
                            "GROSS_PROFIT" -> {
                                Text("ခန့်မှန်း အမြတ်ငွေ စုစုပေါင်း", fontSize = 12.sp, color = BrandRed)
                                Text("%,d Ks".format(totalGrossProfitEst), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandRed)
                            }
                            "STOCK_BALANCE" -> {
                                Text("စုစုပေါင်း ကုန်ပစ္စည်းအမျိုးအမည်", fontSize = 12.sp, color = BrandRed)
                                Text("${products.size} မျိုး", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandRed)
                            }
                            "LOW_STOCK" -> {
                                val lowStockCount = products.count { it.stockQuantity <= it.lowStockThreshold }
                                Text("လက်ကျန်နည်းနေသော ပစ္စည်း", fontSize = 12.sp, color = BrandRed)
                                Text("$lowStockCount မျိုး", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandRed)
                            }
                            "DAILY_SALES" -> {
                                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val todaySales = sales.filter { it.businessDate == todayStr }
                                val todayTotal = todaySales.sumOf { it.totalMinor }
                                Text("ယနေ့ ဘောင်ချာ စုစုပေါင်း (${todaySales.size} စောင်)", fontSize = 12.sp, color = BrandRed)
                                Text("%,d Ks".format(todayTotal), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandRed)
                            }
                            else -> {
                                Text("အချက်အလက် စုစုပေါင်း", fontSize = 12.sp, color = BrandRed)
                                Text("မှတ်တမ်းစုံလင်စွာ သိမ်းဆည်းထားပြီးဖြစ်ပါသည်", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BrandRed)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Items preview list
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    when (reportType) {
                        "DAILY_SALES" -> {
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val todaySales = sales.filter { it.businessDate == todayStr }.sortedByDescending { it.createdAtEpochMs }
                            if (todaySales.isEmpty()) {
                                item { Text("ယနေ့ အရောင်းမှတ်တမ်း မရှိသေးပါ။", color = TextMuted) }
                            } else {
                                items(todaySales) { s ->
                                    val vNo = com.example.offlinepos.domain.VoucherHelper.getVoucherNumber(s, sales)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(vNo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1E3A8A))
                                            Text("Ref: #${s.saleId.take(8).uppercase()}", fontSize = 11.sp, color = TextMuted)
                                        }
                                        Text("%,d Ks".format(s.totalMinor), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandRed)
                                    }
                                    HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                                }
                            }
                        }
                        "LOW_STOCK" -> {
                            val lowProds = products.filter { it.stockQuantity <= it.lowStockThreshold }
                            if (lowProds.isEmpty()) {
                                item { Text("လက်ကျန်နည်းနေသော ပစ္စည်း မရှိသေးပါ။", color = TextMuted) }
                            } else {
                                items(lowProds) { p ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(p.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                        Text("လက်ကျန်: ${p.stockQuantity}", color = BrandRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                                }
                            }
                        }
                        "CUSTOMER_PURCHASES" -> {
                            items(customers) { c ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(c.name, fontSize = 13.sp)
                                    Text("အကြွေးကျန်: %,d Ks".format(c.currentDebtMinor), fontSize = 13.sp, color = BrandRed)
                                }
                                HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                            }
                        }
                        else -> {
                            items(products.take(15)) { p ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(p.name, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Text("%,d Ks".format(p.sellPriceMinor), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                                HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// TOP BAR ACTION DIALOGS (HELP, PROFILE, AI TIPS, DRAWER)
// -----------------------------------------------------------------------------

@Composable
fun PosHelpGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = BrandRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TR-POS အသုံးပြုနည်း အကူအညီ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("၁။ 'ဘောက်ချာဖွင့်မည်' ကိုနှိပ်၍ စျေးရောင်း ဘောက်ချာ ဖွင့်နိုင်ပါသည်။", fontSize = 13.sp, color = TextDark)
                Spacer(modifier = Modifier.height(6.dp))
                Text("၂။ 'အဝယ်' မှတစ်ဆင့် ကုန်ပစ္စည်း အသစ်ထည့်သွင်းခြင်းနှင့် လက်ကျန်တိုးခြင်းများ ပြုလုပ်နိုင်ပါသည်။", fontSize = 13.sp, color = TextDark)
                Spacer(modifier = Modifier.height(6.dp))
                Text("၃။ 'စျေးဝယ်သူ အကြွေးစာရင်း' တွင် အကြွေးရောင်းချမှုနှင့် အကြွေးဆပ်စာရင်းများကို စီမံနိုင်ပါသည်။", fontSize = 13.sp, color = TextDark)
                Spacer(modifier = Modifier.height(6.dp))
                Text("၄။ 'အစီရင်ခံစာများ' ကဏ္ဍတွင် နေ့စဉ်အရောင်းနှင့် အမြတ်စာရင်းများကို အချိန်မရွေး ကြည့်ရှုနိုင်ပါသည်။", fontSize = 13.sp, color = TextDark)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("နားလည်ပါပြီ")
                }
            }
        }
    }
}

@Composable
fun ProfileInfoDialog(viewModel: PosViewModel, onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    val storeProfile by viewModel.storeProfile.collectAsState()
    val actor by viewModel.actor.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = BrandRed, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(storeProfile.storeName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("တာဝန်ကျ စာရေး: ${actor.userId}", fontSize = 13.sp, color = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("ဖုန်း: ${storeProfile.phone}", fontSize = 13.sp, color = TextDark)
                Text("လိပ်စာ: ${storeProfile.address}", fontSize = 13.sp, color = TextDark)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onOpenSettings) {
                        Text("ဆိုင်အချက်အလက် ပြင်မည်")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                    ) {
                        Text("ပိတ်မည်")
                    }
                }
            }
        }
    }
}

@Composable
fun AiTipsModalDialog(viewModel: PosViewModel, onDismiss: () -> Unit, onOpenAiCopilot: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("AI အရောင်းနှင့် ကုန်ပစ္စည်း အကြံပြုချက်", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("💡 ရောင်းအားအကောင်းဆုံး ထိပ်တန်းပစ္စည်းများကို မျက်နှာစာတွင် ထားရှိရန် အကြံပြုပါသည်။", fontSize = 13.sp, color = TextDark)
                Spacer(modifier = Modifier.height(8.dp))
                Text("💡 ပစ္စည်းလက်ကျန် နည်းနေသော ကုန်စည်များကို အချိန်မီ ကြိုတင်မှာယူထားပါ။", fontSize = 13.sp, color = TextDark)
                Spacer(modifier = Modifier.height(8.dp))
                Text("💡 အကြွေးကျန်များနေသော စျေးဝယ်သူများအား အကြွေးသတိပေးချက် အသိပေးနိုင်ပါသည်။", fontSize = 13.sp, color = TextDark)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onOpenAiCopilot) {
                        Text("AI အကူ မေးမည်")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                    ) {
                        Text("ပိတ်မည်")
                    }
                }
            }
        }
    }
}

@Composable
fun SideNavDrawerDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAiCopilot: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("TR-POS စနစ် မီနူး", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BrandRed)
                Spacer(modifier = Modifier.height(14.dp))

                DrawerMenuItem("ကုန်စည်စာရင်း (Inventory)", Icons.Default.Inventory) {
                    onDismiss()
                    onNavigateToInventory()
                }
                DrawerMenuItem("ဘောက်ချာမှတ်တမ်း (Sales History)", Icons.Default.Receipt) {
                    onDismiss()
                    onNavigateToSales()
                }
                DrawerMenuItem("AI အကူ (AI Copilot)", Icons.Default.AutoAwesome) {
                    onDismiss()
                    onNavigateToAiCopilot()
                }
                DrawerMenuItem("စနစ်ပြင်ဆင်ချက် (Settings)", Icons.Default.Tune) {
                    onDismiss()
                    onNavigateToSettings()
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed)
                ) {
                    Text("ပိတ်မည်")
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = BrandRed, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = TextDark)
    }
    HorizontalDivider(color = BorderLight, thickness = 0.5.dp)
}
