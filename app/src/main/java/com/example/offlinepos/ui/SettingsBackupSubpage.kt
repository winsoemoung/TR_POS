package com.example.offlinepos.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()
    val lastFirestoreSyncTime by viewModel.lastFirestoreSyncTime.collectAsState()
    val outboxPendingCount by viewModel.outboxPendingCount.collectAsState()
    var googleEmailInput by remember(googleAccount) { mutableStateOf(googleAccount ?: "WINSOEMOUNG@gmail.com") }

    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreChoice by remember { mutableStateOf(false) }

    val storeProfile by viewModel.storeProfile.collectAsState()
    var backupSchedule by remember(storeProfile) { mutableStateOf(storeProfile.autoBackupSchedule) }
    val authRecoveryIntent by viewModel.authRecoveryIntent.collectAsState()
    var oauthCodeInput by remember { mutableStateOf("") }
    var isExchangingCode by remember { mutableStateOf(false) }

    // Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val success = viewModel.handleGoogleSignInResult(result.data)
            if (success) {
                backupStatusMessage = "Google Account ဖြင့် အောင်မြင်စွာ ချိတ်ဆက်ပြီးပါပြီ"
                viewModel.setBanner("Google Account ဖြင့် အောင်မြင်စွာ ချိတ်ဆက်ပြီးပါပြီ")
            } else {
                backupStatusMessage = "Google Sign-In စစ်ဆေးမရပါ"
            }
        }
    }

    // Google Account Picker Launcher
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val email = result.data?.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
            if (!email.isNullOrBlank()) {
                viewModel.connectGoogleAccount(email)
                viewModel.triggerAutoDriveSync("Account Picker Selected")
                backupStatusMessage = "$email ဖြင့် ချိတ်ဆက်ပြီးပါပြီ"
                viewModel.setBanner("$email ဖြင့် ချိတ်ဆက်ပြီးပါပြီ")
            }
        }
    }

    // Google Drive Auth Permission Recovery Launcher
    val authRecoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.clearAuthRecoveryIntent()
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.syncGoogleDriveNow { ok, msg ->
                backupStatusMessage = msg
                viewModel.setBanner(msg)
            }
        }
    }

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

    val receiptCsvBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val csvFile = viewModel.exportReceiptsCsvFile()
                withContext(Dispatchers.Main) {
                    if (csvFile != null) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { output ->
                                csvFile.inputStream().use { it.copyTo(output) }
                            }
                            backupStatusMessage = "ဘောင်ချာမှတ်တမ်းများ (Receipt History CSV) အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ"
                            viewModel.setBanner("ဘောင်ချာမှတ်တမ်းများ (Receipt History CSV) အောင်မြင်စွာ သိမ်းဆည်းပြီးပါပြီ")
                        } catch (e: Exception) {
                            backupStatusMessage = "CSV သိမ်းဆည်းမရပါ: ${e.message}"
                        }
                    } else {
                        backupStatusMessage = "ဘောင်ချာမှတ်တမ်း CSV ထုတ်ယူမရပါ"
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // -------------------------------------------------------------
        // FIREBASE CLOUD FIRESTORE & GOOGLE SIGN-IN SYNC CARD
        // -------------------------------------------------------------
        Card(
            modifier = Modifier.fillMaxWidth().testTag("firebase_firestore_sync_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            border = BorderStroke(1.5.dp, Color(0xFFFCA5A5))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔥", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Firebase Firestore Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF991B1B))
                            Text("Google Account တိုက်ရိုက်ချိတ်ဆက်မှု", fontSize = 11.sp, color = Color(0xFF7F1D1D))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (firebaseUser != null) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ) {
                        Text(
                            text = if (firebaseUser != null) "✓ Logged In" else "Sign In လိုအပ်",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (firebaseUser != null) Color(0xFF15803D) else Color(0xFFB91C1C),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (firebaseUser != null) {
                    // Logged in user info
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("👤", fontSize = 20.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = firebaseUser?.displayName ?: "Google User",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = firebaseUser?.email ?: "",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(8.dp))

                            firestoreSyncStatus?.let { status ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📡 အခြေအနေ: ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                                    Text(status, fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Medium)
                                }
                            }
                            if (lastFirestoreSyncTime > 0L) {
                                val syncDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(lastFirestoreSyncTime))
                                Text("နောက်ဆုံး Sync: $syncDate", fontSize = 10.sp, color = Color(0xFF64748B))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerFirestoreSync("Settings Sync Button") },
                            modifier = Modifier.weight(1f).testTag("firestore_sync_now_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            enabled = !isFirestoreSyncing
                        ) {
                            if (isFirestoreSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync လုပ်နေသည်...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ အခုပဲ Sync မည်", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.signOutGoogle(context as? Activity) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                        ) {
                            Text("အကောင့်ထွက်မည်", fontSize = 11.sp)
                        }
                    }
                } else {
                    // Not logged in: Show Google Sign-In prompt
                    Text(
                        text = "Google Account ဖြင့် ဝင်ရောက်ထားပါက ကုန်ပစ္စည်းများ၊ အရောင်းဘောင်ချာများနှင့် ဖောက်သည်အကြွေးစာရင်းများကို Cloud Firestore သို့ အချိန်နှင့်တပြေးညီ အလိုအလျောက် Sync သိမ်းဆည်းပေးမည်ဖြစ်ပါသည်။",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val activity = context as? Activity
                            if (activity != null) {
                                viewModel.signInWithGoogle(activity) { success, err ->
                                    if (success) {
                                        backupStatusMessage = "Google Account ဖြင့် အောင်မြင်စွာ ချိတ်ဆက်ပြီးပါပြီ"
                                    } else if (err != null) {
                                        backupStatusMessage = err
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("google_signin_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("G ", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFFFBBF24))
                            Text("Google Account ဖြင့် Sign In ဝင်မည်", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

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

                if (authRecoveryIntent != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("⚠️ Google Drive App Data ခွင့်ပြုချက် အတည်ပြုရန် လိုအပ်ပါသည်", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                            Text("Google Drive သို့ အလိုအလျောက် sync လုပ်နိုင်ရန် Drive ခွင့်ပြုချက် (Authorization) ကို အတည်ပြုပေးရန် နှိပ်ပါ။", fontSize = 11.sp, color = Color(0xFF92400E))
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    authRecoveryIntent?.let { authRecoveryLauncher.launch(it) }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                modifier = Modifier.fillMaxWidth().testTag("grant_drive_permission_button")
                            ) {
                                Text("ခွင့်ပြုချက် အတည်ပြုမည် (Grant Permission)", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                }

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
                        Button(
                            onClick = { viewModel.syncGoogleDriveNow { _, message -> backupStatusMessage = message } },
                            modifier = Modifier.weight(1.2f).testTag("backup_to_drive_button"),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isDriveSyncing
                        ) {
                            if (isDriveSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Syncing...", fontSize = 11.sp)
                            } else {
                                Text("☁️ Drive Sync / Backup မည်", fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.restoreFromGoogleDrive { ok, message ->
                                    backupStatusMessage = message
                                    viewModel.setBanner(message)
                                }
                            },
                            modifier = Modifier.weight(1.2f).testTag("restore_from_drive_button"),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isDriveSyncing
                        ) {
                            Text("📥 Drive မှ Restore မည်", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    var isTestingConn by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = {
                            isTestingConn = true
                            viewModel.testGoogleDriveConnection { _, msg ->
                                isTestingConn = false
                                backupStatusMessage = msg
                                viewModel.setBanner(msg)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("test_drive_connection_button"),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isDriveSyncing && !isTestingConn
                    ) {
                        if (isTestingConn) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Google Drive API စစ်ဆေးနေပါသည်...", fontSize = 11.sp)
                        } else {
                            Text("🔍 Drive ချိတ်ဆက်မှု အခြေအနေ စစ်ဆေးမည် (Test Connection)", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { viewModel.disconnectGoogleAccount() },
                        modifier = Modifier.fillMaxWidth().testTag("disconnect_google_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Google Account အကောင့်ဖြုတ်မည်", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(emailBringIntoViewRequester)
                    ) {
                        Text(
                            "Google Account ဖြင့် ချိတ်ဆက်၍ အလိုအလျောက် Cloud Auto-sync ပြုလုပ်ပါ:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // 1. One-tap Google Sign-In
                        Button(
                            onClick = {
                                try {
                                    googleSignInLauncher.launch(viewModel.googleSignInIntent())
                                } catch (e: Exception) {
                                    backupStatusMessage = "Google Sign-In ဖွင့်မရပါ: ${e.message}"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("🔑 Google Account ဖြင့် Sign-In ဝင်မည်", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Google Account Picker from device
                        OutlinedButton(
                            onClick = {
                                try {
                                    accountPickerLauncher.launch(viewModel.googleAccountPickerIntent())
                                } catch (e: Exception) {
                                    backupStatusMessage = "Account Picker ဖွင့်မရပါ: ${e.message}"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("google_account_picker_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("📱 ဖုန်းထဲရှိ Google Account ရွေးချယ်မည်", fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("သို့မဟုတ် Gmail လိပ်စာ တိုက်ရိုက်ရိုက်ထည့်ရန်:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = googleEmailInput,
                                onValueChange = {
                                    googleEmailInput = it
                                    coroutineScope.launch { emailBringIntoViewRequester.bringIntoView() }
                                },
                                placeholder = { Text("WINSOEMOUNG@gmail.com") },
                                modifier = Modifier.weight(1f).testTag("google_email_input"),
                                singleLine = true,
                            )
                            Button(
                                onClick = { viewModel.connectGoogleAccount(googleEmailInput) },
                                modifier = Modifier.align(Alignment.CenterVertically).testTag("connect_google_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ချိတ်မည်", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("အကြံပြု အကောင့်:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = googleEmailInput == "WINSOEMOUNG@gmail.com",
                                onClick = {
                                    googleEmailInput = "WINSOEMOUNG@gmail.com"
                                    viewModel.connectGoogleAccount("WINSOEMOUNG@gmail.com")
                                },
                                label = { Text("WINSOEMOUNG@gmail.com", fontSize = 10.sp) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Google Cloud OAuth 2.0 Client credentials panel
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🛡️ Google Cloud OAuth 2.0 (pos-sync-4)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                                        Text("Configured", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                                Text("Client ID: ${com.example.offlinepos.Sync.GoogleOAuthManager.getClientId(context)}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val url = viewModel.getGoogleOAuthConsentUrl()
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(browserIntent)
                                            backupStatusMessage = "ဘရောက်ဆာတွင် ခွင့်ပြုချက်ပေးပြီးနောက် ရရှိသော Code ကို အောက်တွင် ထည့်သွင်းပါ"
                                        } catch (e: Exception) {
                                            backupStatusMessage = "Browser ဖွင့်မရပါ: ${e.message}"
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("browser_oauth_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("🌐 Browser ဖြင့် တိုက်ရိုက် Google OAuth ခွင့်ပြုမည်", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = oauthCodeInput,
                                        onValueChange = { oauthCodeInput = it },
                                        placeholder = { Text("4/0A... Authorization Code ထည့်ရန်", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f).testTag("oauth_code_input"),
                                        singleLine = true,
                                    )
                                    Button(
                                        onClick = {
                                            if (oauthCodeInput.isNotBlank()) {
                                                isExchangingCode = true
                                                viewModel.exchangeManualOAuthCode(oauthCodeInput) { ok, msg ->
                                                    isExchangingCode = false
                                                    backupStatusMessage = msg
                                                    viewModel.setBanner(msg)
                                                    if (ok) oauthCodeInput = ""
                                                }
                                            }
                                        },
                                        enabled = oauthCodeInput.isNotBlank() && !isExchangingCode,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.align(Alignment.CenterVertically).testTag("exchange_oauth_code_button")
                                    ) {
                                        if (isExchangingCode) {
                                            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 1.5.dp)
                                        } else {
                                            Text("အတည်ပြုမည်", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("💡 Google Drive Sync အောင်မြင်ရန် လိုအပ်ချက်များ:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Text("၁။ Google Cloud Console (pos-sync-4) တွင် 'Google Drive API' ကို Enable ပြုလုပ်ထားရန် လိုအပ်ပါသည်။", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("၂။ OAuth Consent Screen တွင် Status သည် Testing ဖြစ်ပါက အသုံးပြုမည့် Gmail ကို 'Test users' ထဲ ထည့်ထားပေးရပါမည်။", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("၃။ အက်ပ်တွင် '⚠️ Google Drive ခွင့်ပြုချက် အတည်ပြုရန် လိုအပ်ပါသည်' ပေါ်ပါက 'ခွင့်ပြုချက် အတည်ပြုမည်' ကို တစ်ချက်နှိပ်၍ Allow ပေးပါ။", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("၄။ Browser ဖြင့် တိုက်ရိုက်ခွင့်ပြုလိုပါက အထက်ပါ '🌐 Browser ဖြင့် တိုက်ရိုက် Google OAuth ခွင့်ပြုမည်' ကို နှိပ်၍ Code ရယူထည့်သွင်းနိုင်ပါသည်။", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Local SQLite Database Export & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("local_database_sync_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("💾 Local Database Sync & Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Offline-First Room SQLite v3", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (outboxPendingCount > 0) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = if (outboxPendingCount > 0) "Pending Queue: $outboxPendingCount" else "✓ Local DB Synced",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (outboxPendingCount > 0) Color(0xFFB45309) else Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Small animated progress bar when Pending Queue > 0
                if (outboxPendingCount > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFD97706)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "⏳ Syncing pending operations...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Text(
                                    "Pending Queue: $outboxPendingCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFFD97706),
                                trackColor = Color(0xFFFDE68A)
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🧾", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ဤ Backup တွင် ဖွင့်ခဲ့ပြီးသမျှသော ဘောင်ချာမှတ်တမ်းများ (Receipt History)၊ အရောင်းပြေစာများ၊ ကုန်ပစ္စည်းများနှင့် ဝယ်သူစာရင်း အားလုံး ပြည့်စုံစွာ ပါဝင်ပါသည်။",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 15.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val fileName = "tr_pos_full_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.db"
                            try {
                                backupLauncher.launch(fileName)
                            } catch (e: Exception) {
                                backupStatusMessage = "ဖိုင်သိမ်းဆည်းမှုကို ဖွင့်မရပါ: ${e.localizedMessage}"
                            }
                        },
                        modifier = Modifier.weight(1.2f).testTag("export_backup_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📤 Full Backup ထုတ်မည် (ဘောင်ချာပါဝင်)", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                restoreLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/x-sqlite3"))
                            } catch (e: Exception) {
                                backupStatusMessage = "ဖိုင်ရွေးချယ်မှုကို ဖွင့်မရပါ: ${e.localizedMessage}"
                            }
                        },
                        modifier = Modifier.weight(0.9f).testTag("import_backup_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📥 Restore ပြန်တင်မည်", fontSize = 11.sp)
                    }
                }

                // Dedicated Receipts CSV Export Button
                OutlinedButton(
                    onClick = {
                        val csvFileName = "tr_pos_receipts_history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
                        try {
                            receiptCsvBackupLauncher.launch(csvFileName)
                        } catch (e: Exception) {
                            backupStatusMessage = "CSV သိမ်းဆည်းမှုကို ဖွင့်မရပါ: ${e.localizedMessage}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("export_receipts_csv_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("📑 ဘောင်ချာမှတ်တမ်းများ (Receipts History CSV) သီးသန့် ထုတ်ယူမည်", fontSize = 11.sp)
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

        // SQLite Database Maintenance & Auto-Compacting (VACUUM) Card
        var isVacuuming by remember { mutableStateOf(false) }
        var currentDbSize by remember { mutableStateOf(com.example.offlinepos.data.local.AppDatabase.getDatabaseFileSize(context)) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("🧹 Database Auto-Compacting (VACUUM)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("WAL Checkpoint, Index Optimization & Auto-Vacuum", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("လက်ရှိ Database ဖိုင်ဆိုက်:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(currentDbSize, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7),
                        ) {
                            Text(
                                "Auto-Index Active",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Text(
                    "အရောင်းပြေစာများ၊ ပစ္စည်းများ ပြင်ဆင်/ဖျက်ပစ်ခြင်း များလာပါက Database ဖိုင်ဆိုက် မဖောင်းပွစေရန်နှင့် Query မြန်ဆန်စေရန် အခါအားလျော်စွာ VACUUM ပြုလုပ်ပေးပါသည်။",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                Button(
                    onClick = {
                        isVacuuming = true
                        viewModel.runDatabaseVacuumMaintenance { result ->
                            isVacuuming = false
                            currentDbSize = com.example.offlinepos.data.local.AppDatabase.getDatabaseFileSize(context)
                            backupStatusMessage = result.message
                        }
                    },
                    enabled = !isVacuuming,
                    modifier = Modifier.fillMaxWidth().testTag("run_database_vacuum_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isVacuuming) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("နေရာလွတ် ပြန်သိမ်းနေပါသည်...", fontSize = 11.sp)
                    } else {
                        Text("⚡ နေရာလွတ် ပြန်သိမ်းမည် (VACUUM & Optimize Database)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    Text("Database Import & Restore ရွေးချယ်မှု", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• 'Merge All Data' သည် ဘောင်ချာမှတ်တမ်း (Receipt History)၊ အရောင်းပြေစာများနှင့် ကုန်ပစ္စည်းစာရင်းများကို လက်ရှိ data မပျက်ဘဲ ပေါင်းစပ်ထည့်သွင်းမည်။\n• 'Replace Database' သည် လက်ရှိ database တစ်ခုလုံးကို backup file ဖြင့် အစားထိုးမည်။",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val uri = pendingRestoreUri
                            showRestoreChoice = false
                            pendingRestoreUri = null
                            if (uri != null) viewModel.mergeDatabaseWithReceiptsAndProducts(uri) { success, msg ->
                                backupStatusMessage = msg
                                viewModel.setBanner(msg)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("merge_all_data_button"),
                    ) { Text("✨ ဘောင်ချာ & ပစ္စည်းများ အားလုံး ပေါင်းစပ်မည် (Merge All)") }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
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
                        modifier = Modifier.fillMaxWidth().testTag("replace_database_button"),
                    ) { Text("🔄 Database တစ်ခုလုံး အစားထိုးမည် (Full Replace)") }
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
