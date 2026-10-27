package com.example.offlinepos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsBusinessSubpage(
    viewModel: PosViewModel,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showCandlestickChart by remember {
        mutableStateOf(context.getSharedPreferences("pos_settings", 0).getBoolean("show_sales_candlestick", false))
    }

    var showCustomerDialog by remember { mutableStateOf(false) }
    var showStockInDialog by remember { mutableStateOf(false) }
    var showShiftReportDialog by remember { mutableStateOf(false) }

    val storeConfig = remember { StoreConfig(context) }
    val promoCodes = remember { storeConfig.getPromoCodes() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Candlestick Chart Preference Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("📈 နေ့စဉ်/လစဉ် အရောင်း Candlestick", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("အရောင်းမှတ်တမ်းတွင် အတက်/အကျ အရောင်ပါသော Daily & Monthly chart ပြသမည်", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = showCandlestickChart,
                    onCheckedChange = {
                        showCandlestickChart = it
                        context.getSharedPreferences("pos_settings", 0).edit().putBoolean("show_sales_candlestick", it).apply()
                    },
                    modifier = Modifier.testTag("settings_candlestick_switch"),
                )
            }
        }

        // Quick Business Management Hub
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🏢 လုပ်ငန်းစီမံခန့်ခွဲမှု အမြန်လမ်းကြောင်း (Quick Hub)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCustomerDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("👥 ဝယ်သူ & အကြွေး", fontSize = 11.5.sp)
                    }
                    OutlinedButton(
                        onClick = { showStockInDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("📦 ပစ္စည်းအဝင်", fontSize = 11.5.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showShiftReportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("📑 နေ့ချုပ်ပိတ်သိမ်းခြင်း (Z-Report စစ်ဆေးမည်)", fontSize = 13.sp)
                }
            }
        }

        // Promo Codes & VIP Discounts Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🎟️ ပရိုမိုးရှင်း ကူပွန်နှင့် VIP လျှော့ဈေးများ (Promo Codes)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("ငွေရှင်းချိန်တွင် ရိုက်ထည့်နိုင်သော လျှော့ဈေးကုဒ်များ စာရင်း:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                promoCodes.forEach { p ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("• ${p.code}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                Text(p.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                val discLabel = if (p.discountPercent > 0) "${p.discountPercent}% Off" else "${"%,d".format(p.discountFlatMinor)} Ks Off"
                                Text(
                                    text = discLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }

    if (showCustomerDialog) {
        CustomerManagementDialog(viewModel = viewModel, onDismiss = { showCustomerDialog = false })
    }
    if (showStockInDialog) {
        StockInDialog(viewModel = viewModel, onDismiss = { showStockInDialog = false })
    }
    if (showShiftReportDialog) {
        ShiftReportDialog(viewModel = viewModel, onDismiss = { showShiftReportDialog = false })
    }
}
