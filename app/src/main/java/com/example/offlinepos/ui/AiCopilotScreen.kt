package com.example.offlinepos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.offlinepos.ai.AiAction
import com.example.offlinepos.ai.AiActionType
import com.example.offlinepos.ai.AiChatMessage
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.offlinepos.ai.MessageSender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCopilotScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit = {},
) {
    val messages by viewModel.aiMessages.collectAsState()
    val isThinking by viewModel.isAiThinking.collectAsState()
    val storeProfile by viewModel.storeProfile.collectAsState()
    val actor by viewModel.actor.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll to latest message on new message or thinking state change
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // Top App Bar with Store logo, Store Name & Gemini Assistant badge
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ai_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StoreLogoAvatar(
                            customLogoPath = storeProfile.customLogoPath,
                            contentDescription = "Store Owner Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Gemini AI Assistant",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ) {
                                    Text(
                                        text = "3.8 Flash",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${storeProfile.storeName} • POS Copilot & General AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearAiChat() },
                        modifier = Modifier.testTag("clear_ai_chat_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat")
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("ai_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "AI Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )

            // Search / Command Bar & Dynamic Suggestion Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { 
                        Text(
                            "Gemini အား မေးချင်ရာမေးပါ သို့မဟုတ် POS အမိန့်ပေးပါ...",
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoAwesome, 
                            contentDescription = "Gemini", 
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (inputText.isNotEmpty()) {
                                IconButton(onClick = { inputText = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (inputText.isNotBlank()) {
                                        viewModel.sendAiCommand(inputText)
                                        inputText = ""
                                    }
                                },
                                enabled = inputText.isNotBlank() && !isThinking,
                                modifier = Modifier.testTag("ai_send_button")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank() && !isThinking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendAiCommand(inputText)
                            inputText = ""
                        }
                    })
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Dynamic Category / Suggestion Chips (Both Gemini General AI, BI Insights & POS Copilot)
                val promptSuggestions = listOf(
                    "📊 ၁ ရက် & ၁၅ ရက် သုံးသပ်ချက် (BI Report)",
                    "📅 ၁၂ လ ရာသီအလိုက် ကြိုတင်လှောင်ရန် စတော့",
                    "🏆 ရောင်းအကောင်းဆုံး ပစ္စည်းများ",
                    "⚠️ ရောင်းမထွက် / Dead Stock",
                    "📦 ပစ္စည်း ပြန်လည်မှာယူရန် (Restock)",
                    "🏢 ကုန်သွင်းသူများ စာရင်း",
                    "📉 ပျက်စီး/ပြန်သွင်း ပစ္စည်းများ",
                    "💡 အရောင်းတိုးမည့် နည်းလမ်းများ",
                    "💰 စုစုပေါင်း အရောင်းရငွေ",
                    "👥 အကြွေးကျန် Customer များ",
                    "✨ မေးချင်ရာမေးပါ",
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(promptSuggestions) { prompt ->
                        FilterChip(
                            selected = false,
                            onClick = { 
                                val actualPrompt = when (prompt) {
                                    "📊 ၁ ရက် & ၁၅ ရက် သုံးသပ်ချက် (BI Report)" -> "၁ ရက် နှင့် ၁၅ ရက် စီးပွားရေး သုံးသပ်ချက် အစီရင်ခံစာ (BI Report) ထုတ်ပေးပါ။ ရောင်းအကောင်းဆုံး၊ ရောင်းမထွက်သော စတော့နှင့် ပြန်လည်မှာယူရမည့် စာရင်းများကို တွက်ချက်ပြပေးပါ။"
                                    "📅 ၁၂ လ ရာသီအလိုက် ကြိုတင်လှောင်ရန် စတော့" -> "၁၂ လ ရာသီအလိုက် ရောင်းအားနှင့် ကြိုတင်လှောင်ထားသင့်သည့် စတော့ပမာဏ (Annual Demand & Purchase Volume Forecast) ကို လအလိုက် အသေးစိတ် ရှင်းပြပေးပါ။"
                                    "🏆 ရောင်းအကောင်းဆုံး ပစ္စည်းများ" -> "ဆိုင်တွင် ရောင်းအကောင်းဆုံး ပစ္စည်းများနှင့် ရရှိသောအမြတ်ငွေများကို တွက်ချက်ပြပေးပါ။"
                                    "⚠️ ရောင်းမထွက် / Dead Stock" -> "ဆိုင်တွင် အရောင်းထိုင်းနေသော သို့မဟုတ် ရောင်းမထွက်ဘဲ စတော့မိနေသော Dead Stock ပစ္စည်းများကို ဖော်ထုတ်ပေးပြီး ငွေဖော်နည်း အကြံပေးပါ။"
                                    "📦 ပစ္စည်း ပြန်လည်မှာယူရန် (Restock)" -> "စတော့လက်ကျန် နည်းနေသော ပစ္စည်းများကို မည်သည့် ပမာဏ ပြန်လည်မှာယူသင့်သည်နှင့် ကုန်သွင်းသူများကို အကြံပေးပါ။"
                                    "🏢 ကုန်သွင်းသူများ စာရင်း" -> "ကျွန်ုပ် ဆိုင်သို့ ကုန်ပစ္စည်းပေးသွင်းသော ကုန်သွင်းသူများ (Suppliers) စာရင်းနှင့် ဝယ်ယူမှုများကို ဖော်ပြပေးပါ။"
                                    "📉 ပျက်စီး/ပြန်သွင်း ပစ္စည်းများ" -> "ပျက်စီးဆုံးရှုံးမှုများနှင့် Customer ပစ္စည်းပြန်သွင်း/ပြန်အမ်းငွေ မှတ်တမ်းများကို သုံးသပ်ပြပေးပါ။"
                                    "✨ မေးချင်ရာမေးပါ" -> "မင်္ဂလာပါ Gemini! မင်း ဘာတွေ လုပ်ပေးနိုင်သလဲ ရှင်းပြပေးပါ။"
                                    "💡 အရောင်းတိုးမည့် နည်းလမ်းများ" -> "ကျွန်တော့်ဆိုင် အရောင်းတိုးစေဖို့ လက်တွေ့ကျတဲ့ စီးပွားရေးနဲ့ ဈေးရောင်း နည်းလမ်းကောင်းတွေ အကြံပေးပါ။"
                                    else -> prompt
                                }
                                viewModel.sendAiCommand(actualPrompt) 
                            },
                            label = { Text(prompt, fontSize = 12.sp) },
                            modifier = Modifier.testTag("category_chip_$prompt"),
                        )
                    }
                }
            }

            // Automated 1st & 15th BI Insights Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📊 ၁ ရက် & ၁၅ ရက် စီးပွားရေးဆန်းစစ်ချက် (BI Insights)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "ရောင်းအားအကောင်းဆုံး၊ Dead Stock နှင့် ၁၂ လ ရာသီအလိုက် လှောင်ရမည့် စာရင်း အဆင်သင့်ရှိပါသည်",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.sendAiCommand("၁ ရက် နှင့် ၁၅ ရက် စီးပွားရေး သုံးသပ်ချက် အစီရင်ခံစာ (BI Report) ထုတ်ပေးပါ။")
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("ထုတ်ယူမည်", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Chat Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (messages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Google Gemini AI Assistant & POS Copilot",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Gemini App ကဲ့သို့ မည်သည့် အထွေထွေ ဗဟုသုတ၊ စီးပွားရေး အကြံဉာဏ်၊ စာရေးသားခြင်းနှင့် ဘာသာပြန်ခြင်းများကိုမဆို လွတ်လပ်စွာ မေးမြန်းနိုင်သလို၊ ဆိုင်တွင်း စတော့၊ အရောင်း၊ စာရင်းဇယားနှင့် ဈေးနှုန်းများကိုလည်း တိုက်ရိုက် စီမံခိုင်းစေနိုင်ပါသည် ခင်ဗျာ။",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    ) {
                                        Text(
                                            text = "🌐 Gemini 3.8 Flash Online",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                    ) {
                                        Text(
                                            text = "📦 Offline POS Copilot",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            onExecuteAction = { action ->
                                viewModel.executeAiAction(action)
                            }
                        )
                    }
                }

                if (isThinking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI စဉ်းစားတွက်ချက်နေပါသည်...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (showSettingsDialog) {
            AiSettingsDialog(
                viewModel = viewModel,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
fun ChatMessageItem(
    message: AiChatMessage,
    onExecuteAction: (AiAction) -> Unit,
) {
    val isUser = message.sender == MessageSender.USER

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(28.dp)
                        .padding(top = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp,
                ),
                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                tonalElevation = 1.dp,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Executable Action Card
        message.action?.let { action ->
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(start = if (isUser) 0.dp else 36.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (action.isExecuted) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "⚙️ AI Superuser Command",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        if (action.isExecuted) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDCFCE7),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Executed",
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "လုပ်ဆောင်ပြီးပါပြီ",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = action.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Detail Specs for All Action Types
                    when (val type = action.type) {
                        is AiActionType.AddProduct -> {
                            Text(
                                text = "• ပစ္စည်း: ${type.name} (${type.category})\n• Code (SKU): ${type.sku}\n• Barcode: ${type.barcode ?: "-"}\n• ရောင်းဈေး: %,d Ks | သွင်းဈေး: %,d Ks\n• လက်ကျန်: ${type.stockQuantity} ခု".format(
                                    type.sellPriceMinor,
                                    type.costPriceMinor
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.ReplaceProductCode -> {
                            Text(
                                text = "• ပစ္စည်း: ${type.productName}\n• Code အဟောင်း: ${type.oldCode} ➔ Code အသစ်: ${type.newCode}\n• အမျိုးအစား: ${if (type.isBarcode) "Barcode" else "SKU Code"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.UpdateSku -> {
                            Text(
                                text = "• ပစ္စည်း: ${type.productName}\n• Code အဟောင်း: ${type.oldSku} ➔ Code အသစ်: ${type.newSku}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.UpdateBarcode -> {
                            Text(
                                text = "• ပစ္စည်း: ${type.productName}\n• Barcode အဟောင်း: ${type.oldBarcode ?: "-"} ➔ Barcode အသစ်: ${type.newBarcode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.BatchReplaceCodePrefix -> {
                            Text(
                                text = "• အသုတ်လိုက် Code ပြုပြင်ခြင်း: ပစ္စည်းအားလုံးရှေ့တွင် '${type.newPrefix}' Prefix တပ်ဆင်မည်",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.AddPromoCode -> {
                            Text(
                                text = "• Promo / Discount Code: ${type.code}\n• လျှော့နှုန်း: ${if (type.discountPercent > 0) "${type.discountPercent}%" else "%,d Ks".format(type.discountFlatMinor)}\n• ဖော်ပြချက်: ${type.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.ReplacePromoCode -> {
                            Text(
                                text = "• Code အဟောင်း: ${type.oldCode} ➔ Code အသစ်: ${type.newCode}\n• လျှော့နှုန်းသစ်: ${type.discountPercent}%\n• ဖော်ပြချက်: ${type.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.ExecuteDynamicCode -> {
                            Text(
                                text = "• Language: ${type.codeLanguage}\n• Script: ${type.codeScript}\n• ဖော်ပြချက်: ${type.explanation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.ApplyCartDiscountByCode -> {
                            Text(
                                text = "• Promo Code '${type.promoCode}' ဖြင့် ${type.discountPercent}% လျှော့ပေးခြင်း",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.UpdatePrice -> {
                            Text(
                                text = "• ပစ္စည်း: ${type.productName}\n• ပြင်ဆင်မည့် ရောင်းဈေး: %,d Ks".format(type.newSellPriceMinor),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.AdjustStock -> {
                            Text(
                                text = "• ပစ္စည်း: ${type.productName}\n• လက်ကျန် ထပ်ဖြည့်အရေအတွက်: +${type.adjustment}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.SetTaxRate -> {
                            Text(
                                text = "• သတ်မှတ်မည့် အခွန်နှုန်းထား: ${type.newTaxPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.UpdateStoreProfile -> {
                            Text(
                                text = "• ဆိုင်အမည်: ${type.storeName ?: '-'}\n• ဖုန်း: ${type.phone ?: '-'}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is AiActionType.BatchReplaceCategory -> {
                            Text(
                                text = "• ကုန်ပစ္စည်းအမျိုးအစား အစားထိုးခြင်း: '${type.oldCategory}' ➔ '${type.newCategory}'",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!action.isExecuted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onExecuteAction(action) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .testTag("execute_ai_action_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("အတည်ပြု လုပ်ဆောင်မည် (Execute Code / Action)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiSettingsDialog(
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
) {
    var apiKeyInput by remember { mutableStateOf(viewModel.getGeminiApiKey()) }
    var autoExecute by remember { mutableStateOf(viewModel.isAiAutoExecute()) }
    var superuserPrivileges by remember { mutableStateOf(viewModel.isAiSuperuserGranted()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("ai_settings_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI စနစ် သတ်မှတ်ချက်များ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "Close")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Google Gemini 3.8 Flash (AI Logic)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "App Check ဖြင့် Cloud AI အလိုအလျောက် ချိတ်ဆက်ထားပါသည်။",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Google Gemini API Key (Optional / Backup):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text("AIzaSy... (Gemini 3.8 Flash)", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("gemini_api_key_input"),
                    shape = RoundedCornerShape(10.dp),
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "💡 သီးသန့် Gemini API Key ထည့်ထားပါက Backup အဖြစ် သုံးစွဲမည်ဖြစ်ပါသည်။ မထည့်ထားပါကလည်း Firebase AI Logic နှင့် Offline Rule Engine တို့ဖြင့် ချောမွေ့စွာ အလုပ်လုပ်ပါသည်။",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Superuser Privileges",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "AI အား Code အသစ်ထည့်ခြင်း၊ အစားထိုးခြင်း နှင့် ပြုပြင်ခြင်း အခွင့်အရေး အပြည့်အဝ ပေးရန်",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(
                        checked = superuserPrivileges,
                        onCheckedChange = { superuserPrivileges = it },
                        modifier = Modifier.testTag("ai_superuser_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Execute Actions",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "ခလုတ်ထပ်မံနှိပ်စရာမလိုဘဲ AI အမိန့်ကို ချက်ချင်း အလိုအလျောက် အတည်ပြုလုပ်ဆောင်မည်",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(
                        checked = autoExecute,
                        onCheckedChange = { autoExecute = it },
                        modifier = Modifier.testTag("ai_auto_execute_switch")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        viewModel.setGeminiApiKey(apiKeyInput)
                        viewModel.setAiAutoExecute(autoExecute)
                        viewModel.setAiSuperuserGranted(superuserPrivileges)
                        viewModel.setBanner("AI သတ်မှတ်ချက်များကို သိမ်းဆည်းပြီးပါပြီ")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_ai_settings_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("သိမ်းဆည်းမည် (Save Settings)")
                }
            }
        }
    }
}
