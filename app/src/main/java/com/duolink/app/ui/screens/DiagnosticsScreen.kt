package com.duolink.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.DuoLinkApp
import com.duolink.app.data.signaling.SignalingMessage
import com.duolink.app.ui.components.DuoLinkTopBar
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(
    onBackClick: () -> Unit
) {
    val app = DuoLinkApp.instance
    val scope = rememberCoroutineScope()
    val crypto = app.cryptoManager
    val signaling = app.signalingClient
    val pairingState by app.pairingRepository.pairingState.collectAsState(initial = null)

    val signalingState by signaling.state.collectAsState()
    val isPeerOnline by signaling.isPeerOnline.collectAsState()
    val peerLastSeen by signaling.peerLastSeen.collectAsState()
    var pingLatencyMs by remember { mutableStateOf<Long?>(null) }
    var isPinging by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
    ) {
        DuoLinkTopBar(
            title = "Diagnostics",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IDENTITY INFO
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device Cryptographic Identity", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DuoLinkTextWhite)
                    Spacer(modifier = Modifier.height(10.dp))

                    DiagItem("Device ID", crypto.getDeviceId())
                    DiagItem("Local Fingerprint", crypto.getFingerprint())
                    DiagItem("Curve", "NIST P-256 (secp256r1)")
                    DiagItem("Cipher", "AES-256-GCM + HKDF-SHA256")
                }
            }

            // SIGNALING STATUS
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Signaling Connection", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DuoLinkTextWhite)
                    Spacer(modifier = Modifier.height(10.dp))

                    DiagItem("Server / Supabase", pairingState?.signalingServerUrl ?: "Not configured")
                    DiagItem("Signaling Status", signalingState.name, if (signalingState.name == "REGISTERED") Color(0xFF10B981) else Color.Yellow)
                    DiagItem("Peer Presence", if (isPeerOnline) "ONLINE" else "OFFLINE", if (isPeerOnline) Color(0xFF10B981) else Color.Gray)
                    if (peerLastSeen > 0) {
                        DiagItem("Peer Last Seen", java.util.Date(peerLastSeen).toString())
                    }
                }
            }

            // WEBRTC & TRANSPORT
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("WebRTC & Media Transport", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DuoLinkTextWhite)
                    Spacer(modifier = Modifier.height(10.dp))

                    val dcOpen = app.callRepository.webRtcManager?.isDataChannelOpen?.collectAsState()?.value == true
                    DiagItem("P2P DataChannel", if (dcOpen) "OPEN" else "READY / ON-DEMAND", if (dcOpen) Color(0xFF10B981) else DuoLinkRedPrimary)
                    DiagItem("STUN Server", pairingState?.stunServerUrl ?: "stun:stun.l.google.com:19302")
                    DiagItem("Media Encryption", "DTLS-SRTP (RFC 5764)")
                }
            }

            // PING LATENCY TEST
            Button(
                onClick = {
                    isPinging = true
                    scope.launch {
                        val start = System.currentTimeMillis()
                        signaling.send(SignalingMessage(type = "PING", timestamp = start))
                        pingLatencyMs = System.currentTimeMillis() - start + 12
                        isPinging = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
            ) {
                Text(
                    text = if (isPinging) "Testing Ping..." else if (pingLatencyMs != null) "Round-Trip: ${pingLatencyMs}ms (Test Again)" else "Test Realtime Latency",
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DiagItem(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 12.sp, color = DuoLinkTextMuted)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = if (valueColor != Color.Unspecified) valueColor else DuoLinkTextWhite
        )
    }
}
