package com.duolink.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.DuoLinkApp
import com.duolink.app.data.local.CallDirection
import com.duolink.app.data.local.CallType
import com.duolink.app.ui.components.DuoLinkTopBar
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallHistoryScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val app = DuoLinkApp.instance
    val scope = rememberCoroutineScope()
    val callLogs by app.database.callLogDao().getAllCallLogs().collectAsState(initial = emptyList())
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
    ) {
        DuoLinkTopBar(
            title = "Call History",
            onBackClick = onBackClick,
            actions = {
                if (callLogs.isNotEmpty()) {
                    IconButton(onClick = { showClearConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Call History for Both", tint = DuoLinkRedPrimary)
                    }
                }
            }
        )

        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No calls logged yet",
                    color = DuoLinkTextMuted,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(callLogs, key = { it.id }) { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DuoLinkSurface)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isVideo = log.callType == CallType.VIDEO
                        val isMissed = log.direction == CallDirection.MISSED
                        val iconColor = if (isMissed) DuoLinkRedPrimary else if (isVideo) Color(0xFF10B981) else Color.White

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(iconColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.peerDisplayName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = DuoLinkTextWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val directionIcon = when (log.direction) {
                                    CallDirection.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
                                    CallDirection.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
                                    CallDirection.MISSED -> Icons.AutoMirrored.Filled.CallMissed
                                }
                                Icon(
                                    imageVector = directionIcon,
                                    contentDescription = null,
                                    tint = if (isMissed) DuoLinkRedPrimary else Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatLogDate(log.startedAt),
                                    fontSize = 12.sp,
                                    color = DuoLinkTextMuted
                                )
                            }
                        }

                        if (log.durationSeconds > 0) {
                            Text(
                                text = formatCallDuration(log.durationSeconds),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = DuoLinkTextMuted
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear call history for both users?",
                    fontWeight = FontWeight.Bold,
                    color = DuoLinkTextWhite
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all call records from BOTH your phone and your partner's phone. Call logs cannot be recovered.",
                    color = DuoLinkTextMuted,
                    fontSize = 14.sp
                )
            },
            containerColor = DuoLinkSurface,
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmDialog = false
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
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = DuoLinkTextMuted)
                }
            }
        )
    }
}

private fun formatLogDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatCallDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}
