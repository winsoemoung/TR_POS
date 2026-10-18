package com.example.offlinepos.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlinepos.hardware.QrCodeHelper

@Composable
fun SettingsServerSubpage(
    viewModel: PosViewModel,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isServerRunning by viewModel.isServerRunning.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val serverIp by viewModel.serverIp.collectAsState()
    val receivedCount by viewModel.serverReceivedCount.collectAsState()
    var showServerDialog by remember { mutableStateOf(false) }

    val serverQrBitmap = remember(serverUrl, isServerRunning) {
        if (isServerRunning && serverUrl.isNotBlank()) {
            QrCodeHelper.generateQrBitmap(serverUrl, 260)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Server Status Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isServerRunning) Color(0xFFDCFCE7).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Wifi,
                                    contentDescription = "Server Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("🌐 Main POS Server (15C)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Direct HTML5 Web Portal စနစ်", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isServerRunning) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    ) {
                        Text(
                            text = if (isServerRunning) "📡 Server ON" else "📡 Server OFF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isServerRunning) Color(0xFF15803D) else Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "ဖုန်းထဲတွင် App မသွင်းလိုသော ဆိုင်ဝန်ထမ်းများအတွက် Wi-Fi တစ်ခုတည်း ချိတ်ဆက်ထားရုံဖြင့် Chrome/Browser မှတစ်ဆင့် Server IP သို့ ဝင်ရောက်၍ ကုန်ပစ္စည်းလှမ်းထည့်သည့် စနစ်ဖြစ်ပါသည်",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isServerRunning) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (isServerRunning) viewModel.stopLocalServer() else viewModel.startLocalServer()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isServerRunning) "🟢 Server Online (လည်ပတ်နေပါသည်)" else "🔴 Server ရပ်တန့်ထားပါသည်",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isServerRunning) Color(0xFF15803D) else Color(0xFFB91C1C)
                            )
                            if (isServerRunning) {
                                Text(
                                    text = "Web Portal: $serverUrl",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF166534),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "ဝန်ထမ်းထည့်သွင်းမှု: $receivedCount ခု လက်ခံရရှိပြီး",
                                    fontSize = 11.sp,
                                    color = Color(0xFF15803D)
                                )
                            } else {
                                Text(
                                    text = "နှိပ်၍ Server ချက်ချင်း ပြန်လည်ဖွင့်လှစ်ပါ",
                                    fontSize = 11.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                if (isServerRunning) viewModel.stopLocalServer() else viewModel.startLocalServer()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isServerRunning) "ရပ်တန့်မည်" else "စတင်ဖွင့်မည်", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Web Portal Quick Links & QR Code Card
        if (isServerRunning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📱 ဝန်ထမ်းများ Portal ဝင်ရောက်ရန် လင့်ခ် & QR Code", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("POS Web Portal URL", serverUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Portal Link ကို Copy ကူးပြီးပါပြီ:\n$serverUrl", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("📋 Link ကူးယူမည်", fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Browser ဖွင့်မရပါ: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("🌐 Web Portal ဖွင့်မည်", fontSize = 11.5.sp)
                        }
                    }

                    serverQrBitmap?.let { qr ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📷 ဖုန်း Camera ဖြင့် Scan ဖတ်၍ Portal တိုက်ရိုက်ဖွင့်ရန် QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Image(
                                    bitmap = qr.asImageBitmap(),
                                    contentDescription = "Server QR Code",
                                    modifier = Modifier
                                        .size(170.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .padding(6.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "တူညီသော Wi-Fi ကွန်ရက်တွင် ဝန်ထမ်းဖုန်းဖြင့် Scan ဖတ်ပါက Chrome သို့ တိုက်ရိုက်ရောက်ရှိပါမည်",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // Features & Mode Switch Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("✨ Remote Product Addition စနစ်အသေးစိတ်", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("• ဝန်ထမ်းအမည် (Staff Name) မှတ်တမ်းတင်ခြင်း", fontSize = 11.sp)
                Text("• ကုန်ပစ္စည်းအမည်၊ အမျိုးအစား (Categories)၊ ယူနစ် သတ်မှတ်ခြင်း", fontSize = 11.sp)
                Text("• Barcode Scan ဖတ်ခြင်း သို့မဟုတ် SKU ထည့်သွင်းခြင်း", fontSize = 11.sp)
                Text("• ရောင်းဈေး၊ ဝယ်ရင်းဈေး၊ လက်ကျန်အရေအတွက်နှင့် သတိပေးလက်ကျန်", fontSize = 11.sp)
                Text("• လှမ်းထည့်လိုက်သော ပစ္စည်းများ Database ထဲသို့ တိုက်ရိုက်ရောက်ရှိခြင်း", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showServerDialog = true },
                        modifier = Modifier.weight(1f).testTag("open_server_status_btn"),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("📡 Fullscreen QR", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.setStaffClientMode(true) },
                        modifier = Modifier.weight(1f).testTag("switch_to_client_mode_btn"),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("📱 Staff Client App သို့", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }

    if (showServerDialog) {
        ServerStatusDialog(
            viewModel = viewModel,
            onDismiss = { showServerDialog = false },
            onOpenStaffClient = {
                showServerDialog = false
                viewModel.setStaffClientMode(true)
            }
        )
    }
}
