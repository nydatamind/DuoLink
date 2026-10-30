package com.duolink.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.R
import com.duolink.app.ui.components.DuoLinkTopBar
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite

@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
    ) {
        DuoLinkTopBar(
            title = "About DuoLink",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .shadow(elevation = 10.dp, shape = CircleShape, spotColor = DuoLinkRedPrimary.copy(alpha = 0.5f))
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.duolink_logo),
                    contentDescription = "DuoLink Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Text(
                text = "DuoLink",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = DuoLinkTextWhite)
            )

            Text(
                text = "Version 1.0.0 (Production Release)",
                fontSize = 13.sp,
                color = DuoLinkTextMuted
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Two-Person Sovereign Link", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DuoLinkTextWhite)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DuoLink is an exclusive private communication app built strictly for TWO authorized individuals. It operates without public user directories, third-party social databases, or readable server transcripts.",
                        fontSize = 14.sp,
                        color = DuoLinkTextMuted,
                        lineHeight = 20.sp
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cryptographic Architecture", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DuoLinkTextWhite)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Identity & Signatures: NIST P-256 (SHA-256 with ECDSA)\n• Key Agreement: Diffie-Hellman (ECDH P-256)\n• Key Derivation: RFC 5869 HKDF-SHA256\n• Message Encryption: Authenticated AES-256-GCM\n• Media Stream: WebRTC DTLS-SRTP for Voice & Video\n• Local Storage: Android Keystore protected Room SQLite\n• Alert Engine: Silent hardware vibration pattern engine",
                        fontSize = 13.sp,
                        color = DuoLinkTextMuted,
                        lineHeight = 22.sp
                    )
                }
            }

            Text(
                text = "Engineered for pure privacy, reliable communication, and zero data leakage.",
                fontSize = 12.sp,
                color = DuoLinkTextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
