package com.example.offlinepos.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.offlinepos.data.local.ProductEntity
import com.example.offlinepos.domain.ProductDraft

/**
 * AIMS POS Product List Screen (ကုန်စည်များ)
 * Faithfully matches user-provided screenshot:
 * - TopAppBar: Back arrow, "ကုန်စည်များ", Grid toggle, Tag icon, Search
 * - Category filter pills: Elevated soft rose/salmon pill for "အားလုံး(X Items)" and grey pills for categories
 * - Product row: Crimson red rounded box with white 3D isometric cube, product title, price in Ks, soft salmon status dot
 * - Squircle light-grey Floating Action Button (+)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit = {},
) {
    val products by viewModel.allProducts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val predictions by viewModel.stockPredictions.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("အားလုံး") }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showStockPredictionDialog by remember { mutableStateOf(false) }
    var showStockInDialog by remember { mutableStateOf(false) }
    var stockInPreselectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var stockInSuggestedQty by remember { mutableStateOf(10L) }
    var productToDelete by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedProductForOptions by remember { mutableStateOf<ProductEntity?>(null) }

    val urgentPredictions = predictions.filter { (it.daysRemaining <= 7 && it.currentStock > 0) || it.currentStock <= 0 }
    val urgentCount = urgentPredictions.size

    val filteredList = products.filter {
        val matchesSearch = searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.sku.contains(searchQuery, ignoreCase = true) ||
                (it.barcode?.contains(searchQuery, ignoreCase = true) == true)
        val matchesCat = selectedCategoryFilter == "အားလုံး" || it.category.equals(selectedCategoryFilter, ignoreCase = true)
        matchesSearch && matchesCat
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("inventory_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                title = {
                    Text(
                        text = "ကုန်စည်များ",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                },
                actions = {
                    IconButton(
                        onClick = { isGridView = !isGridView },
                        modifier = Modifier.testTag("toggle_grid_view_button")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle Grid/List View",
                            tint = Color(0xFF334155)
                        )
                    }
                    IconButton(
                        onClick = { showCategoryDialog = true },
                        modifier = Modifier.testTag("category_filter_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sell,
                            contentDescription = "Categories",
                            tint = Color(0xFF334155)
                        )
                    }
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.testTag("toggle_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF334155)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    navigationIconContentColor = Color(0xFF1E293B),
                    titleContentColor = Color(0xFF1E293B),
                    actionIconContentColor = Color(0xFF334155),
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_new_product_fab"),
                containerColor = Color(0xFFE2E8F0),
                contentColor = Color(0xFF1E293B),
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Product",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            // Animated Search Input Bar
            AnimatedVisibility(visible = isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ကုန်စည် အမည် သို့မဟုတ် Code ဖြင့် ရှာရန်...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("inventory_search_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                    )
                )
            }

            // Category Filter Pills (AIMS soft salmon pill when selected, grey pills for others)
            val categoryList = remember(categories, products) {
                listOf("အားလုံး") + (categories + products.map { it.category }).filter { it.isNotBlank() }.distinct()
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(categoryList) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    val pillLabel = if (cat == "အားလုံး") "အားလုံး(${products.size} Items)" else cat
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEAA6A6) else Color(0xFFE5E7EB),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategoryFilter = cat }
                            .testTag("category_pill_$cat")
                    ) {
                        Text(
                            text = pillLabel,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF3B1D1D) else Color(0xFF4B5563),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Product List or Grid
            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .testTag("inventory_grid"),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filteredList, key = { "${it.productId}_${it.sku}" }) { product ->
                        AimsProductGridCard(
                            product = product,
                            onClick = { viewModel.openProductDetails(product) },
                            onLongClick = { selectedProductForOptions = product },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("inventory_list"),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(filteredList, key = { "${it.productId}_${it.sku}" }) { product ->
                        AimsProductListItem(
                            product = product,
                            onClick = { viewModel.openProductDetails(product) },
                            onLongClick = { selectedProductForOptions = product },
                        )
                    }
                }
            }
        }
    }

    // Category Filter Selection Dialog (triggered by Tag Icon in TopAppBar)
    if (showCategoryDialog) {
        val categoryList = remember(categories, products) {
            listOf("အားလုံး") + (categories + products.map { it.category }).filter { it.isNotBlank() }.distinct()
        }
        Dialog(onDismissRequest = { showCategoryDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "အမျိုးအစား ရွေးချယ်ရန် (Categories)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categoryList) { cat ->
                            val count = if (cat == "အားလုံး") products.size else products.count { it.category.equals(cat, ignoreCase = true) }
                            val isSelected = selectedCategoryFilter == cat
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFEAA6A6) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedCategoryFilter = cat
                                        showCategoryDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF3B1D1D) else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "$count မျိုး",
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color(0xFF3B1D1D) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { showCategoryDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("ပိတ်မည်")
                    }
                }
            }
        }
    }

    // Long-Press Product Actions Dialog
    selectedProductForOptions?.let { prod ->
        Dialog(onDismissRequest = { selectedProductForOptions = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = prod.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "လက်ကျန်: ${prod.stockQuantity} ${prod.unitName} | ဈေး: %,d Ks".format(prod.sellPriceMinor),
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            selectedProductForOptions = null
                            stockInPreselectedProduct = prod
                            stockInSuggestedQty = 10L
                            showStockInDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF16A34A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📥 အဝင်စတော့ သွင်းမည် (Stock-In)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            selectedProductForOptions = null
                            viewModel.openProductDetails(prod)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("✏️ အချက်အလက် ကြည့်ရှု / ပြင်ဆင်မည်")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            selectedProductForOptions = null
                            productToDelete = prod
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🗑️ ကုန်ပစ္စည်း ဖျက်မည်", color = Color.White)
                    }
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

@Composable
fun AimsIsometricCube(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(Color(0xFFDC2626), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val r = w * 0.44f
            val gap = 1.2.dp.toPx()

            // Isometric 3D cube top face (diamond)
            val topPath = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r * 0.866f, cy - r * 0.5f)
                lineTo(cx, cy)
                lineTo(cx - r * 0.866f, cy - r * 0.5f)
                close()
            }
            // Left face
            val leftPath = Path().apply {
                moveTo(cx - r * 0.866f, cy - r * 0.5f + gap)
                lineTo(cx - gap * 0.5f, cy + gap * 0.5f)
                lineTo(cx - gap * 0.5f, cy + r - gap * 0.5f)
                lineTo(cx - r * 0.866f, cy + r * 0.5f)
                close()
            }
            // Right face
            val rightPath = Path().apply {
                moveTo(cx + gap * 0.5f, cy + gap * 0.5f)
                lineTo(cx + r * 0.866f, cy - r * 0.5f + gap)
                lineTo(cx + r * 0.866f, cy + r * 0.5f)
                lineTo(cx + gap * 0.5f, cy + r - gap * 0.5f)
                close()
            }

            drawPath(topPath, Color.White)
            drawPath(leftPath, Color.White.copy(alpha = 0.92f))
            drawPath(rightPath, Color.White.copy(alpha = 0.85f))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AimsProductListItem(
    product: ProductEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .testTag("product_row_${product.productId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Crimson Red rounded container with white 3D isometric cube
            AimsIsometricCube()

            Spacer(modifier = Modifier.width(14.dp))

            // Product name and price in Ks
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "%,d Ks".format(product.sellPriceMinor),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Soft salmon/rose pink status dot as shown in screenshot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (product.isActive) Color(0xFFEAA6A6) else Color(0xFFCBD5E1),
                        shape = CircleShape
                    )
            )
        }

        // Hairline divider starting after red box
        HorizontalDivider(
            color = Color(0xFFF1F5F9),
            thickness = 1.dp,
            modifier = Modifier.padding(start = 74.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AimsProductGridCard(
    product: ProductEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .testTag("product_grid_${product.productId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                AimsIsometricCube(modifier = Modifier.size(38.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (product.isActive) Color(0xFFEAA6A6) else Color(0xFFCBD5E1),
                            shape = CircleShape
                        )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "%,d Ks".format(product.sellPriceMinor),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
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
