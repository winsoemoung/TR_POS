package com.example.offlinepos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.offlinepos.data.local.PurchaseEntity
import com.example.offlinepos.data.local.SupplierEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SuppliersManagementDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onNavigateToStockInForSupplier: (supplierName: String) -> Unit = {},
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val purchases by viewModel.purchases.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSupplierForDetails by remember { mutableStateOf<SupplierEntity?>(null) }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else suppliers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.shopName.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery, ignoreCase = true) ||
                    it.address.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("suppliers_management_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ကုန်သွင်းသူများ (Suppliers)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "စုစုပေါင်း: ${suppliers.size} ဦး",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_suppliers_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search & Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_supplier_input"),
                        placeholder = { Text("အမည်၊ ဆိုင်၊ ဖုန်းဖြင့် ရှာရန်...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            editingSupplier = null
                            showAddDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("add_supplier_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("အသစ်ထည့်", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Suppliers List
                if (filteredSuppliers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "ရှာဖွေမှုနှင့် ကိုက်ညီသော ကုန်သွင်းသူ မရှိပါ" else "ကုန်သွင်းသူ စာရင်း မရှိသေးပါ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSuppliers, key = { it.supplierId }) { supplier ->
                            val supplierPurchases = purchases.filter {
                                it.supplierName.equals(supplier.name, ignoreCase = true) ||
                                        (supplier.shopName.isNotBlank() && it.supplierName.equals(supplier.shopName, ignoreCase = true))
                            }
                            val totalPurchaseAmount = supplierPurchases.sumOf { it.totalCostMinor }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSupplierForDetails = supplier }
                                    .testTag("supplier_card_${supplier.supplierId}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = supplier.shopName.ifBlank { supplier.name },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (supplier.shopName.isNotBlank() && supplier.name.isNotBlank()) {
                                                Text(
                                                    text = "ကိုယ်စားလှယ်: ${supplier.name}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingSupplier = supplier
                                                    showAddDialog = true
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteSupplier(supplier.supplierId) { _, msg ->
                                                        viewModel.setBanner(msg)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (supplier.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(supplier.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    if (supplier.address.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(supplier.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ဝယ်ယူမှု: ${supplierPurchases.size} ကြိမ် (%,d Ks)".format(totalPurchaseAmount),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF15803D)
                                        )
                                        Text(
                                            text = "အသေးစိတ်ကြည့်ရန် →",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Supplier Dialog
    if (showAddDialog) {
        SupplierEditDialog(
            supplier = editingSupplier,
            onDismiss = { showAddDialog = false },
            onSave = { entity ->
                viewModel.saveSupplier(entity) { _, msg ->
                    viewModel.setBanner(msg)
                    showAddDialog = false
                }
            }
        )
    }

    // Supplier Details & Purchases View Dialog
    selectedSupplierForDetails?.let { supplier ->
        val supplierPurchases = purchases.filter {
            it.supplierName.equals(supplier.name, ignoreCase = true) ||
                    (supplier.shopName.isNotBlank() && it.supplierName.equals(supplier.shopName, ignoreCase = true))
        }

        SupplierDetailSheetDialog(
            supplier = supplier,
            purchases = supplierPurchases,
            onDismiss = { selectedSupplierForDetails = null },
            onStockIn = {
                selectedSupplierForDetails = null
                onDismiss()
                onNavigateToStockInForSupplier(supplier.shopName.ifBlank { supplier.name })
            }
        )
    }
}

@Composable
fun SupplierEditDialog(
    supplier: SupplierEntity?,
    onDismiss: () -> Unit,
    onSave: (SupplierEntity) -> Unit,
) {
    var name by remember { mutableStateOf(supplier?.name ?: "") }
    var shopName by remember { mutableStateOf(supplier?.shopName ?: "") }
    var phone by remember { mutableStateOf(supplier?.phone ?: "") }
    var email by remember { mutableStateOf(supplier?.email ?: "") }
    var address by remember { mutableStateOf(supplier?.address ?: "") }
    var taxNumber by remember { mutableStateOf(supplier?.taxNumber ?: "") }
    var note by remember { mutableStateOf(supplier?.note ?: "") }

    val isInputValid = name.isNotBlank() || shopName.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .testTag("supplier_edit_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (supplier == null) "ကုန်သွင်းသူ အသစ်မှတ်တမ်းတင်ရန်" else "ကုန်သွင်းသူ အချက်အလက် ပြင်ဆင်ရန်",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("ဆိုင် / ကုမ္ပဏီ အမည် (Shop / Company)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_shop_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("ကိုယ်စားလှယ် / ဆိုင်ရှင် အမည် (Contact Person) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("ဖုန်းနံပါတ် (Phone)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_phone_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("အီးမေးလ် (Email)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("supplier_email_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ဆိုင်လိပ်စာ (Address)") },
                    modifier = Modifier.fillMaxWidth().testTag("supplier_address_input")
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("မှတ်ချက် (ဥပမာ- လက်ကားစျေးနှုန်း ရရှိသည်)") },
                    modifier = Modifier.fillMaxWidth().testTag("supplier_note_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ပယ်ဖျက်မည်")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val entity = (supplier ?: SupplierEntity(name = "")).copy(
                                name = name.trim().ifBlank { shopName.trim() },
                                shopName = shopName.trim(),
                                phone = phone.trim(),
                                email = email.trim(),
                                address = address.trim(),
                                taxNumber = taxNumber.trim(),
                                note = note.trim(),
                            )
                            onSave(entity)
                        },
                        enabled = isInputValid,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_supplier_button")
                    ) {
                        Text("သိမ်းဆည်းမည်")
                    }
                }
            }
        }
    }
}

@Composable
fun SupplierDetailSheetDialog(
    supplier: SupplierEntity,
    purchases: List<PurchaseEntity>,
    onDismiss: () -> Unit,
    onStockIn: () -> Unit,
) {
    val totalSpend = purchases.sumOf { it.totalCostMinor }
    val totalItemsCount = purchases.sumOf { it.quantity }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .testTag("supplier_detail_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = supplier.shopName.ifBlank { supplier.name },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "ကုန်သွင်းသူ အသေးစိတ်နှင့် ဝယ်ယူမှုမှတ်တမ်း",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Supplier Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("👤 ကိုယ်စားလှယ်: ${supplier.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        if (supplier.phone.isNotBlank()) Text("📞 ဖုန်း: ${supplier.phone}", fontSize = 12.sp)
                        if (supplier.email.isNotBlank()) Text("✉️ အီးမေးလ်: ${supplier.email}", fontSize = 12.sp)
                        if (supplier.address.isNotBlank()) Text("📍 လိပ်စာ: ${supplier.address}", fontSize = 12.sp)
                        if (supplier.note.isNotBlank()) Text("📝 မှတ်ချက်: ${supplier.note}", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spend Summary Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("စုစုပေါင်း အဝယ်ငွေ", fontSize = 11.sp, color = Color(0xFF15803D))
                            Text("%,d Ks".format(totalSpend), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF166534))
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("အဝယ်အရေအတွက်", fontSize = 11.sp, color = Color(0xFF1D4ED8))
                            Text("${purchases.size} ကြိမ် ($totalItemsCount ခု)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E40AF))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Purchased Items History List
                Text(
                    text = "📦 ဝယ်ယူခဲ့သော ပစ္စည်းများ စာရင်း:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (purchases.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("ဤကုန်သွင်းသူထံမှ အဝယ်မှတ်တမ်း မရှိသေးပါ", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        purchases.take(15).forEach { purchase ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = purchase.productName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "${dateFormat.format(Date(purchase.createdAtEpochMs))} • ${purchase.quantity} ခု x %,d Ks".format(purchase.unitCostMinor),
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                        if (purchase.invoiceRef.isNotBlank()) {
                                            Text(
                                                text = "Invoice: ${purchase.invoiceRef}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "%,d Ks".format(purchase.totalCostMinor),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ပိတ်မည်")
                    }

                    Button(
                        onClick = onStockIn,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ပစ္စည်း အသစ်သွင်းမည် (Stock In)")
                    }
                }
            }
        }
    }
}
