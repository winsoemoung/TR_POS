package com.example.offlinepos.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import com.example.offlinepos.R
import com.example.offlinepos.data.local.ProductEntity
import com.example.offlinepos.domain.ProductDraft
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
) {
    val products by viewModel.allProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val predictions by viewModel.stockPredictions.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("အားလုံး") }
    var selectedStatusFilter by remember { mutableStateOf("အားလုံး") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showStockPredictionDialog by remember { mutableStateOf(false) }
    var showStockInDialog by remember { mutableStateOf(false) }
    var stockInPreselectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var stockInSuggestedQty by remember { mutableStateOf(10L) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var recentSearches by remember { mutableStateOf(RecentSearchHelper.getRecentSearches(context)) }
    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(viewModel::exportProductsCsv) }
    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::importProductsCsv) }

    val urgentPredictions = predictions.filter { (it.daysRemaining <= 7 && it.currentStock > 0) || it.currentStock <= 0 }
    val urgentCount = urgentPredictions.size

    val filteredList = products.filter {
        val matchesSearch = searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.sku.contains(searchQuery, ignoreCase = true)
        val matchesCat = selectedCategoryFilter == "အားလုံး" || it.category.equals(selectedCategoryFilter, ignoreCase = true)
        val matchesStatus = when (selectedStatusFilter) {
            "Active" -> it.isActive
            "Inactive" -> !it.isActive
            else -> true
        }
        matchesSearch && matchesCat && matchesStatus
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Management", fontWeight = FontWeight.Bold) },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        // AI Stock Prediction Action with Urgency Badge
                        IconButton(
                            onClick = { showStockPredictionDialog = true },
                            modifier = Modifier.testTag("inventory_ai_prediction_button")
                        ) {
                            Box {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "AI Stock Prediction",
                                    tint = if (urgentCount > 0) Color(0xFFEA580C) else MaterialTheme.colorScheme.primary
                                )
                                if (urgentCount > 0) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.size(15.dp).align(Alignment.TopEnd)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("$urgentCount", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Direct Stock-In Button
                        OutlinedButton(
                            onClick = {
                                stockInPreselectedProduct = null
                                stockInSuggestedQty = 10L
                                showStockInDialog = true
                            },
                            modifier = Modifier.height(32.dp).testTag("inventory_stock_in_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📥 အဝင် (Stock-In)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(onClick = { importCsvLauncher.launch(arrayOf("text/csv", "text/plain")) }) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Import CSV")
                        }
                        IconButton(onClick = { exportCsvLauncher.launch("products.csv") }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                        }
                        StoreLogoAvatar(
                            customLogoPath = storeProfile.customLogoPath,
                            contentDescription = "Inventory Manager",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_new_product_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (it.trim().length >= 2) {
                        recentSearches = RecentSearchHelper.addSearchTerm(context, it)
                    }
                },
                placeholder = { Text("Name or SKU") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inventory_search_field"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            // Recent Searches Chips (stores last 5 search terms locally)
            if (recentSearches.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = "🕒 ရှာဖွေမှုများ:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(recentSearches) { term ->
                            androidx.compose.material3.SuggestionChip(
                                onClick = {
                                    searchQuery = term
                                    recentSearches = RecentSearchHelper.addSearchTerm(context, term)
                                },
                                label = { Text(term, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            RecentSearchHelper.clearSearches(context)
                            recentSearches = emptyList()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear recent searches",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Category Filter pills
            val catPills = listOf("အားလုံး") + categories
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(catPills) { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(cat, fontSize = 12.sp) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("အားလုံး", "Active", "Inactive").forEach { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = { selectedStatusFilter = status },
                        label = { Text(status, fontSize = 12.sp) },
                    )
                }
            }

            // Inventory Summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ပစ္စည်း စုစုပေါင်း", style = MaterialTheme.typography.labelSmall)
                        Text("${products.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Active", style = MaterialTheme.typography.labelSmall)
                        Text("${products.count { it.isActive }}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Inactive", style = MaterialTheme.typography.labelSmall)
                        Text("${products.count { !it.isActive }}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("လက်ကျန် စုစုပေါင်း", style = MaterialTheme.typography.labelSmall)
                        Text("${products.sumOf { it.stockQuantity }}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("လက်ကျန်နည်း ပစ္စည်း", style = MaterialTheme.typography.labelSmall)
                        Text(
                            "${products.count { it.stockQuantity <= it.lowStockThreshold }}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            // AI Stock Prediction Alert Banner
            if (urgentCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("ai_stock_prediction_alert_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDBA74)))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF97316).copy(alpha = 0.15f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "⚡ AI Stock Prediction သတိပေးချက်",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF9A3412)
                                )
                                Text(
                                    text = "လာမည့် ၃ ရက်မှ ၇ ရက်အတွင်း ပြတ်လပ်နိုင်သော ပစ္စည်း ($urgentCount) မျိုး ရှိပါသည်",
                                    fontSize = 11.sp,
                                    color = Color(0xFFC2410C)
                                )
                            }
                        }
                        Button(
                            onClick = { showStockPredictionDialog = true },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ခန့်မှန်းချက်", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "💡 Product name ကို Double Tap နှိပ်၍ အသေးစိတ် အချက်အလက် ကြည့်ရှုနိုင်ပါသည်",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Products List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.testTag("inventory_list"),
            ) {
                items(filteredList, key = { "${it.productId}_${it.sku}" }) { product ->
                    val prodPrediction = predictions.find { it.product.productId == product.productId }
                    InventoryItemCard(
                        product = product,
                        prediction = prodPrediction,
                        onDoubleTap = { viewModel.openProductDetails(product) },
                        onStockIn = {
                            stockInPreselectedProduct = product
                            stockInSuggestedQty = prodPrediction?.recommendedPackCount ?: 10L
                            showStockInDialog = true
                        },
                        onAdjustStock = { qty -> viewModel.adjustStock(product.productId, qty) },
                        onArchive = { productToDelete = product },
                        onActivate = { viewModel.activateProduct(product.productId) },
                    )
                }
            }
        }
    }

    // Confirm Delete Dialog for Products
    productToDelete?.let { prod ->
        ConfirmDeleteDialog(
            title = "🗑️ ကုန်ပစ္စည်း ဖျက်ရန် အတည်ပြုပါ",
            message = "ကုန်ပစ္စည်း [${prod.name}] ကို စာရင်းမှ ဖျက်ပစ်ရန် သေချာပါသလား? ဤလုပ်ဆောင်ချက်ကို ပြန်ပြင်၍ မရနိုင်ပါ။",
            confirmText = "ဖျက်မည်",
            cancelText = "မလုပ်တော့ပါ",
            onConfirm = {
                viewModel.archiveProduct(prod.productId)
                productToDelete = null
            },
            onDismiss = { productToDelete = null }
        )
    }

    if (showAddDialog) {
        AddProductDialog(
            categories = categories,
            onSaveImage = { uri -> viewModel.saveImageFromGalleryUri(uri) },
            onDismiss = { showAddDialog = false },
            onAdd = { draft, sku ->
                viewModel.addNewProduct(draft, sku) { success, msg ->
                    if (success) {
                        showAddDialog = false
                    }
                    viewModel.setBanner(msg)
                }
            }
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
            onDismiss = { showStockInDialog = false }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InventoryItemCard(
    product: ProductEntity,
    prediction: com.example.offlinepos.domain.StockPredictionItem? = null,
    onDoubleTap: () -> Unit,
    onStockIn: () -> Unit,
    onAdjustStock: (Long) -> Unit,
    onArchive: () -> Unit,
    onActivate: () -> Unit,
) {
    val isLowStock = product.stockQuantity <= product.lowStockThreshold

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onDoubleTap,
                onDoubleClick = onDoubleTap,
            )
            .testTag("inventory_item_${product.productId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProductVisualHelper.ProductVisual(
                    product = product,
                    modifier = Modifier.size(54.dp),
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "အမျိုးအစား: ${product.category} | SKU: ${product.sku}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (product.isActive) "အခြေအနေ: Active" else "အခြေအနေ: Inactive",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.isActive) Color(0xFF15803D) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = if (product.isRoofingSheet) "ရောင်း: %,d Ks/ပေ | ဝယ်: %,d Ks/ပေ".format(product.sellPriceMinor, product.costPriceMinor)
                        else "ရောင်း: %,d Ks | ဝယ်: %,d Ks".format(product.sellPriceMinor, product.costPriceMinor),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (product.wholesaleUnitMultiplier > 1L || product.wholesalePriceMinor > 0L) {
                        Text(
                            text = "📦 ၁ ${product.wholesaleUnitName} = ${product.wholesaleUnitMultiplier} ${product.unitName} (လက်ကား: %,d Ks)".format(product.wholesalePriceMinor),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F766E),
                        )
                    }
                    if (prediction != null && (prediction.daysRemaining <= 7 || prediction.currentStock <= 0)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(prediction.riskLevel.colorHex).copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = if (prediction.daysRemaining in 1..7) "⚠️ ${prediction.daysRemaining} ရက်အတွင်း ပြတ်မည်" else prediction.riskLevel.labelBurmese,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(prediction.riskLevel.colorHex),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (product.isActive) {
                    IconButton(
                        onClick = onArchive,
                        modifier = Modifier.testTag("archive_product_${product.productId}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Archive", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                } else {
                    TextButton(onClick = onActivate, modifier = Modifier.testTag("activate_product_${product.productId}")) {
                        Text("ပြန်သုံးမည်", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Stock adjustments
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "လက်ကျန်: ",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isLowStock) Color(0xFFFEF2F2) else Color(0xFFDCFCE7),
                    ) {
                        Text(
                            text = "${product.stockQuantity} ${product.unitName}${if (isLowStock) " (⚠️သတိပေး)" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isLowStock) Color(0xFFDC2626) else Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onStockIn,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("အဝင် +", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onAdjustStock(-1) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp),
                    ) {
                        Text("-1", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { onAdjustStock(5) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp),
                    ) {
                        Text("+5", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { onAdjustStock(10) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp),
                    ) {
                        Text("+10", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddProductDialog(
    categories: List<String>,
    onSaveImage: (Uri) -> String?,
    onDismiss: () -> Unit,
    onAdd: (draft: ProductDraft, sku: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var barcode by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("20") }
    var lowStockThreshold by remember { mutableStateOf("3") }
    var selectedCategory by remember { mutableStateOf(if (categories.isNotEmpty()) categories.first() else "ဘိလပ်မြေ") }
    var newCategoryInput by remember { mutableStateOf("") }
    var unitName by remember { mutableStateOf("ခု") }
    var isRoofingSheet by remember { mutableStateOf(false) }
    var hasWholesale by remember { mutableStateOf(false) }
    var wholesaleUnitName by remember { mutableStateOf("ဖာ") }
    var wholesaleMultiplier by remember { mutableStateOf("12") }
    var wholesalePrice by remember { mutableStateOf("") }
    var wholesaleMinQty by remember { mutableStateOf("1") }
    var pickedImageUri by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .testTag("add_product_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "ကုန်ပစ္စည်း အသစ် ထည့်သွင်းခြင်း",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Camera and Gallery Image Picker
                ProductImagePicker(
                    imagePath = pickedImageUri,
                    onImageSelected = { pickedImageUri = it },
                    onSaveGalleryUri = onSaveImage,
                    modifier = Modifier.padding(bottom = 10.dp),
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Category Grouping
                Text("အမျိုးအစား အုပ်စု (Category Group) *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val existingAndDefaults = (listOf("ဘိလပ်မြေ", "သွပ်ပြား", "သံချောင်း", "ဆေးသုတ်", "ပိုက်နှင့်ဆက်စပ်", "သံမှို / ကိရိယာ") + categories).distinct()

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(existingAndDefaults) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat && newCategoryInput.isBlank(),
                            onClick = {
                                selectedCategory = cat
                                newCategoryInput = ""
                                if (cat == "သွပ်ပြား") isRoofingSheet = true
                            },
                            label = { Text(cat, fontSize = 11.sp) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = newCategoryInput,
                    onValueChange = {
                        newCategoryInput = it
                        if (it.isNotBlank()) selectedCategory = it.trim()
                    },
                    label = { Text("သို့မဟုတ် အမျိုးအစားအသစ် ရိုက်ထည့်ရန် (ဥပမာ- ဘိလပ်မြေ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_category_input"),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Roofing Sheet Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .padding(vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = isRoofingSheet,
                        onCheckedChange = {
                            isRoofingSheet = it
                            if (it) unitName = "ချပ်"
                        },
                        modifier = Modifier.testTag("roofing_checkbox"),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("သွပ်ပြား (Roofing Sheet)", fontWeight = FontWeight.Bold)
                        Text("တစ်ပေ စျေးနှုန်းဖြင့် ပေအရှည် x ချပ်ရေ တွက်ချက်မည်", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Unit Selection & Input
                Text("ယူနစ် သတ်မှတ်ရန် (Unit) *", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                val commonUnits = listOf("ခု", "အိတ်", "ပိဿာ", "ဗူး", "ချပ်", "လိပ်", "တန်", "ထုပ်", "စုံ", "ပေ", "ကီလို")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    items(commonUnits) { u ->
                        FilterChip(
                            selected = unitName == u,
                            onClick = { unitName = u },
                            label = { Text(u, fontSize = 11.sp) },
                        )
                    }
                }
                OutlinedTextField(
                    value = unitName,
                    onValueChange = { unitName = it },
                    label = { Text("ယူနစ် စာသား (ဥပမာ - အိတ်၊ ပိဿာ၊ ဗူး၊ ခု)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_unit_input"),
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isRoofingSheet) "သွပ်ပြား အမည် (ဥပမာ- သွပ် အနီရောင်) *" else "ပစ္စည်း အမည် (ဥပမာ- Crown ဘိလပ်မြေ) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU / ကုဒ်") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("product_sku_input"),
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_barcode_input"),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isRoofingSheet) "ဝယ်စျေး (Ks / ပေ)" else "ဝယ်စျေး (Ks)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_cost_input"),
                    )
                    OutlinedTextField(
                        value = sellPrice,
                        onValueChange = { sellPrice = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isRoofingSheet) "ရောင်းစျေး (Ks / ပေ) *" else "ရောင်းစျေး (Ks) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_sell_input"),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it.filter { ch -> ch.isDigit() } },
                        label = { Text(if (isRoofingSheet) "လက်ကျန် (ချပ်ရေ)" else "လက်ကျန် (${unitName.ifBlank { "ခု" }})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_stock_input"),
                    )
                    OutlinedTextField(
                        value = lowStockThreshold,
                        onValueChange = { lowStockThreshold = it.filter { ch -> ch.isDigit() } },
                        label = { Text("သတိပေး လက်ကျန် (${unitName.ifBlank { "ခု" }})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_low_stock_input"),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Wholesale Multi-Unit Packaging Option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).padding(vertical = 4.dp),
                ) {
                    Checkbox(
                        checked = hasWholesale,
                        onCheckedChange = { hasWholesale = it },
                        modifier = Modifier.testTag("wholesale_checkbox"),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("လက်ကား / ဖာလိုက် ယူနစ်ခွဲ သတ်မှတ်မည်", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("၁ ဖာ = ၁၂ ခု စသဖြင့် ဖာလိုက်စျေးနှင့် စတော့ အလိုအလျောက် နုတ်ယူရန်", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                if (hasWholesale) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = wholesaleUnitName,
                            onValueChange = { wholesaleUnitName = it },
                            label = { Text("လက်ကားယူနစ် (ဥပမာ- ဖာ)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = wholesaleMultiplier,
                            onValueChange = { wholesaleMultiplier = it.filter { ch -> ch.isDigit() } },
                            label = { Text("၁ ဖာဝင် အရေအတွက်") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = wholesalePrice,
                            onValueChange = { wholesalePrice = it.filter { ch -> ch.isDigit() } },
                            label = { Text("လက်ကားစျေး (Ks / ဖာ)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = wholesaleMinQty,
                            onValueChange = { wholesaleMinQty = it.filter { ch -> ch.isDigit() } },
                            label = { Text("အနည်းဆုံး ဖာရေ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("မလုပ်တော့ပါ")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cost = costPrice.toLongOrNull() ?: 0L
                            val sell = sellPrice.toLongOrNull() ?: 0L
                            val stockQty = stock.toLongOrNull() ?: 0L
                            val lowStock = lowStockThreshold.toLongOrNull() ?: 3L
                            val barcodeVal = barcode.trim().takeIf { it.isNotEmpty() }
                            val catVal = if (newCategoryInput.isNotBlank()) newCategoryInput.trim() else selectedCategory
                            val wsPrice = if (hasWholesale) (wholesalePrice.toLongOrNull() ?: 0L) else 0L
                            val wsMin = if (hasWholesale) (wholesaleMinQty.toLongOrNull() ?: 0L) else 0L
                            val wsMult = if (hasWholesale) maxOf(wholesaleMultiplier.toLongOrNull() ?: 1L, 1L) else 1L
                            val wsName = if (hasWholesale) wholesaleUnitName.trim().ifBlank { "ဖာ" } else "ဖာ"

                            val draft = ProductDraft(
                                name = name.trim(),
                                barcode = barcodeVal,
                                costPriceMinor = cost,
                                sellPriceMinor = sell,
                                stockQuantity = stockQty,
                                category = catVal,
                                isRoofingSheet = isRoofingSheet,
                                lowStockThreshold = lowStock,
                                imageUri = pickedImageUri,
                                unitName = unitName.trim().ifBlank { "ခု" },
                                wholesalePriceMinor = wsPrice,
                                wholesaleMinQty = wsMin,
                                wholesaleUnitName = wsName,
                                wholesaleUnitMultiplier = wsMult,
                            )
                            val errors = draft.validate()
                            if (errors.isNotEmpty()) {
                                errorMessage = errors.first()
                            } else {
                                onAdd(draft, sku.trim())
                            }
                        },
                        modifier = Modifier.testTag("save_product_button")
                    ) {
                        Text("သိမ်းဆည်းမည်")
                    }
                }
            }
        }
    }
}
