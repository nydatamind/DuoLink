package com.duolink.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.DuoLinkApp
import com.duolink.app.data.local.CallType
import com.duolink.app.data.repository.CallState
import com.duolink.app.ui.components.AvatarView
import com.duolink.app.ui.components.WebRtcLocalPreviewView
import com.duolink.app.ui.components.WebRtcRemoteVideoView
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun CallScreen(
    onCallEnded: () -> Unit
) {
    val app = DuoLinkApp.instance
    val callRepo = app.callRepository
    val vibration = app.vibrationAlertManager
    val scope = rememberCoroutineScope()

    val callState by callRepo.callState.collectAsState()
    val callType by callRepo.currentCallType.collectAsState()
    val durationSeconds by callRepo.callDurationSeconds.collectAsState()
    val remoteVideoTrack by callRepo.remoteVideoTrack.collectAsState()
    val pairingState by app.pairingRepository.pairingState.collectAsState(initial = null)
    val partnerName = pairingState?.peerDisplayName ?: "Partner"

    var isMicMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(callType == CallType.VIDEO) }

    LaunchedEffect(callState) {
        if (callState == CallState.INCOMING_RINGING) {
            vibration.startCallVibration()
        } else if (callState == CallState.CONNECTED || callState == CallState.ENDED || callState == CallState.FAILED) {
            vibration.stopCallVibration()
        }

        if (callState == CallState.ENDED || callState == CallState.FAILED) {
            onCallEnded()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            vibration.stopCallVibration()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
    ) {
        // VIDEO RENDERING BACKGROUND
        if (callType == CallType.VIDEO && callRepo.webRtcManager != null) {
            val webrtc = callRepo.webRtcManager!!
            if (remoteVideoTrack != null) {
                WebRtcRemoteVideoView(
                    webRtcManager = webrtc,
                    remoteTrack = remoteVideoTrack,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Floating PIP local camera
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .size(width = 110.dp, height = 150.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, DuoLinkRedPrimary, RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                WebRtcLocalPreviewView(
                    webRtcManager = webrtc,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // TOP CALL HEADER
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (callType == CallType.VOICE || remoteVideoTrack == null) {
                AvatarView(name = partnerName, sizeDp = 96)
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(
                text = partnerName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DuoLinkTextWhite
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            val statusText = when (callState) {
                CallState.OUTGOING_CALLING -> "Calling..."
                CallState.INCOMING_RINGING -> "Incoming ${callType.name.lowercase()} call..."
                CallState.CONNECTING -> "Connecting..."
                CallState.CONNECTED -> formatCallDuration(durationSeconds)
                CallState.RECONNECTING -> "Reconnecting..."
                CallState.ENDED -> "Call ended"
                CallState.FAILED -> "Call failed"
                CallState.IDLE -> ""
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = if (callState == CallState.CONNECTED) Color.Green else DuoLinkRedPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            )

            if (callState == CallState.CONNECTED) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Encrypted (DTLS-SRTP)",
                    fontSize = 12.sp,
                    color = DuoLinkTextMuted
                )
            }
        }

        // BOTTOM CALL CONTROLS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp, start = 24.dp, end = 24.dp)
        ) {
            if (callState == CallState.INCOMING_RINGING) {
                // Incoming controls: Reject (Red) and Accept (Green)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            vibration.stopCallVibration()
                            callRepo.rejectIncomingCall()
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(DuoLinkRedPrimary)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            vibration.stopCallVibration()
                            scope.launch { callRepo.acceptIncomingCall() }
                        },
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else {
                // Active call controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black)
                        .clip(RoundedCornerShape(32.dp))
                        .background(DuoLinkSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute
                    IconButton(
                        onClick = {
                            isMicMuted = !isMicMuted
                            callRepo.webRtcManager?.setMicrophoneMuted(isMicMuted)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isMicMuted) DuoLinkRedPrimary else DuoLinkCharcoalBg)
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute",
                            tint = Color.White
                        )
                    }

                    // Speaker
                    IconButton(
                        onClick = {
                            isSpeakerOn = !isSpeakerOn
                            callRepo.audioSwitchManager.setSpeakerphone(isSpeakerOn)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) DuoLinkRedPrimary else DuoLinkCharcoalBg)
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = "Speaker",
                            tint = Color.White
                        )
                    }

                    // Camera Switch (if Video Call)
                    if (callType == CallType.VIDEO) {
                        IconButton(
                            onClick = { callRepo.webRtcManager?.switchCamera() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(DuoLinkCharcoalBg)
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                        }
                    }

                    // End Call
                    IconButton(
                        onClick = {
                            vibration.stopCallVibration()
                            callRepo.endCall()
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = DuoLinkRedPrimary.copy(alpha = 0.5f))
                            .clip(CircleShape)
                            .background(DuoLinkRedPrimary)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

private fun formatCallDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
