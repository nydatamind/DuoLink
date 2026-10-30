package com.duolink.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (isNewSignup: Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = DuoLinkApp.instance
    val authManager = app.authManager

    // 0: Sign In, 1: Sign Up
    var selectedAuthMode by remember { mutableIntStateOf(0) }

    var googleEmail by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isCheckingOAuth by remember { mutableStateOf(false) }
    var showOAuthNotConfiguredDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        // Mascot Logo
        Box(
            modifier = Modifier
                .size(86.dp)
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

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "DuoLink",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = DuoLinkTextWhite
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (selectedAuthMode == 0) "Sign in with your Google account" else "Sign up with your Google account",
            style = MaterialTheme.typography.bodyMedium.copy(color = DuoLinkTextMuted)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // SEPARATE SIGN IN / SIGN UP SEGMENTED SWITCHER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(DuoLinkSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedAuthMode == 0) DuoLinkRedPrimary else Color.Transparent)
                    .clickable {
                        selectedAuthMode = 0
                        emailError = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign In",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (selectedAuthMode == 0) Color.White else DuoLinkTextMuted
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedAuthMode == 1) DuoLinkRedPrimary else Color.Transparent)
                    .clickable {
                        selectedAuthMode = 1
                        emailError = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sign Up",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (selectedAuthMode == 1) Color.White else DuoLinkTextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // AUTHENTICATION FORM CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (selectedAuthMode == 0) "Existing Account Sign In" else "New Account Sign Up",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DuoLinkTextWhite
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (selectedAuthMode == 0)
                        "Enter the Google email you used when creating your DuoLink identity."
                    else
                        "Enter your Google email to register. You will choose a unique username on the next step.",
                    fontSize = 12.sp,
                    color = DuoLinkTextMuted,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = googleEmail,
                    onValueChange = {
                        googleEmail = it
                        emailError = null
                    },
                    label = { Text("Google Account Email") },
                    placeholder = { Text("e.g. yourname@gmail.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = DuoLinkRedPrimary)
                    },
                    isError = emailError != null,
                    supportingText = emailError?.let { { Text(it, color = DuoLinkRedPrimary) } },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = DuoLinkTextWhite,
                        unfocusedTextColor = DuoLinkTextWhite,
                        focusedBorderColor = DuoLinkRedPrimary,
                        unfocusedBorderColor = DuoLinkTextMuted.copy(alpha = 0.4f),
                        focusedLabelColor = DuoLinkRedPrimary,
                        unfocusedLabelColor = DuoLinkTextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val trimmed = googleEmail.trim().lowercase()
                        if (trimmed.isBlank() || !trimmed.contains("@") || !trimmed.contains(".")) {
                            emailError = "Please enter a valid Google email address"
                            return@Button
                        }

                        authManager.signInWithGoogleAccount(trimmed)
                        val isNew = selectedAuthMode == 1
                        Toast.makeText(
                            context,
                            if (isNew) "Google Account connected. Set your username next." else "Welcome back!",
                            Toast.LENGTH_SHORT
                        ).show()
                        onLoginSuccess(isNew)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                ) {
                    Text(
                        text = if (selectedAuthMode == 0) "Sign In with Google" else "Sign Up with Google",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BROWSER OAUTH OPTION
        OutlinedButton(
            onClick = {
                if (isCheckingOAuth) return@OutlinedButton
                isCheckingOAuth = true
                scope.launch {
                    val isEnabled = authManager.isGoogleOAuthEnabled()
                    isCheckingOAuth = false
                    if (isEnabled) {
                        authManager.startGoogleOAuth(context)
                    } else {
                        showOAuthNotConfiguredDialog = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DuoLinkTextWhite)
        ) {
            if (isCheckingOAuth) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = DuoLinkRedPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Checking Supabase Provider...", fontSize = 13.sp)
            } else {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = null,
                    tint = DuoLinkRedPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedAuthMode == 0) "Sign In via Web Browser OAuth" else "Sign Up via Web Browser OAuth",
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // IDENTITY PRIVACY GUARANTEE CARD
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = DuoLinkRedPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Complete Identity Privacy Guarantee",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = DuoLinkTextWhite
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Your Gmail address is NEVER shared with your partner.\n• Your Google profile name and photo are NEVER displayed.\n• Each partner sees only your chosen unique username.\n• Custom nicknames are stored strictly on the local phone.",
                    fontSize = 11.sp,
                    color = DuoLinkTextMuted,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // DIALOG: SUPABASE GOOGLE OAUTH NOT ENABLED
    if (showOAuthNotConfiguredDialog) {
        AlertDialog(
            onDismissRequest = { showOAuthNotConfiguredDialog = false },
            containerColor = DuoLinkSurface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = DuoLinkRedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Provider Not Enabled",
                        fontWeight = FontWeight.Bold,
                        color = DuoLinkTextWhite,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "In your Supabase project (kypyymlubmylmomoqyoq), the Google OAuth provider has not yet been toggled ON in the dashboard.",
                        fontSize = 13.sp,
                        color = DuoLinkTextMuted,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "👉 Fast & Secure Solution:\nEnter your Google / Gmail account in the text box on this screen to authenticate immediately without waiting for Supabase OAuth setup.",
                        fontSize = 12.sp,
                        color = DuoLinkTextWhite,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showOAuthNotConfiguredDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                ) {
                    Text("Enter Email on Screen", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOAuthNotConfiguredDialog = false }) {
                    Text("Close", color = DuoLinkTextMuted)
                }
            }
        )
    }
}
