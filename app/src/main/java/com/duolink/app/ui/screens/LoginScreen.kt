package com.duolink.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.DuoLinkApp
import com.duolink.app.R
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val app = DuoLinkApp.instance
    val authManager = app.authManager

    var customGoogleEmail by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
            .statusBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 28.dp)
        ) {
            // Logo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .shadow(elevation = 12.dp, shape = CircleShape, spotColor = DuoLinkRedPrimary.copy(alpha = 0.5f))
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.duolink_logo),
                    contentDescription = "DuoLink Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DuoLink",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = DuoLinkTextWhite
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Private Authentication with Google",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = DuoLinkTextMuted
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // STRICT PRIVACY GUARANTEE BANNER
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = DuoLinkRedPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Complete Identity Privacy Guarantee",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DuoLinkTextWhite
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Your Gmail address is NEVER shared with your partner.\n• Your Google profile name is NEVER displayed to your partner.\n• Your Google profile picture is NEVER synced.\n• Google account info is strictly internal for authentication.\n• Each partner assigns their own private nickname locally.",
                        fontSize = 12.sp,
                        color = DuoLinkTextMuted,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // MAIN GOOGLE OAUTH BUTTON
            Button(
                onClick = {
                    authManager.startGoogleOAuth(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign in with Google",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ONE-CLICK TEST GOOGLE IDENTITIES (Convenient for 2 phones/emulators)
            Text(
                text = "OR SIGN IN WITH INSTANT GOOGLE IDENTITY",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = DuoLinkTextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        authManager.signInWithGoogleAccount("user.a.duolink@gmail.com")
                        Toast.makeText(context, "Signed in as User A", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DuoLinkTextWhite)
                ) {
                    Text("User A (Phone 1)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        authManager.signInWithGoogleAccount("user.b.duolink@gmail.com")
                        Toast.makeText(context, "Signed in as User B", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DuoLinkRedPrimary)
                ) {
                    Text("User B (Phone 2)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!showCustomInput) {
                TextButton(onClick = { showCustomInput = true }) {
                    Text("Custom Google Account Email", color = DuoLinkTextMuted, fontSize = 12.sp)
                }
            } else {
                OutlinedTextField(
                    value = customGoogleEmail,
                    onValueChange = { customGoogleEmail = it },
                    label = { Text("Google Account Email") },
                    placeholder = { Text("e.g. yourname@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (customGoogleEmail.isNotBlank()) {
                            authManager.signInWithGoogleAccount(customGoogleEmail.trim())
                            Toast.makeText(context, "Authenticated successfully", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                ) {
                    Text("Continue with this Account", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

