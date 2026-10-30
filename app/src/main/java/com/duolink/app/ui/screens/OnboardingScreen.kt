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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.R
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite

@Composable
fun OnboardingScreen(
    onStartPairing: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 36.dp)
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
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Welcome to DuoLink",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = DuoLinkTextWhite
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Exclusive, sovereign communication engineered for exactly two people.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DuoLinkTextMuted
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            FeatureItem(
                icon = Icons.Default.Lock,
                title = "Hardware Cryptography",
                description = "ECDH P-256 + AES-256-GCM authenticated encryption on your device."
            )

            Spacer(modifier = Modifier.height(18.dp))

            FeatureItem(
                icon = Icons.Default.Call,
                title = "Direct WebRTC Calls",
                description = "Peer-to-peer voice and video with DTLS-SRTP encryption."
            )

            Spacer(modifier = Modifier.height(18.dp))

            FeatureItem(
                icon = Icons.Default.Vibration,
                title = "Silent Vibration Engine",
                description = "Zero visible banners, zero ringtones, zero lock-screen text."
            )
        }

        Button(
            onClick = onStartPairing,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Begin Secure Pairing",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DuoLinkSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DuoLinkRedPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DuoLinkTextWhite
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DuoLinkTextMuted
                )
            )
        }
    }
}
