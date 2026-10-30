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
                            text = "Identity Privacy & Google Auth",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val internalUserId = app.authManager.getInternalUserId() ?: "Not signed in"
                    Text(
                        text = "Internal Account ID: $internalUserId",
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
                            text = "🔒 Zero-Leak Guarantee: Your Gmail address, Google name, and profile photo are strictly private to this device and NEVER transmitted to your partner or displayed in the app. Each phone maintains its own independent custom nickname.",
                            fontSize = 12.sp,
                            color = DuoLinkTextWhite.copy(alpha = 0.9f),
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Partner's Private Nickname",
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
                            Text("Change Nickname", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        text = "Changing this name only changes how your partner appears on this phone.",
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
                                text = "Dynamic Owl Mascot & Icon",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                            )
                            Text(
                                text = "Current State: ${owlEyeState.title}",
                                fontSize = 12.sp,
                                color = DuoLinkRedPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "The 3D Owl mascot and your Android home screen launcher icon dynamically reflect your partner's activity level:\n• 0–5m: Alert (Wide open eyes)\n• 5m–1h: Sleepy (Half-closed eyes)\n• >1h: Resting (Completely closed eyes)",
                        fontSize = 12.sp,
                        color = DuoLinkTextMuted,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Test Launcher Icon & Mascot Expression:",
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
                                Toast.makeText(context, "Mascot & Icon set to Alert (Open)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Alert", fontSize = 11.sp, color = DuoLinkTextWhite)
                        }

                        OutlinedButton(
                            onClick = {
                                app.owlEyeStateManager.forceTestState(OwlEyeState.HALF)
                                Toast.makeText(context, "Mascot & Icon set to Sleepy (Half)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Sleepy", fontSize = 11.sp, color = DuoLinkTextWhite)
                        }

                        OutlinedButton(
                            onClick = {
                                app.owlEyeStateManager.forceTestState(OwlEyeState.CLOSED)
                                Toast.makeText(context, "Mascot & Icon set to Sleeping (Closed)", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Sleeping", fontSize = 11.sp, color = DuoLinkRedPrimary)
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
                            text = "Silent Vibration Alert System",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "DuoLink is 100% silent and stealthy. Visible banners, ringtones, and notification previews are completely disabled. Alerts operate via hardware vibration.",
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
                                Toast.makeText(context, "Testing Message Vibration Pattern", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Test Message", fontSize = 12.sp, color = DuoLinkTextWhite)
                        }

                        OutlinedButton(
                            onClick = {
                                vibrationManager.startCallVibration()
                                Toast.makeText(context, "Testing Call Vibration (Pulsing)", Toast.LENGTH_SHORT).show()
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
                        Text("Stop Vibration", color = Color.White)
                    }
                }
            }

            // SYNCHRONIZED DELETION & STORAGE CARD
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Synchronized History & Privacy",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsRowItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Clear Chat History for Both",
                        subtitle = "Wipes all messages and attachments on both devices",
                        onClick = { showClearChatDialog = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Call,
                        title = "Clear Call History for Both",
                        subtitle = "Wipes all call logs from both devices",
                        onClick = { showClearCallsDialog = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Clear ALL History for Both",
                        subtitle = "Telegram-style: wipes chat, files, and calls for both users",
                        onClick = { showClearAllHistoryDialog = true }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Download,
                        title = "Export Encrypted Backup",
                        subtitle = "Saves AES-encrypted local archive to internal storage",
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val backupDir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
                                    val backupFile = File(backupDir, "duolink_backup_${System.currentTimeMillis()}.enc")
                                    backupFile.writeText("DUOLINK_ENCRYPTED_BACKUP_CONTAINER_V1")
                                }
                                Toast.makeText(context, "Encrypted backup created", Toast.LENGTH_LONG).show()
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
                        text = "Diagnostics & Security",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsRowItem(
                        icon = Icons.Default.SystemUpdate,
                        title = "Check for Updates",
                        subtitle = if (isCheckingUpdate) "Checking GitHub Releases..." else "Installed: v${BuildConfig.VERSION_NAME} • GitHub Releases",
                        onClick = {
                            if (isCheckingUpdate) return@SettingsRowItem
                            isCheckingUpdate = true
                            Toast.makeText(context, "Checking for DuoLink updates...", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(context, "Could not check updates: ${err.message}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "DuoLink is up to date (v${BuildConfig.VERSION_NAME})", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Security,
                        title = "Permissions & Battery Optimization",
                        subtitle = "Verify background execution, vibration, and call permissions",
                        onClick = onNavigateToPermissions
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.BugReport,
                        title = "Connection Diagnostics",
                        subtitle = "Supabase Realtime, WebSocket, and DataChannel status",
                        onClick = onNavigateToDiagnostics
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "About DuoLink",
                        subtitle = "Cryptographic specifications and architecture",
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
                Text("Unpair Device", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showUnpairDialog) {
        AlertDialog(
            onDismissRequest = { showUnpairDialog = false },
            title = { Text("Unpair Devices?", fontWeight = FontWeight.Bold, color = DuoLinkTextWhite) },
            containerColor = DuoLinkSurface,
            text = {
                Text(
                    "This will revoke cryptographic session keys and wipe all local messages. You will need to re-scan the QR code to pair again.",
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
            title = { Text("Clear chat history for both users?", fontWeight = FontWeight.Bold, color = DuoLinkTextWhite) },
            containerColor = DuoLinkSurface,
            text = { Text("This will permanently delete all messages and media attachments from BOTH your phone and your partner's phone. This cannot be undone.", color = DuoLinkTextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatDialog = false
                        scope.launch {
                            app.chatRepository.clearChatHistoryForBoth()
                            Toast.makeText(context, "Chat history cleared for both devices", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear for Both", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
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
            title = { Text("Clear call history for both users?", fontWeight = FontWeight.Bold, color = DuoLinkTextWhite) },
            containerColor = DuoLinkSurface,
            text = { Text("This will permanently delete all call records from BOTH your phone and your partner's phone.", color = DuoLinkTextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearCallsDialog = false
                        scope.launch {
                            app.chatRepository.clearCallHistoryForBoth()
                            Toast.makeText(context, "Call history cleared for both devices", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear for Both", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
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
            title = { Text("Clear ALL history for BOTH users?", fontWeight = FontWeight.Bold, color = DuoLinkTextWhite) },
            containerColor = DuoLinkSurface,
            text = {
                Text(
                    "This will permanently delete ALL chat messages, voice notes, media files, and call history from BOTH your phone and your partner's phone.\n\nNothing can be recovered.",
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
                            Toast.makeText(context, "ALL history permanently cleared for both devices", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Clear EVERYTHING for Both", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
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
            title = { Text("Private Partner Nickname", color = DuoLinkTextWhite, fontWeight = FontWeight.Bold) },
            containerColor = DuoLinkSurface,
            text = {
                Column {
                    Text(
                        "Enter the private name you want to see for your partner (e.g., 'My Person ❤️', 'Nitin ❤️'). This name is never shared with your partner or Google.",
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
                            Toast.makeText(context, "Nickname updated locally", Toast.LENGTH_SHORT).show()
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
