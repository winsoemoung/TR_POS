package com.example.offlinepos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsNotificationSubpage(
    viewModel: PosViewModel,
) {
    val context = LocalContext.current
    val storeConfig = remember { StoreConfig(context) }
    val storeProfile by viewModel.storeProfile.collectAsState()

    // Telegram Bot States
    var telegramEnabled by remember { mutableStateOf(storeConfig.isTelegramEnabled()) }
    var telegramToken by remember { mutableStateOf(storeConfig.getTelegramBotToken().orEmpty()) }
    var telegramChatId by remember { mutableStateOf(storeConfig.getTelegramChatId().orEmpty()) }
    var isTestingTelegram by remember { mutableStateOf(false) }
    var telegramTestResult by remember { mutableStateOf<String?>(null) }

    // Viber Bot States
    var viberEnabled by remember { mutableStateOf(storeConfig.isViberEnabled()) }
    var viberToken by remember { mutableStateOf(storeConfig.getViberBotToken().orEmpty()) }
    var viberReceiverId by remember { mutableStateOf(storeConfig.getViberReceiverId().orEmpty()) }
    var isTestingViber by remember { mutableStateOf(false) }
    var viberTestResult by remember { mutableStateOf<String?>(null) }

    // Auto-send on daily closing toggle
    var autoSendDailyClose by remember { mutableStateOf(storeConfig.isAutoSendReportsEnabled()) }
    var isDispatchingReport by remember { mutableStateOf(false) }
    var dispatchStatusMessage by remember { mutableStateOf<String?>(null) }

    val lastReportTime = storeConfig.getLastBotReportTime()
    val lastReportStatus = storeConfig.getLastBotStatus()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overview Hero Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🤖 အလိုအလျောက် သတင်းပို့စနစ်", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (telegramEnabled || viberEnabled) Color(0xFFDCFCE7) else Color(0xFFF3F4F6)
                    ) {
                        Text(
                            text = if (telegramEnabled || viberEnabled) "✓ Active" else "Setup",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (telegramEnabled || viberEnabled) Color(0xFF15803D) else Color.Gray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "နေ့စဉ် ဆိုင်ပိတ်ချိန်/နေ့ချုပ်ချိန်တွင် နေ့စဉ်ရောင်းအားစုစုပေါင်း၊ အမြတ်ငွေနှင့် ကုန်ပစ္စည်းလက်ကျန်နည်းနေမှု အကျဉ်းချုပ်စာရင်းကို ဆိုင်ရှင်၏ Telegram Bot သို့မဟုတ် Viber နှင့် ဖုန်း Notification သို့ အလိုအလျောက် အသိပေးချက် (Push Alert) ပေးပို့ပေးပါသည်။",
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Daily Closing Auto-send Switch & Instant Dispatch Test
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
                        Text("⏰ ဆိုင်ပိတ်ချိန် အလိုအလျောက် ပေးပို့ခြင်း", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "နေ့ချုပ် (Z-Report) ပိတ်သိမ်းသည်နှင့် Bot များထံ စာရင်းချက်ချင်းပို့မည်",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoSendDailyClose,
                        onCheckedChange = {
                            autoSendDailyClose = it
                            storeConfig.setAutoSendReportsEnabled(it)
                        },
                        modifier = Modifier.testTag("auto_send_daily_close_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Instant dispatch test
                Button(
                    onClick = {
                        isDispatchingReport = true
                        dispatchStatusMessage = null
                        viewModel.dispatchDailyReportNow { success, message ->
                            isDispatchingReport = false
                            dispatchStatusMessage = message
                            viewModel.setBanner(message)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("dispatch_daily_report_now_button"),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isDispatchingReport
                ) {
                    if (isDispatchingReport) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("သတင်းပို့နေပါသည်...", fontSize = 12.sp)
                    } else {
                        Text("🚀 ယနေ့စာရင်း အကျဉ်းချုပ် အခုချက်ချင်း အစမ်းပေးပို့မည်", fontSize = 12.sp)
                    }
                }

                dispatchStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (lastReportTime > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val dateFormatted = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(lastReportTime))
                    Text("နောက်ဆုံး အလိုအလျောက် သတင်းပို့ခဲ့သည့်အချိန်: $dateFormatted", fontSize = 10.sp, color = Color(0xFF15803D))
                    lastReportStatus?.let {
                        Text("အခြေအနေ: $it", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // 1. Telegram Bot Integration Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✈️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Telegram Bot ချိတ်ဆက်မှု", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Channel / Group သို့မဟုတ် ဆိုင်ရှင်ထံသို့", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = telegramEnabled,
                        onCheckedChange = {
                            telegramEnabled = it
                            storeConfig.setTelegramEnabled(it)
                        },
                        modifier = Modifier.testTag("telegram_bot_enabled_switch")
                    )
                }

                if (telegramEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = telegramToken,
                        onValueChange = {
                            telegramToken = it
                            storeConfig.setTelegramBotToken(it)
                        },
                        label = { Text("Telegram Bot Token") },
                        placeholder = { Text("123456789:ABCdefGhIJKlmNoPQR") },
                        modifier = Modifier.fillMaxWidth().testTag("telegram_bot_token_input"),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Telegram @BotFather မှ bot ဖန်တီး၍ ရရှိသော API Token ကို ထည့်ပါ",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = telegramChatId,
                        onValueChange = {
                            telegramChatId = it
                            storeConfig.setTelegramChatId(it)
                        },
                        label = { Text("Chat ID သို့မဟုတ် Channel Username") },
                        placeholder = { Text("12345678 သို့မဟုတ် @my_shop_reports") },
                        modifier = Modifier.fillMaxWidth().testTag("telegram_chat_id_input"),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "ဆိုင်ရှင်၏ User ID သို့မဟုတ် ပို့လိုသော Group/Channel ID (သို့) @channel_name",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (telegramToken.isBlank() || telegramChatId.isBlank()) {
                                    telegramTestResult = "⚠️ Bot Token နှင့် Chat ID ထည့်သွင်းပေးပါ"
                                    return@OutlinedButton
                                }
                                isTestingTelegram = true
                                telegramTestResult = null
                                viewModel.testTelegramBot(telegramToken, telegramChatId) { success, msg ->
                                    isTestingTelegram = false
                                    telegramTestResult = msg
                                    viewModel.setBanner(msg)
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("test_telegram_button"),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isTestingTelegram
                        ) {
                            if (isTestingTelegram) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("စမ်းသပ်နေသည်...", fontSize = 11.sp)
                            } else {
                                Text("🔔 Test Message ပို့မည်", fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                storeConfig.setTelegramBotToken(telegramToken)
                                storeConfig.setTelegramChatId(telegramChatId)
                                storeConfig.setTelegramEnabled(true)
                                viewModel.setBanner("Telegram ဆက်တင် သိမ်းဆည်းပြီးပါပြီ")
                            },
                            modifier = Modifier.weight(1f).testTag("save_telegram_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("သိမ်းဆည်းမည်", fontSize = 11.sp)
                        }
                    }

                    telegramTestResult?.let { res ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            res,
                            fontSize = 11.sp,
                            color = if (res.contains("အောင်မြင်")) Color(0xFF15803D) else Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 2. Viber Bot Integration Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🟣", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Viber Bot ချိတ်ဆက်မှု", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Viber Admin Account ထံသို့", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = viberEnabled,
                        onCheckedChange = {
                            viberEnabled = it
                            storeConfig.setViberEnabled(it)
                        },
                        modifier = Modifier.testTag("viber_bot_enabled_switch")
                    )
                }

                if (viberEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = viberToken,
                        onValueChange = {
                            viberToken = it
                            storeConfig.setViberBotToken(it)
                        },
                        label = { Text("Viber Bot Auth Token") },
                        placeholder = { Text("44526-a1b2c3d4-e5f6g7h8") },
                        modifier = Modifier.fillMaxWidth().testTag("viber_bot_token_input"),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Viber Partner Portal မှ Bot ဖန်တီး၍ ရရှိသော X-Viber-Auth-Token",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = viberReceiverId,
                        onValueChange = {
                            viberReceiverId = it
                            storeConfig.setViberReceiverId(it)
                        },
                        label = { Text("Receiver ID (ဆိုင်ရှင် Viber User ID)") },
                        placeholder = { Text("0123456789abcdef=") },
                        modifier = Modifier.fillMaxWidth().testTag("viber_receiver_id_input"),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Bot နှင့် စကားစပြောထားသော ဆိုင်ရှင်၏ Viber Unique User ID",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (viberToken.isBlank() || viberReceiverId.isBlank()) {
                                    viberTestResult = "⚠️ Bot Token နှင့် Receiver ID ထည့်သွင်းပေးပါ"
                                    return@OutlinedButton
                                }
                                isTestingViber = true
                                viberTestResult = null
                                viewModel.testViberBot(viberToken, viberReceiverId) { success, msg ->
                                    isTestingViber = false
                                    viberTestResult = msg
                                    viewModel.setBanner(msg)
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("test_viber_button"),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isTestingViber
                        ) {
                            if (isTestingViber) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("စမ်းသပ်နေသည်...", fontSize = 11.sp)
                            } else {
                                Text("🔔 Viber Test ပို့မည်", fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                storeConfig.setViberBotToken(viberToken)
                                storeConfig.setViberReceiverId(viberReceiverId)
                                storeConfig.setViberEnabled(true)
                                viewModel.setBanner("Viber ဆက်တင် သိမ်းဆည်းပြီးပါပြီ")
                            },
                            modifier = Modifier.weight(1f).testTag("save_viber_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("သိမ်းဆည်းမည်", fontSize = 11.sp)
                        }
                    }

                    viberTestResult?.let { res ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            res,
                            fontSize = 11.sp,
                            color = if (res.contains("အောင်မြင်")) Color(0xFF15803D) else Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 3. Android System Push Notification Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📲", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ဖုန်းမျက်နှာပြင် Push Notification", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "ဆိုင်ပိတ်ချိန်တိုင်းတွင် Android Status Bar တွင် နေ့စဉ်ရောင်းအား၊ အမြတ်ငွေနှင့် ကုန်ပစ္စည်းလက်ကျန်နည်းနေမှု အကျဉ်းချုပ်ကို Notification အဖြစ် ပြသပေးပါသည်။ အင်တာနက်မရှိသည့်အချိန်တွင်လည်း ဖုန်း Notification ချက်ချင်း တက်ပါမည်။",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}
