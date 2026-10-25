package com.example.offlinepos

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.offlinepos.Sync.WeeklyRecommendationWorker
import com.example.offlinepos.Sync.GoogleDriveSyncWorker
import java.util.concurrent.TimeUnit
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import com.example.offlinepos.ui.AiCopilotScreen
import com.example.offlinepos.ui.CartSheet
import com.example.offlinepos.ui.CheckoutDialog
import com.example.offlinepos.ui.FeaturesHomeScreen
import com.example.offlinepos.ui.HomeScreen
import com.example.offlinepos.ui.InventoryScreen
import com.example.offlinepos.ui.PosViewModel
import com.example.offlinepos.ui.ProductDetailsDialog
import com.example.offlinepos.ui.ReceiptDialog
import com.example.offlinepos.ui.RoofingCalculatorDialog
import com.example.offlinepos.ui.SalesHistoryScreen
import com.example.offlinepos.ui.SettingsScreen
import com.example.offlinepos.ui.StaffClientScreen
import com.example.offlinepos.ui.theme.OfflinePOSTheme
import com.example.offlinepos.data.local.AppDatabase
import java.io.File

enum class PosTab(val title: String) {
    REGISTER("အရောင်း"),
    SALES("ဘောက်ချာများ"),
    FEATURES("လုပ်ဆောင်ချက်များ"),
    AI_COPILOT("AI အကူ"),
    SETTINGS("ဆက်တင်"),
    INVENTORY("ပစ္စည်းစာရင်း"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.offlinepos.ai.configureAppCheck(this, intent)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e("OfflinePOS", "Uncaught exception on $thread: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
        enableEdgeToEdge()
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 787)
            }
        }
        val viewModel = createViewModelWithDatabaseRecovery()
        runCatching {
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "weekly_stock_recommendation",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WeeklyRecommendationWorker>(7, TimeUnit.DAYS).build(),
            )
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "google-drive-background-sync",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<GoogleDriveSyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(GoogleDriveSyncWorker.constraints())
                    .build(),
            )
        }.onFailure { err ->
            android.util.Log.w("MainActivity", "WorkManager initialization/scheduling bypassed", err)
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                com.example.offlinepos.ui.StoreConfig.ThemeMode.DARK -> true
                com.example.offlinepos.ui.StoreConfig.ThemeMode.LIGHT -> false
                com.example.offlinepos.ui.StoreConfig.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            OfflinePOSTheme(darkTheme = isDark) {
                MainApp(viewModel = viewModel)
            }
        }
    }

    private fun createViewModelWithDatabaseRecovery(): PosViewModel {
        return try {
            val dbInstance = AppDatabase.get(applicationContext)
            // Force schema validation and SQLite connection NOW inside the try block
            dbInstance.openHelper.writableDatabase
            PosViewModel(applicationContext)
        } catch (firstError: Throwable) {
            android.util.Log.e("MainActivity", "Database open failed on first attempt, initiating recovery", firstError)
            AppDatabase.closeAndReset()
            val db = getDatabasePath("offline_pos.db")
            val stamp = System.currentTimeMillis()
            val backup = File(db.parentFile, "offline_pos.db.pre_recovery_$stamp")
            if (db.exists()) db.renameTo(backup)
            File(db.path + "-wal").takeIf { it.exists() }?.renameTo(File(db.parentFile, "offline_pos.db.pre_recovery_$stamp-wal"))
            File(db.path + "-shm").takeIf { it.exists() }?.renameTo(File(db.parentFile, "offline_pos.db.pre_recovery_$stamp-shm"))
            try {
                val freshDb = AppDatabase.get(applicationContext)
                freshDb.openHelper.writableDatabase
                PosViewModel(applicationContext).also {
                    it.setBanner("Database ကို အောင်မြင်စွာ ပြန်လည်ပြင်ဆင်ပြီး ဖြစ်ပါသည်။")
                }
            } catch (secondError: Throwable) {
                android.util.Log.e("MainActivity", "Database recovery failed, resetting files", secondError)
                db.delete()
                File(db.path + "-wal").delete()
                File(db.path + "-shm").delete()
                AppDatabase.closeAndReset()
                PosViewModel(applicationContext)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MainApp(viewModel: PosViewModel) {
    val isStaffClientMode by viewModel.isStaffClientMode.collectAsState()

    if (isStaffClientMode) {
        StaffClientScreen(
            viewModel = viewModel,
            onSwitchToServerMode = {
                viewModel.setStaffClientMode(false)
            }
        )
        return
    }

    var selectedTab by remember { mutableStateOf(PosTab.REGISTER) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val cart by viewModel.cart.collectAsState()
    val totals by viewModel.totals.collectAsState()
    val receipt by viewModel.receipt.collectAsState()
    val actor by viewModel.actor.collectAsState()
    val bannerMessage by viewModel.bannerMessage.collectAsState()
    val detailsProduct by viewModel.detailsProduct.collectAsState()
    val roofingProduct by viewModel.roofingProduct.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show banner notifications
    LaunchedEffect(bannerMessage) {
        bannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    // Predictive back handler for tabs and sheets
    BackHandler(
        enabled = showCartSheet || showCheckoutDialog || detailsProduct != null || roofingProduct != null || selectedTab != PosTab.REGISTER
    ) {
        when {
            roofingProduct != null -> viewModel.dismissRoofingCalculator()
            detailsProduct != null -> viewModel.dismissProductDetails()
            showCheckoutDialog -> showCheckoutDialog = false
            showCartSheet -> showCartSheet = false
            selectedTab != PosTab.REGISTER -> selectedTab = PosTab.REGISTER
        }
    }

    val isImeVisible = WindowInsets.isImeVisible

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isImeVisible) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    NavigationBarItem(
                        selected = selectedTab == PosTab.REGISTER,
                        onClick = { selectedTab = PosTab.REGISTER },
                        icon = { Icon(Icons.Default.Store, contentDescription = "အရောင်း") },
                        label = { Text("အရောင်း") },
                        modifier = Modifier.testTag("nav_register"),
                    )
                    NavigationBarItem(
                        selected = selectedTab == PosTab.INVENTORY,
                        onClick = { selectedTab = PosTab.INVENTORY },
                        icon = { Icon(Icons.Default.Inventory, contentDescription = "ပစ္စည်းစာရင်း") },
                        label = { Text("ပစ္စည်းစာရင်း") },
                        modifier = Modifier.testTag("nav_inventory"),
                    )
                    NavigationBarItem(
                        selected = selectedTab == PosTab.SALES,
                        onClick = { selectedTab = PosTab.SALES },
                        icon = { Icon(Icons.Default.Receipt, contentDescription = "ဘောက်ချာများ") },
                        label = { Text("ဘောက်ချာများ") },
                        modifier = Modifier.testTag("nav_sales"),
                    )
                    NavigationBarItem(
                        selected = selectedTab == PosTab.AI_COPILOT,
                        onClick = { selectedTab = PosTab.AI_COPILOT },
                        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI အကူ") },
                        label = { Text("AI အကူ") },
                        modifier = Modifier.testTag("nav_ai_copilot"),
                    )
                    NavigationBarItem(
                        selected = selectedTab == PosTab.FEATURES || selectedTab == PosTab.SETTINGS,
                        onClick = { selectedTab = PosTab.FEATURES },
                        icon = { Icon(Icons.Default.Category, contentDescription = "လုပ်ဆောင်ချက်") },
                        label = { Text("လုပ်ဆောင်ချက်") },
                        modifier = Modifier.testTag("nav_features"),
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            when (selectedTab) {
                PosTab.REGISTER -> HomeScreen(
                    viewModel = viewModel,
                    onOpenCart = { showCartSheet = true },
                )
                PosTab.INVENTORY -> InventoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { selectedTab = PosTab.REGISTER },
                )
                PosTab.SALES -> SalesHistoryScreen(
                    viewModel = viewModel,
                )
                PosTab.AI_COPILOT -> AiCopilotScreen(
                    viewModel = viewModel,
                    onNavigateBack = { selectedTab = PosTab.REGISTER },
                )
                PosTab.FEATURES -> FeaturesHomeScreen(
                    viewModel = viewModel,
                    onNavigateToInventory = { selectedTab = PosTab.INVENTORY },
                    onNavigateToSales = { selectedTab = PosTab.SALES },
                    onNavigateToSettings = { selectedTab = PosTab.SETTINGS },
                    onNavigateToAiCopilot = { selectedTab = PosTab.AI_COPILOT },
                )
                PosTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                )
            }
        }
    }

    // Cart Sheet Dialog
    if (showCartSheet) {
        CartSheet(
            items = cart,
            totals = totals,
            canApplyDiscount = actor.canApplyDiscount,
            taxRatePercent = storeProfile.taxRatePercent,
            customLogoPath = storeProfile.customLogoPath,
            onQuantityChange = { id, qty -> viewModel.updateQuantity(id, qty) },
            onRemoveItem = { id -> viewModel.removeFromCart(id) },
            onApplyDiscount = { id, disc -> viewModel.applyDiscount(id, disc) },
            onApplyPromoCode = { viewModel.applyPromoCodeToCart(it) },
            onToggleWholesale = { id -> viewModel.toggleCartItemWholesale(id) },
            onClearCart = { viewModel.clearCart() },
            onRequestManagerUnlock = {
                viewModel.setBanner("လျှော့စျေးကို တိုက်ရိုက်အသုံးပြုနိုင်ပါသည်")
            },
            onProceedToCheckout = {
                showCartSheet = false
                showCheckoutDialog = true
            },
            onDismiss = { showCartSheet = false },
        )
    }

    val customers by viewModel.customers.collectAsState()

    // Checkout Flow Dialog
    if (showCheckoutDialog) {
        CheckoutDialog(
            totals = totals,
            customLogoPath = storeProfile.customLogoPath,
            customers = customers,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { method, tendered, customer, splitCash, splitDigital, deliveryFee ->
                viewModel.checkout(
                    paymentMethod = method,
                    tenderedMinor = tendered,
                    deliveryFeeMinor = deliveryFee,
                    selectedCustomer = customer,
                    splitCashMinor = splitCash,
                    splitDigitalMinor = splitDigital,
                ) { success, msg ->
                    if (success) {
                        showCheckoutDialog = false
                    }
                    viewModel.setBanner(msg)
                }
            }
        )
    }

    // Receipt Modal Dialog
    receipt?.let { currentReceipt ->
        ReceiptDialog(
            receipt = currentReceipt,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissReceipt() },
        )
    }

    // Product Details Dialog (Shown on double tap from any search or inventory screen)
    val allCategories by viewModel.categories.collectAsState()
    detailsProduct?.let { product ->
        ProductDetailsDialog(
            product = product,
            categories = allCategories,
            onDismiss = { viewModel.dismissProductDetails() },
            onAddToCart = { viewModel.addToCart(product) },
            onAdjustStock = { qty -> viewModel.adjustStock(product.productId, qty) },
            onUpdateProduct = { updated -> viewModel.updateProduct(updated) },
            onSaveImage = { uri -> viewModel.saveImageFromGalleryUri(uri) },
        )
    }

    // Roofing Calculator Dialog
    roofingProduct?.let { product ->
        RoofingCalculatorDialog(
            product = product,
            onDismiss = { viewModel.dismissRoofingCalculator() },
            onConfirm = { lengthFeet, sheetQty, _ ->
                viewModel.addRoofingSheetToCart(product, lengthFeet, sheetQty)
            }
        )
    }
}
