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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsSecuritySubpage(
    viewModel: PosViewModel,
) {
    val scrollState = rememberScrollState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val auditEvents by viewModel.auditEvents.collectAsState()
    var aiSuperuserEnabled by remember { mutableStateOf(viewModel.isAiSuperuserGranted()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // AI Superuser Privileges Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖 AI Superuser အခွင့်အရေး", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.padding(4.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (aiSuperuserEnabled) Color(0xFFDCFCE7) else Color(0xFFF3F4F6)
                            ) {
                                Text(
                                    text = if (aiSuperuserEnabled) "ACTIVE" else "DISABLED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (aiSuperuserEnabled) Color(0xFF15803D) else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "AI လက်ထောက်အား ကုန်ပစ္စည်းဈေးနှုန်း၊ အုပ်စုနှင့် စာရင်းများ တိုက်ရိုက်ပြင်ဆင်ခွင့် ပြုထားသည်",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = aiSuperuserEnabled,
                        onCheckedChange = {
                            aiSuperuserEnabled = it
                            viewModel.setAiSuperuserGranted(it)
                        },
                        modifier = Modifier.testTag("ai_superuser_switch"),
                    )
                }
            }
        }

        // Audit Events Log Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📋 စနစ်လုပ်ဆောင်ချက် မှတ်တမ်းများ (Audit Log)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("${auditEvents.size} records", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text("လတ်တလော ပြုလုပ်ခဲ့သော အရေးကြီး စနစ်မှတ်တမ်းများ (အရောင်း၊ စျေးနှုန်းပြင်ဆင်မှု၊ Auto-merge):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (auditEvents.isEmpty()) {
                    Text("မှတ်တမ်း အချက်အလက် မရှိသေးပါ", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    auditEvents.take(15).forEach { ev ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${ev.action} (${ev.aggregateType})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    val timeStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(ev.createdAtEpochMs))
                                    Text(text = timeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                ev.reason?.let { r ->
                                    if (r.isNotBlank()) {
                                        Text(text = r, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Text(text = "အသုံးပြုသူ: ${ev.actorId}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Terminal Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("စနစ် အချက်အလက်များ (System Specifications)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• ဆိုင်: ${storeProfile.storeName}", style = MaterialTheme.typography.bodySmall)
                Text("• ဗားရှင်း: TR-POS.apk (Production Release)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("• Google Drive Sync: Two-way Conflict Resolution Auto-merge ပါဝင်ပြီး", style = MaterialTheme.typography.bodySmall)
                Text("• ဆာဗာ အဆင့်: Local Embedded HTTP REST & WebSocket (Port 8787)", style = MaterialTheme.typography.bodySmall)
                Text("• ဝန်ထမ်း Web App: Chrome/Browser Direct QR Web Portal", style = MaterialTheme.typography.bodySmall)
                Text("• AI အဆင့်: Gemini 3.5 Flash Online + Superuser Local NLP", style = MaterialTheme.typography.bodySmall)
                Text("• Printer ပုံစံ: 80mm / 58mm ESC/POS Thermal (Bluetooth & USB)", style = MaterialTheme.typography.bodySmall)
                Text("• ဒေတာဘေ့စ်: SQLite Room Database (Offline-first Architecture)", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
