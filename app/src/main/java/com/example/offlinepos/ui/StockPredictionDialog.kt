package com.example.offlinepos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.offlinepos.data.local.ProductEntity
import com.example.offlinepos.domain.StockPredictionItem
import com.example.offlinepos.domain.StockRiskLevel

@Composable
fun StockPredictionDialog(
    viewModel: PosViewModel,
    onOpenStockIn: (ProductEntity, Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val predictions by viewModel.stockPredictions.collectAsState()
    var filterLevel by remember { mutableStateOf<StockRiskLevel?>(null) }

    val criticalCount = predictions.count { it.riskLevel == StockRiskLevel.OUT_OF_STOCK || it.riskLevel == StockRiskLevel.CRITICAL }
    val warningCount = predictions.count { it.riskLevel == StockRiskLevel.WARNING }

    val displayedItems = predictions.filter {
        filterLevel == null || it.riskLevel == filterLevel
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("stock_prediction_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // Header with AI Sparkle Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "AI ကုန်ပစ္စည်းကြိုတင်မှာယူမှု ခန့်မှန်းချက်",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sales Velocity & Stock-out Prediction",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stat Overview Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("အရေးပေါ် (≤ ၃ ရက်)", fontSize = 11.sp, color = Color(0xFF991B1B))
                            Text("$criticalCount မျိုး", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("သတိပေး (≤ ၇ ရက်)", fontSize = 11.sp, color = Color(0xFF92400E))
                            Text("$warningCount မျိုး", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filterLevel == null,
                            onClick = { filterLevel = null },
                            label = { Text("အားလုံး (${predictions.size})", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterLevel == StockRiskLevel.CRITICAL,
                            onClick = { filterLevel = StockRiskLevel.CRITICAL },
                            label = { Text("🔴 အရေးပေါ်", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterLevel == StockRiskLevel.WARNING,
                            onClick = { filterLevel = StockRiskLevel.WARNING },
                            label = { Text("🟡 သတိပေး", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterLevel == StockRiskLevel.OUT_OF_STOCK,
                            onClick = { filterLevel = StockRiskLevel.OUT_OF_STOCK },
                            label = { Text("❌ ပြတ်နေသည်", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Prediction Items List
                if (displayedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✅", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "လက်ရှိတွင် ပစ္စည်းများ ပြတ်လပ်နိုင်ခြေ မရှိပါ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedItems, key = { it.product.productId }) { item ->
                            PredictionItemCard(
                                item = item,
                                onStockInClick = { onOpenStockIn(item.product, item.recommendedPackCount) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("ပိတ်မည်")
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionItemCard(
    item: StockPredictionItem,
    onStockInClick: () -> Unit,
) {
    val p = item.product
    val riskColor = Color(item.riskLevel.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = p.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "SKU: ${p.sku} | ${p.category}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = riskColor.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, riskColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = item.riskLevel.labelBurmese,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = riskColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Data Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("လက်ရှိလက်ကျန်", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${item.currentStock} ${p.unitName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.currentStock <= p.lowStockThreshold) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Text("တစ်ရက်ရောင်းရနှုန်း", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "%.1f %s/ရက်".format(item.dailyRunRate, p.unitName),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text("ပြတ်လပ်မည့်ရက်", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (item.daysRemaining >= 999) "ရောင်းအားမရှိ" else "${item.daysRemaining} ရက်အတွင်း",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = riskColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recommendation & Restock Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "အကြံပြုမှာယူရန်: +${item.recommendedPackCount} ${item.recommendedRestockUnit}" +
                                (if (item.recommendedRestockUnit != p.unitName) " (${item.recommendedRestockQty} ${p.unitName})" else ""),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "ခန့်မှန်းအရင်း: %,d Ks".format(item.estimatedRestockCostMinor),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onStockInClick,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("အဝင်သွင်းမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
