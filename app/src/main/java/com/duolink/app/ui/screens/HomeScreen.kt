package com.duolink.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.DuoLinkApp
import com.duolink.app.R
import com.duolink.app.data.local.CallType
import com.duolink.app.ui.components.AvatarView
import com.duolink.app.ui.components.StatusBadge
import com.duolink.app.ui.mascot.OwlEyeState
import com.duolink.app.ui.mascot.OwlMascotView
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
fun HomeScreen(
    onNavigateToChat: () -> Unit,
    onNavigateToCall: () -> Unit,
    onNavigateToCallHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPairing: () -> Unit
) {
    val app = DuoLinkApp.instance
    val scope = rememberCoroutineScope()
    val pairingState by app.pairingRepository.pairingState.collectAsState(initial = null)
    val owlEyeState by app.owlEyeStateManager.owlEyeState.collectAsState()
    val isOnline by app.signalingClient.isPeerOnline.collectAsState()
    val lastSeen by app.signalingClient.peerLastSeen.collectAsState()
    val latestMessage by app.chatRepository.latestMessage.collectAsState(initial = null)
    val unreadCount by app.chatRepository.getUnreadCount().collectAsState(initial = 0)

    val partnerName = pairingState?.peerDisplayName ?: "Partner"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DuoLinkRedPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "DuoLink",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = DuoLinkTextWhite
                        )
                    )
                    Text(
                        text = "Sovereign 2-Person Link",
                        fontSize = 11.sp,
                        color = DuoLinkTextMuted
                    )
                }
            }

            Row {
                IconButton(onClick = onNavigateToCallHistory) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Call History",
                        tint = DuoLinkTextWhite
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = DuoLinkTextWhite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MAIN PARTNER CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OwlMascotView(
                    state = owlEyeState,
                    size = 96.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = partnerName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = DuoLinkTextWhite
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (owlEyeState) {
                        OwlEyeState.OPEN -> "● Active Now (Alert)"
                        OwlEyeState.HALF -> "◐ Sleepy (Inactive recently)"
                        OwlEyeState.CLOSED -> "○ Resting / Inactive"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (owlEyeState) {
                        OwlEyeState.OPEN -> Color(0xFF10B981)
                        OwlEyeState.HALF -> DuoLinkRedPrimary
                        OwlEyeState.CLOSED -> DuoLinkTextMuted
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DuoLinkCharcoalBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = DuoLinkRedPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "End-to-End Encrypted (ECDH P-256)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = DuoLinkTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HomeActionButton(
                        icon = Icons.Default.ChatBubble,
                        label = "Chat",
                        containerColor = DuoLinkRedPrimary,
                        contentColor = Color.White,
                        onClick = onNavigateToChat
                    )

                    HomeActionButton(
                        icon = Icons.Default.Call,
                        label = "Voice Call",
                        containerColor = DuoLinkCharcoalBg,
                        contentColor = DuoLinkRedPrimary,
                        onClick = {
                            scope.launch {
                                app.callRepository.startOutgoingCall(CallType.VOICE)
                                onNavigateToCall()
                            }
                        }
                    )

                    HomeActionButton(
                        icon = Icons.Default.Videocam,
                        label = "Video Call",
                        containerColor = DuoLinkCharcoalBg,
                        contentColor = Color.White,
                        onClick = {
                            scope.launch {
                                app.callRepository.startOutgoingCall(CallType.VIDEO)
                                onNavigateToCall()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RECENT CONVERSATION PREVIEW
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToChat() },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DuoLinkRedPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = null,
                        tint = DuoLinkRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Recent Message",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = DuoLinkTextWhite
                        )
                        if (latestMessage != null) {
                            Text(
                                text = formatMessageTime(latestMessage!!.timestamp),
                                fontSize = 11.sp,
                                color = DuoLinkTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = latestMessage?.content ?: "No messages yet. Say hello!",
                        fontSize = 13.sp,
                        color = DuoLinkTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(DuoLinkRedPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // BOTTOM STATUS BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DuoLinkSurface.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val stateName = app.signalingClient.state.collectAsState().value.name
            Text(
                text = "Signaling: $stateName",
                fontSize = 11.sp,
                color = DuoLinkTextMuted
            )
            Text(
                text = "P2P WebRTC Ready",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DuoLinkRedPrimary
            )
        }
    }
}

@Composable
private fun HomeActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .shadow(elevation = 4.dp, shape = CircleShape, spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(containerColor)
                .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = DuoLinkTextWhite
        )
    }
}

private fun formatLastSeen(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
