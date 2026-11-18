package com.example.offlinepos.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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

    val firebaseUser by viewModel.firebaseUser.collectAsState()
    val isFirebaseSigningIn by viewModel.isFirebaseSigningIn.collectAsState()
    val firebaseAuthError by viewModel.firebaseAuthError.collectAsState()
    val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()
    val lastFirestoreSyncTime by viewModel.lastFirestoreSyncTime.collectAsState()
    val outboxPendingCount by viewModel.outboxPendingCount.collectAsState()
    val firebaseProjectId = remember(context) {
        try {
            com.google.firebase.FirebaseApp.getInstance().options.projectId ?: "fir-winsoe"
        } catch (_: Exception) {
            "fir-winsoe"
        }
    }

    var backupStatusMessage by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showRestoreChoice by remember { mutableStateOf(false) }
    var showFirebaseRestoreDialog by remember { mutableStateOf(false) }
    var expandAutoMergeDetails by remember { mutableStateOf(false) }

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
            colors = CardDefaults.cardColors(
                containerColor = if (firebaseUser != null) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
            ),
            border = BorderStroke(
                1.5.dp,
                if (firebaseUser != null) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = if (firebaseUser != null) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔥", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Firebase Firestore Cloud Sync",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (firebaseUser != null) Color(0xFF14532D) else Color(0xFF991B1B)
                            )
                            Text(
                                "Project: $firebaseProjectId • Google Account ချိတ်ဆက်မှု",
                                fontSize = 11.sp,
                                color = if (firebaseUser != null) Color(0xFF166534) else Color(0xFF7F1D1D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        modifier = Modifier.testTag("firebase_auth_status_badge"),
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isFirebaseSigningIn -> Color(0xFFFEF3C7)
                            firebaseUser != null -> Color(0xFFDCFCE7)
                            else -> Color(0xFFFEE2E2)
                        }
                    ) {
                        Text(
                            text = when {
                                isFirebaseSigningIn -> "⏳ ချိတ်ဆက်နေသည်..."
                                firebaseUser != null -> "✓ Logged In"
                                else -> "✕ Not Logged In"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFirebaseSigningIn -> Color(0xFFB45309)
                                firebaseUser != null -> Color(0xFF15803D)
                                else -> Color(0xFFB91C1C)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (firebaseUser != null) {
                    // Explicit Logged In Confirmation Strip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFF4ADE80)),
                        modifier = Modifier.fillMaxWidth().testTag("firebase_logged_in_banner")
                    ) {
                        Text(
                            text = "✓ Login ဝင်ပြီးပါပြီ (Firebase Cloud Active)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Logged in user info
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
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
                                        text = firebaseUser?.email ?: "(Email မရှိပါ)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "UID: ${firebaseUser?.uid?.take(8) ?: ""}... • Project: $firebaseProjectId",
                                        fontSize = 10.sp,
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

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "ပါဝင်သည့် Cloud Backup/Restore စာရင်း: ကုန်ပစ္စည်း၊ အရောင်းဘောင်ချာနှင့် ပစ္စည်းစာရင်းများ၊ ငွေချေမှု၊ ဝယ်သူနှင့် အကြွေးစာရင်း၊ Supplier၊ အဝယ်၊ Return၊ Shift၊ Stock Movement၊ Receipt Template နှင့် ဆိုင်အချက်အလက်များ။",
                        fontSize = 10.sp,
                        color = Color(0xFF475569),
                        lineHeight = 14.sp,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerFirestoreSync("Settings Sync Button") },
                            modifier = Modifier.weight(1f).testTag("firestore_sync_now_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            enabled = !isFirestoreSyncing
                        ) {
                            if (isFirestoreSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sync လုပ်နေသည်...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⚡ Cloud Backup (Sync All)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.signOutGoogle(context.findActivity()) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            enabled = !isFirestoreSyncing
                        ) {
                            Text("အကောင့်ထွက်မည်", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showFirebaseRestoreDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("firebase_full_restore_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF14532D)),
                        border = BorderStroke(1.dp, Color(0xFF16A34A)),
                        enabled = !isFirestoreSyncing,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (isFirestoreSyncing) "⏳ Firebase Sync/Restore လုပ်နေသည်..." else "📥 Firebase မှ Full Restore ပြန်ယူမည်",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Not logged in: Show Google Sign-In prompt & any inline Auth Error diagnostics
                    Text(
                        text = "Google Account ဖြင့် ဝင်ရောက်ထားပါက ကုန်ပစ္စည်းများ၊ အရောင်းဘောင်ချာများနှင့် ဖောက်သည်အကြွေးစာရင်းများကို Cloud Firestore ($firebaseProjectId) သို့ အချိန်နှင့်တပြေးညီ အလိုအလျောက် Sync သိမ်းဆည်းပေးမည်ဖြစ်ပါသည်။",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "မှတ်ချက်: ဖုန်း Settings > Accounts ထဲတွင် Google Account (ဥပမာ winsoemoung@gmail.com) ဝင်ထားရန် လိုအပ်ပါသည်။",
                        fontSize = 11.sp,
                        color = Color(0xFF7F1D1D),
                        lineHeight = 15.sp
                    )

                    if (!firebaseAuthError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFF87171)),
                            modifier = Modifier.fillMaxWidth().testTag("firebase_auth_error_banner")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚠️ Firebase Login မဝင်နိုင်ပါ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                    TextButton(
                                        onClick = { viewModel.clearFirebaseAuthError() },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("ပိတ်မည်", fontSize = 10.sp, color = Color(0xFF991B1B))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(
                                        text = firebaseAuthError ?: "",
                                        fontSize = 11.sp,
                                        color = Color(0xFF7F1D1D),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val activity = context.findActivity()
                            if (activity != null) {
                                viewModel.signInWithGoogle(activity) { success, err ->
                                    if (success) {
                                        backupStatusMessage = "Google Account ဖြင့် Firebase Login အောင်မြင်စွာ ချိတ်ဆက်ပြီးပါပြီ"
                                    } else if (err != null) {
                                        backupStatusMessage = err
                                    }
                                }
                            } else {
                                val msg = "Activity Context မတွေ့ပါ — App ကို ပြန်ဖွင့်ပြီး ထပ်မံစမ်းသပ်ပါ"
                                backupStatusMessage = msg
                                viewModel.setBanner("⚠️ $msg")
                            }
                        },
                        enabled = !isFirebaseSigningIn,
                        modifier = Modifier.fillMaxWidth().testTag("google_signin_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isFirebaseSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Google Account ချိတ်ဆက်နေပါသည်...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("G ", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFFFBBF24))
                                Text("Google Account ဖြင့် Sign In ဝင်မည်", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Compact Two-way Conflict Resolution Auto-merge Status Strip
        val recordSyncPrefs = remember(context) { context.getSharedPreferences("pos_sync_status", 0) }
        val recordSyncTime = recordSyncPrefs.getLong("last_record_sync_ms", 0L)
        val recordCount = recordSyncPrefs.getInt("last_record_count", 0)
        val conflictsResolved = recordSyncPrefs.getInt("last_conflicts_resolved", 0)
        val conflictSummary = recordSyncPrefs.getString("last_conflict_summary", null)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expandAutoMergeDetails = !expandAutoMergeDetails }
                .testTag("two_way_automerge_compact_card"),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("🔄 Two-way Auto-merge", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFDCFCE7)) {
                            Text(
                                "✓ Active",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (conflictsResolved > 0) "Conflicts $conflictsResolved ခု ညှိပြီး" else "SKU-based merge",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = if (expandAutoMergeDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Auto-merge details",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AnimatedVisibility(visible = expandAutoMergeDetails) {
                    Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "Owner နှင့် Cashier စက်များကြား SKU-based identity ဖြင့် ကုန်ပစ္စည်း၊ ဝယ်သူ၊ အရောင်းနှင့် ငွေစာရင်းများကို အလိုအလျောက် ပဋိပက္ခမရှိ ပေါင်းစပ်ပေးပါသည်။",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                        if (recordSyncTime > 0L) {
                            val formatted = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(recordSyncTime))
                            Text("နောက်ဆုံး Merge: $formatted ($recordCount records)", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        conflictSummary?.let {
                            Text(it, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Firebase Real-time Auto-Sync Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("firebase_realtime_autosync_card"),
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
                    Text("⚡ Firebase Real-time Auto-sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isFirestoreSyncing -> Color(0xFFDBEAFE)
                            firebaseUser != null -> Color(0xFFDCFCE7)
                            else -> Color(0xFFFEF3C7)
                        }
                    ) {
                        Text(
                            text = when {
                                isFirestoreSyncing -> "⏳ Syncing..."
                                firebaseUser != null -> "⚡ Auto-sync Active"
                                else -> "Sign In လိုအပ်"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFirestoreSyncing -> Color(0xFF2563EB)
                                firebaseUser != null -> Color(0xFF15803D)
                                else -> Color(0xFFB45309)
                            },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "ပစ္စည်းအသစ်သွင်းခြင်း၊ အရောင်းဘေလ်ဖွင့်ခြင်း၊ ငွေစာရင်း/အကြွေး ပြင်ဆင်တိုင်း Firebase Firestore ($firebaseProjectId) သို့ ချက်ချင်း Auto-sync ပြုလုပ်ပေးပါသည်။ အော့ဖ်လိုင်းဖြစ်နေပါက Queue တွင် စောင့်ဆိုင်းပြီး လိုင်းပြန်ရသည်နှင့် အလိုအလျောက် ပို့ပေးပါသည် (Offline-first Resilience)။",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (firebaseUser != null) "✓ ချိတ်ဆက်ထားသော Account: ${firebaseUser?.email ?: "Logged In"}" else "⚠️ Firebase Cloud Account Sign In ဝင်ရန် လိုအပ်ပါသည်",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (firebaseUser != null) Color(0xFF15803D) else Color(0xFFB45309)
                        )
                        if (isFirestoreSyncing) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(firestoreSyncStatus ?: "Firebase Cloud သို့ Auto-sync လုပ်နေပါသည်...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            firestoreSyncStatus?.let { status ->
                                Text(
                                    text = status,
                                    fontSize = 11.sp,
                                    color = if (status.contains("မအောင်မြင်") || status.contains("မဝင်")) Color(0xFFB91C1C) else Color(0xFF15803D),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (lastFirestoreSyncTime > 0L) {
                            val formattedTime = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(lastFirestoreSyncTime))
                            Text("နောက်ဆုံး Firebase Sync: $formattedTime", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (firebaseUser != null) {
                            viewModel.triggerFirebaseRealtimeSync("Manual Realtime Sync Button")
                            viewModel.setBanner("⚡ Firebase Real-time Auto-Sync စတင်နေပါသည်...")
                        } else {
                            val activity = context.findActivity()
                            if (activity != null) {
                                viewModel.signInWithGoogle(activity) { success, err ->
                                    if (success) {
                                        backupStatusMessage = "Google Account ဖြင့် Firebase Auto-Sync စတင်ပြီးပါပြီ"
                                    } else if (err != null) {
                                        backupStatusMessage = err
                                    }
                                }
                            } else {
                                viewModel.setBanner("⚠️ ဦးစွာ အပေါ်ရှိ Firebase Card တွင် Sign In ဝင်ပေးပါ")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("firebase_realtime_sync_now_button"),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isFirestoreSyncing && !isFirebaseSigningIn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D))
                ) {
                    if (isFirestoreSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Firebase Real-time Sync လုပ်နေသည်...", fontSize = 11.sp)
                    } else {
                        Text(
                            text = if (firebaseUser != null) "⚡ ယခု Firebase Real-time Sync ပြုလုပ်မည်" else "🔑 Firebase Sign In ဝင်ပြီး Auto-sync စတင်မည်",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Local SQLite Database -> Firebase Sync & Backup Card
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
                        Text("💾 Local Database Sync & Firebase Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Room SQLite v3 • Firebase Cloud Sync Only", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (outboxPendingCount > 0) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = if (outboxPendingCount > 0) "Pending: $outboxPendingCount item${if (outboxPendingCount > 1) "s" else ""}" else "✓ Firebase Synced",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (outboxPendingCount > 0) Color(0xFFB45309) else Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Outbox pending queue status banner (active progress only while actually syncing)
                AnimatedVisibility(
                    visible = outboxPendingCount > 0,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
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
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    if (isFirestoreSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFFD97706)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = when {
                                            isFirestoreSyncing -> "⏳ Firebase Cloud သို့ $outboxPendingCount ခု Sync တင်နေပါသည်..."
                                            firebaseUser == null -> "🔑 Firebase Sign In ဝင်ရန် စောင့်ဆိုင်းနေသည် ($outboxPendingCount ခု)"
                                            else -> "⏳ Sync တင်ရန် ကျန်ရှိနေသေးသည် ($outboxPendingCount ခု)"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Text(
                                    "Pending: $outboxPendingCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                            if (isFirestoreSyncing) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp)),
                                    color = Color(0xFFD97706),
                                    trackColor = Color(0xFFFDE68A)
                                )
                            }
                            Text(
                                text = when {
                                    isFirestoreSyncing -> "အော့ဖ်လိုင်း ပြုလုပ်ထားသော အရောင်း/ပစ္စည်း ပြင်ဆင်မှု $outboxPendingCount ခုကို Firebase Cloud သို့ Sync တင်နေပါသည်။"
                                    firebaseUser == null -> "အပေါ်ရှိ Google Account ဖြင့် Firebase Sign In ဝင်ရောက်ပြီးပါက Pending ဒေတာ $outboxPendingCount ခုကို အလိုအလျောက် Sync တင်ပေးပါမည်။"
                                    else -> "အင်တာနက်လိုင်း ရရှိချိန်တွင် အလိုအလျောက် Sync တင်ပေးမည်ဖြစ်ပြီး အောက်ပါ '🔥 Firebase Sync & Backup' ခလုတ်ကို နှိပ်၍လည်း ချက်ချင်း တင်နိုင်ပါသည်။"
                                },
                                fontSize = 10.sp,
                                color = Color(0xFF92400E),
                                lineHeight = 14.sp
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
                        Text("🔥", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Local Database ထဲရှိ ဘောင်ချာမှတ်တမ်းများ၊ အရောင်းပြေစာများ၊ ကုန်ပစ္စည်းများနှင့် အကြွေးစာရင်း အားလုံးကို Firebase Cloud ဖြင့်သာ တိုက်ရိုက် Sync & Backup ပြုလုပ်ပါသည်။",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Primary Firebase Sync & Restore buttons inside Local DB card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (firebaseUser != null) {
                                viewModel.triggerFirestoreSync("Local DB Firebase Backup")
                                backupStatusMessage = "Firebase Cloud သို့ Sync & Backup စတင်နေပါသည်..."
                            } else {
                                val msg = "Firebase Cloud Backup တင်ရန် အပေါ်ရှိ Google Sign-In ကို ဦးစွာ ဝင်ပေးပါ"
                                backupStatusMessage = msg
                                viewModel.setBanner("⚠️ $msg")
                            }
                        },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(1.2f).testTag("local_db_firebase_sync_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("🔥 Firebase Sync & Backup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (firebaseUser != null) {
                                showFirebaseRestoreDialog = true
                            } else {
                                val msg = "Firebase မှ Restore ယူရန် ဦးစွာ Google Sign-In ဝင်ပေးပါ"
                                backupStatusMessage = msg
                                viewModel.setBanner("⚠️ $msg")
                            }
                        },
                        enabled = !isFirestoreSyncing,
                        modifier = Modifier.weight(0.95f).testTag("local_db_firebase_restore_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📥 Firebase Restore", fontSize = 11.sp)
                    }
                }

                HorizontalDivider()
                Text(
                    "📁 စက်ထဲသို့ Offline ဖိုင် (.db / CSV) သီးသန့် ထုတ်ယူ/ပြန်တင်ရန်:",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
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
                        Text("📤 .db ဖိုင်ထုတ်မည်", fontSize = 11.sp)
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
                        Text("📥 .db ဖိုင်သွင်းမည်", fontSize = 11.sp)
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

    if (showFirebaseRestoreDialog) {
        Dialog(onDismissRequest = { showFirebaseRestoreDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Firebase Cloud Restore ရွေးချယ်မှု",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• 'Full Restore (အားလုံးအစားထိုးမည်)' သည် လက်ရှိစက်ထဲရှိ POS Data များကို Firebase Cloud Backup ဖြင့် အပြည့်အစုံ အစားထိုးပြန်တင်ပေးပါမည်။\n• 'Merge Restore (ပေါင်းစပ်မည်)' သည် လက်ရှိ Data မပျက်ဘဲ Cloud ထဲရှိ မရှိသေးသော ဘောင်ချာနှင့် စာရင်းများကို ပေါင်းစပ်ထည့်သွင်းပေးပါမည်။",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showFirebaseRestoreDialog = false
                            viewModel.restoreFromFirebase(replaceLocal = true) { _, msg ->
                                backupStatusMessage = msg
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("firebase_restore_overwrite_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    ) {
                        Text("🔄 Full Restore (Cloud မှ အားလုံးအစားထိုးမည်)")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showFirebaseRestoreDialog = false
                            viewModel.restoreFromFirebase(replaceLocal = false) { _, msg ->
                                backupStatusMessage = msg
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("firebase_restore_merge_button"),
                    ) {
                        Text("✨ Merge Restore (Data မပျက်ဘဲ ပေါင်းစပ်မည်)")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { showFirebaseRestoreDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
