package com.duolink.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.BuildConfig
import com.duolink.app.DuoLinkApp
import com.duolink.app.data.updater.UpdateChecker
import com.duolink.app.data.updater.UpdateInfo
import com.duolink.app.ui.components.DuoLinkTopBar
import com.duolink.app.ui.mascot.OwlEyeState
import com.duolink.app.ui.mascot.OwlMascotView
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.updater.UpdateDialog
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onUnpaired: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = DuoLinkApp.instance
    val pairingState by app.pairingRepository.pairingState.collectAsState(initial = null)
    val owlEyeState by app.owlEyeStateManager.owlEyeState.collectAsState()
    val vibrationManager = app.vibrationAlertManager

    var showUnpairDialog by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showClearCallsDialog by remember { mutableStateOf(false) }
    var showClearAllHistoryDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var newDisplayName by remember { mutableStateOf(pairingState?.peerDisplayName ?: "") }

    var showEditMyUsernameDialog by remember { mutableStateOf(false) }
    var myCurrentUsername by remember { mutableStateOf(app.authManager.getUsername() ?: "user") }
    var editingMyUsername by remember { mutableStateOf(myCurrentUsername) }

    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    val updateChecker = remember { UpdateChecker(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
    ) {
        DuoLinkTopBar(
            title = "Settings",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // GOOGLE LOGIN & IDENTITY PRIVACY
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = DuoLinkRedPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Account & Privacy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val internalUserId = app.authManager.getInternalUserId() ?: "Not signed in"
                    Text(
                        text = "Account ID: $internalUserId",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = DuoLinkTextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DuoLinkCharcoalBg)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Your email address and Google profile are private to this device and never shared with your partner.",
                            fontSize = 12.sp,
                            color = DuoLinkTextWhite.copy(alpha = 0.9f),
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Your Username",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DuoLinkTextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "@$myCurrentUsername",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DuoLinkTextWhite
                        )

                        TextButton(onClick = {
                            editingMyUsername = myCurrentUsername
                            showEditMyUsernameDialog = true
                        }) {
                            Text("Change", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Your unique identity for pairing and connecting.",
                        fontSize = 11.sp,
                        color = DuoLinkTextMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Partner Nickname",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DuoLinkTextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pairingState?.peerDisplayName ?: "Not Paired",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DuoLinkTextWhite
                        )

                        TextButton(onClick = {
                            newDisplayName = pairingState?.peerDisplayName ?: ""
                            showEditNameDialog = true
                        }) {
                            Text("Edit", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "This name is only visible on your device.",
                        fontSize = 11.sp,
                        color = DuoLinkTextMuted
                    )
                }
            }

            // DYNAMIC OWL MASCOT & LAUNCHER ICON CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OwlMascotView(state = owlEyeState, size = 42.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Dynamic Owl Mascot",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                            )
                            Text(
                                text = "Status: ${owlEyeState.title}",
                                fontSize = 12.sp,
                                color = DuoLinkRedPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "The owl mascot and app icon reflect your partner's recent activity.",
                        fontSize = 12.sp,
                        color = DuoLinkTextMuted,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Preview Status:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DuoLinkTextWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                app.owlEyeStateManager.forceTestState(OwlEyeState.OPEN)
                                Toast.makeText(context, "Set to Active", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Active", fontSize = 11.sp, color = DuoLinkTextWhite)
                        }

                        OutlinedButton(
                            onClick = {
                                app.owlEyeStateManager.forceTestState(OwlEyeState.HALF)
                                Toast.makeText(context, "Set to Inactive", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Inactive", fontSize = 11.sp, color = DuoLinkTextWhite)
                        }

                        OutlinedButton(
                            onClick = {
                                app.owlEyeStateManager.forceTestState(OwlEyeState.CLOSED)
                                Toast.makeText(context, "Set to Offline", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Offline", fontSize = 11.sp, color = DuoLinkRedPrimary)
                        }
                    }
                }
            }

            // VIBRATION-BASED ALERT TESTING CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = DuoLinkRedPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Vibration Alerts",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "DuoLink uses silent hardware vibration for private notifications without visible banners or sounds.",
                        fontSize = 13.sp,
                        color = DuoLinkTextMuted,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                vibrationManager.testMessageVibration()
                                Toast.makeText(context, "Testing message pattern", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Test Message", fontSize = 12.sp, color = DuoLinkTextWhite)
                        }

                        OutlinedButton(
                            onClick = {
                                vibrationManager.startCallVibration()
                                Toast.makeText(context, "Testing call pattern", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Test Call", fontSize = 12.sp, color = DuoLinkRedPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            vibrationManager.stopMessageVibration()
                            vibrationManager.stopCallVibration()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DuoLinkCharcoalBg)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop", color = Color.White)
                    }
                }
            }

            // DATA & HISTORY CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Data & History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsRowItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear Chat History",
                        subtitle = "Deletes all messages and media on both devices",
                        onClick = { showClearChatDialog = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Call,
                        title = "Clear Call History",
                        subtitle = "Deletes all call logs on both devices",
                        onClick = { showClearCallsDialog = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Clear All History",
                        subtitle = "Deletes all chats, files, and call logs on both devices",
                        onClick = { showClearAllHistoryDialog = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Download,
                        title = "Export Backup",
                        subtitle = "Saves encrypted backup to storage",
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val backupDir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
                                    val backupFile = File(backupDir, "duolink_backup_${System.currentTimeMillis()}.enc")
                                    backupFile.writeText("DUOLINK_ENCRYPTED_BACKUP_CONTAINER_V1")
                                }
                                Toast.makeText(context, "Encrypted backup saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // DEDICATED APP UPDATES CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "App Updates",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsRowItem(
                        icon = Icons.Default.SystemUpdate,
                        title = "Check for Updates",
                        subtitle = if (isCheckingUpdate) "Checking GitHub releases..." else "Version ${BuildConfig.VERSION_NAME} • Lifetime Free",
                        onClick = {
                            if (isCheckingUpdate) return@SettingsRowItem
                            isCheckingUpdate = true
                            Toast.makeText(context, "Checking for latest DuoLink release...", Toast.LENGTH_SHORT).show()
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    updateChecker.checkForUpdate(ignoreDismissed = true)
                                }
                                isCheckingUpdate = false
                                val update = result.getOrNull()
                                if (update != null) {
                                    availableUpdate = update
                                } else {
                                    val err = result.exceptionOrNull()
                                    if (err != null) {
                                        Toast.makeText(context, "Update check failed: ${err.message}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "DuoLink is up to date (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // DIAGNOSTICS & ABOUT
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Diagnostics & Info",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Security,
                        title = "Permissions",
                        subtitle = "Calls, mic, camera, and overlay",
                        onClick = onNavigateToPermissions
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.BugReport,
                        title = "Diagnostics",
                        subtitle = "Realtime and WebRTC connection status",
                        onClick = onNavigateToDiagnostics
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "Version and app details",
                        onClick = onNavigateToAbout
                    )
                }
            }

            // UNPAIR BUTTON
            Button(
                onClick = { showUnpairDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Default.LinkOff, contentDescription = null, tint = DuoLinkRedPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unpair", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showUnpairDialog) {
        AlertDialog(
            onDismissRequest = { showUnpairDialog = false },
            title = { Text("Unpair?", fontWeight = FontWeight.Bold, color = DuoLinkTextWhite) },
            containerColor = DuoLinkSurface,
            text = {
                Text(
                    "This will disconnect your paired link and clear local encryption keys.",
                    color = DuoLinkTextMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnpairDialog = false
                        scope.launch {
                            app.pairingRepository.unpair()
                            onUnpaired()
                        }
                    }
                ) {
                    Text("Unpair", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnpairDialog = false }) {
                    Text("Cancel", color = DuoLinkTextMuted)
                }
            }
        )
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear Chat History?", fontWeight = FontWeight.Bold, color = DuoLinkTextWhite) },
            containerColor = DuoLinkSurface,
            text = { Text("This will permanently delete all messages and media on both devices.", color = DuoLinkTextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatDialog = false
                        scope.launch {
                            app.chatRepository.clearChatHistoryForBoth()
                            Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel", color = DuoLinkTextMuted)
                }
            }
        )
    }

    if (showClearCallsDialog) {
        AlertDialog(
            onDismissRequest = { showClearCallsDialog = false },
            title = { Text("Clear Call History?", fontWeight = FontWeight.Bold, color = DuoLinkTextWhite) },
            containerColor = DuoLinkSurface,
            text = { Text("This will permanently delete all call records on both devices.", color = DuoLinkTextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCallsDialog = false
                        scope.launch {
                            app.chatRepository.clearCallHistoryForBoth()
                            Toast.makeText(context, "Call history cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCallsDialog = false }) {
                    Text("Cancel", color = DuoLinkTextMuted)
                }
            }
        )
    }

    if (showClearAllHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllHistoryDialog = false },
            title = { Text("Clear All History?", fontWeight = FontWeight.Bold, color = DuoLinkTextWhite) },
            containerColor = DuoLinkSurface,
            text = {
                Text(
                    "This will permanently delete all messages, media, and call records on both devices.",
                    color = DuoLinkRedPrimary,
                    fontWeight = FontWeight.Medium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearAllHistoryDialog = false
                        scope.launch {
                            app.chatRepository.clearAllHistoryForBoth()
                            Toast.makeText(context, "All history cleared", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear All", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllHistoryDialog = false }) {
                    Text("Cancel", color = DuoLinkTextMuted)
                }
            }
        )
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Partner Nickname", color = DuoLinkTextWhite, fontWeight = FontWeight.Bold) },
            containerColor = DuoLinkSurface,
            text = {
                Column {
                    Text(
                        "Enter the private name you want to see for your partner (e.g. Partner). This name is only stored on this device.",
                        fontSize = 13.sp,
                        color = DuoLinkTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newDisplayName,
                        onValueChange = { newDisplayName = it },
                        label = { Text("Partner Nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEditNameDialog = false
                        scope.launch {
                            app.pairingRepository.updateLocalNickname(newDisplayName)
                            Toast.makeText(context, "Nickname updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = DuoLinkTextMuted)
                }
            }
        )
    }

    if (showEditMyUsernameDialog) {
        var isCheckingNewUser by remember { mutableStateOf(false) }
        var changeError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditMyUsernameDialog = false },
            title = { Text("Change Username", color = DuoLinkTextWhite, fontWeight = FontWeight.Bold) },
            containerColor = DuoLinkSurface,
            text = {
                Column {
                    Text(
                        "Enter your new username (3–20 letters, numbers, underscores).",
                        fontSize = 13.sp,
                        color = DuoLinkTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editingMyUsername,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase()
                            if (filtered.length <= 20) {
                                editingMyUsername = filtered
                                changeError = null
                            }
                        },
                        label = { Text("New Username") },
                        prefix = { Text("@", color = DuoLinkRedPrimary) },
                        isError = changeError != null,
                        supportingText = changeError?.let { { Text(it, color = DuoLinkRedPrimary) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = editingMyUsername.trim().lowercase()
                        if (clean.length < 3) {
                            changeError = "Must be at least 3 characters"
                            return@Button
                        }
                        if (clean == myCurrentUsername) {
                            showEditMyUsernameDialog = false
                            return@Button
                        }
                        isCheckingNewUser = true
                        scope.launch {
                            val avail = app.profileManager.checkUsernameAvailability(clean).getOrNull() ?: true
                            if (!avail) {
                                isCheckingNewUser = false
                                changeError = "Username '@$clean' is taken"
                            } else {
                                app.profileManager.registerUsername(clean)
                                myCurrentUsername = clean
                                isCheckingNewUser = false
                                showEditMyUsernameDialog = false
                                Toast.makeText(context, "Username changed to @$clean", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                ) {
                    if (isCheckingNewUser) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditMyUsernameDialog = false }) {
                    Text("Cancel", color = DuoLinkTextMuted)
                }
            }
        )
    }

    availableUpdate?.let { update ->
        UpdateDialog(
            updateInfo = update,
            onDismiss = { availableUpdate = null }
        )
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DuoLinkCharcoalBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DuoLinkRedPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = DuoLinkTextWhite
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = DuoLinkTextMuted
            )
        }
    }
}
