package com.example.offlinepos.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsBackupSubpage(
    viewModel: PosViewModel,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val emailBringIntoViewRequester = remember { BringIntoViewRequester() }

    val googleAccount by viewModel.googleAccount.collectAsState()
    val lastGoogleBackupTime by viewModel.lastGoogleBackupTime.collectAsState()
    val isDriveSyncing by viewModel.isDriveSyncing.collectAsState()
    val driveSyncStatus by viewModel.driveSyncStatus.collectAsState()
    var googleEmailInput by remember(googleAccount) { mutableStateOf(googleAccount ?: "WINSOEMOUNG@gmail.com") }

    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreChoice by remember { mutableStateOf(false) }

    val storeProfile by viewModel.storeProfile.collectAsState()
    var backupSchedule by remember(storeProfile) { mutableStateOf(storeProfile.autoBackupSchedule) }

    // File pickers
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val exported = viewModel.exportDatabaseFile()
                withContext(Dispatchers.Main) {
                    if (exported != null) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { output ->
                                exported.inputStream().use { it.copyTo(output) }
                            }
                            backupStatusMessage = "Backup အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ"
                            viewModel.setBanner("Backup အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ")
                        } catch (e: Exception) {
                            backupStatusMessage = "Backup သိမ်းဆည်းမရပါ: ${e.message}"
                        }
                    } else {
                        backupStatusMessage = "Database ဖိုင် backup ထုတ်ယူမရပါ"
                    }
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreChoice = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Two-way Conflict Resolution Auto-merge Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔄 Two-way Conflict Resolution Auto-merge", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFDCFCE7),
                    ) {
                        Text(
                            "✓ Auto-merge Active",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Owner phone နှင့် Cashier စက်များကြား SKU-based identity ဖြင့် ကုန်ပစ္စည်း၊ ဝယ်သူ၊ အရောင်းနှင့် ငွေစာရင်းများကို ပဋိပက္ခမဖြစ်စေဘဲ Two-way Auto-merge ဖြင့် အလိုအလျောက် ညှိနှိုင်းပေါင်းစပ်ပေးပါသည်။",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                val recordSyncPrefs = context.getSharedPreferences("pos_sync_status", 0)
                val recordSyncTime = recordSyncPrefs.getLong("last_record_sync_ms", 0L)
                val recordCount = recordSyncPrefs.getInt("last_record_count", 0)
                val conflictsResolved = recordSyncPrefs.getInt("last_conflicts_resolved", 0)
                val conflictSummary = recordSyncPrefs.getString("last_conflict_summary", null)

                if (recordSyncTime > 0L) {
                    val formatted = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(recordSyncTime))
                    Text("နောက်ဆုံး Auto-merge လုပ်ခဲ့သည့်အချိန်: $formatted ($recordCount records)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    if (conflictsResolved > 0) {
                        Text("⚡ ပဋိပက္ခ (Conflicts) $conflictsResolved ခုကို အလိုအလျောက် အောင်မြင်စွာ ညှိနှိုင်းပေါင်းစပ်ပြီးပါပြီ", style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                    }
                    conflictSummary?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Two-way record merge sync မလုပ်ရသေးပါ (ပစ္စည်းသွင်းခြင်း သို့မဟုတ် အရောင်းဖွင့်သည်နှင့် အလိုအလျောက် ပေါင်းစပ်ပါမည်)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }

        // Google Drive Real-time Auto-Sync Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("☁️ Google Drive Real-time Auto-sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "ပစ္စည်းအသစ်သွင်းခြင်း၊ အရောင်းဘေလ်ဖွင့်ခြင်း၊ ငွေစာရင်းပြင်ဆင်တိုင်း Drive သို့ ချက်ချင်း Auto-sync ပြုလုပ်ပေးပါသည်။ အော့ဖ်လိုင်းဖြစ်နေပါက Queue တွင် စောင့်ဆိုင်းပြီး လိုင်းပြန်ရသည်နှင့် အလိုအလျောက် ပို့ပေးပါသည် (Offline-first Resilience)။",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (googleAccount != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✅ ချိတ်ဆက်ထားသော Google Account:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFDCFCE7),
                                ) {
                                    Text(
                                        "⚡ Auto-sync Active",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(googleAccount!!, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)

                            Spacer(modifier = Modifier.height(6.dp))
                            if (isDriveSyncing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(driveSyncStatus ?: "Google Drive သို့ Sync လုပ်နေပါသည်...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            } else {
                                driveSyncStatus?.let { status ->
                                    Text(
                                        status,
                                        fontSize = 11.sp,
                                        color = if (status.contains("မအောင်မြင်") || status.contains("ပြတ်တောက်") || status.contains("စောင့်ဆိုင်း") || status.contains("အော့ဖ်လိုင်း")) Color(0xFFD97706) else Color(0xFF15803D),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (lastGoogleBackupTime > 0) {
                                val formattedTime = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(lastGoogleBackupTime))
                                Text("နောက်ဆုံး Backup လုပ်ခဲ့သည့်အချိန်: $formattedTime", fontSize = 11.sp, color = Color(0xFF15803D))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.disconnectGoogleAccount() },
                            modifier = Modifier.weight(1f).testTag("disconnect_google_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("အကောင့်ဖြုတ်မည်", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                        Button(
                            onClick = { viewModel.syncGoogleDriveNow { _, message -> backupStatusMessage = message } },
                            modifier = Modifier.weight(1.3f).testTag("backup_to_drive_button"),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isDriveSyncing
                        ) {
                            if (isDriveSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Syncing...", fontSize = 11.sp)
                            } else {
                                Text("☁️ အခုချက်ချင်း Sync မည်", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(emailBringIntoViewRequester)
                    ) {
                        OutlinedTextField(
                            value = googleEmailInput,
                            onValueChange = {
                                googleEmailInput = it
                                coroutineScope.launch { emailBringIntoViewRequester.bringIntoView() }
                            },
                            label = { Text("Google အကောင့် (Gmail)") },
                            placeholder = { Text("WINSOEMOUNG@gmail.com") },
                            modifier = Modifier.fillMaxWidth().testTag("google_email_input"),
                            singleLine = true,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.connectGoogleAccount(googleEmailInput) },
                            modifier = Modifier.fillMaxWidth().testTag("connect_google_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🔗 Google Account ဖြင့် ချိတ်ဆက်မည်", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Local SQLite Database Export & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("💾 ဒေသတွင်း ဒေတာ အရန်သိမ်း/ပြန်တင်ခြင်း (Local DB Backup)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "SQLite Database ဖိုင်တစ်ခုလုံးကို သိမ်းဆည်းရန် သို့မဟုတ် အခြားစက်သို့ ပြောင်းရွှေ့ရန် သုံးပါ:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val fileName = "offlinepos_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.db"
                            try {
                                backupLauncher.launch(fileName)
                            } catch (e: Exception) {
                                backupStatusMessage = "ဖိုင်သိမ်းဆည်းမှုကို ဖွင့်မရပါ: ${e.localizedMessage}"
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("export_backup_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📤 Backup ထုတ်မည်", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                restoreLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/x-sqlite3"))
                            } catch (e: Exception) {
                                backupStatusMessage = "ဖိုင်ရွေးချယ်မှုကို ဖွင့်မရပါ: ${e.localizedMessage}"
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("import_backup_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📥 Restore ပြန်တင်မည်", fontSize = 11.sp)
                    }
                }

                // Share DB directly via intent
                OutlinedButton(
                    onClick = {
                        val dbFile = context.getDatabasePath("offline_pos.db")
                        if (dbFile.exists()) {
                            try {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dbFile)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Database Backup"))
                            } catch (e: Exception) {
                                backupStatusMessage = "Share မအောင်မြင်ပါ: ${e.message}"
                            }
                        } else {
                            backupStatusMessage = "Database ဖိုင် မတွေ့ပါ"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("📤 Database ဖိုင်အား Share / Messenger သို့ ပေးပို့မည်", fontSize = 11.sp)
                }

                backupStatusMessage?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (it.contains("မအောင်မြင်") || it.contains("မတွေ့")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider()

                Column {
                    Text("အလိုအလျောက် ဒေတာ အရန်သိမ်းမည့် အချိန် (Schedule):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("DAILY" to "နေ့စဉ် (ညနေ ၈:၀၀)", "WEEKLY" to "အပတ်စဉ် (တနင်္ဂနွေ)", "MANUAL" to "ကိုယ်တိုင်သာ")) { (schedule, label) ->
                            FilterChip(
                                selected = backupSchedule == schedule,
                                onClick = {
                                    backupSchedule = schedule
                                    viewModel.updateStoreProfile(storeProfile.copy(autoBackupSchedule = schedule))
                                    viewModel.setBanner("Backup Schedule ကို $label သို့ ပြောင်းလဲသိမ်းဆည်းပြီးပါပြီ")
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }

    if (showRestoreChoice && pendingRestoreUri != null) {
        Dialog(onDismissRequest = {
            showRestoreChoice = false
            pendingRestoreUri = null
        }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Choose Database Import Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Replace mode သည် လက်ရှိ database တစ်ခုလုံးကို backup file ဖြင့် အစားထိုးမည်။ Merge mode သည် Product များကိုသာ ပေါင်းထည့်ပြီး ရှိပြီးသား data များကို မဖျက်ပါ။",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val uri = pendingRestoreUri
                            showRestoreChoice = false
                            pendingRestoreUri = null
                            if (uri != null) viewModel.importDatabaseFile(uri) { success, msg ->
                                backupStatusMessage = msg
                                viewModel.setBanner(msg)
                                if (success) (context as? Activity)?.recreate()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Replace Current Database") }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            val uri = pendingRestoreUri
                            showRestoreChoice = false
                            pendingRestoreUri = null
                            if (uri != null) viewModel.mergeProductsFromDatabase(uri) { success, msg ->
                                backupStatusMessage = msg
                                viewModel.setBanner(msg)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Merge Products Only") }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            showRestoreChoice = false
                            pendingRestoreUri = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Cancel") }
                }
            }
        }
    }
}
