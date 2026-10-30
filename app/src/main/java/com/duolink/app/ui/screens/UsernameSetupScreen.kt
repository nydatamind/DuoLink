package com.duolink.app.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun UsernameSetupScreen(
    onUsernameCreated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = DuoLinkApp.instance
    val profileManager = app.profileManager

    var usernameInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isAvailable by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Mascot Logo
        Box(
            modifier = Modifier
                .size(88.dp)
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
            text = "Create Your Username",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = DuoLinkTextWhite
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose a unique username for your private connection.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = DuoLinkTextMuted,
                lineHeight = 19.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Username",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DuoLinkTextWhite
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = {
                        val filtered = it.filter { char -> char.isLetterOrDigit() || char == '_' }.lowercase()
                        if (filtered.length <= 20) {
                            usernameInput = filtered
                            errorMessage = null
                            isAvailable = false
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = DuoLinkRedPrimary)
                    },
                    trailingIcon = {
                        if (isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = DuoLinkRedPrimary,
                                strokeWidth = 2.dp
                            )
                        } else if (isAvailable) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
                        } else if (errorMessage != null) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = DuoLinkRedPrimary)
                        }
                    },
                    placeholder = { Text("Enter username", color = DuoLinkTextMuted) },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = DuoLinkRedPrimary, fontSize = 12.sp)
                        } else if (isAvailable) {
                            Text("Username available", color = Color(0xFF4CAF50), fontSize = 12.sp)
                        } else {
                            Text("Letters, numbers, and underscores (3–20 characters).", color = DuoLinkTextMuted, fontSize = 11.sp)
                        }
                    },
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val clean = usernameInput.trim().lowercase()
                        if (clean.length < 3) {
                            errorMessage = "Username must be at least 3 characters"
                            return@Button
                        }
                        if (!clean.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))) {
                            errorMessage = "Only letters, numbers, and underscores allowed"
                            return@Button
                        }

                        isChecking = true
                        errorMessage = null
                        scope.launch {
                            val checkRes = profileManager.checkUsernameAvailability(clean)
                            isChecking = false
                            val available = checkRes.getOrNull() ?: true
                            if (!available) {
                                errorMessage = "Username '@$clean' is taken"
                                isAvailable = false
                            } else {
                                isAvailable = true
                                isSaving = true
                                val saveRes = profileManager.registerUsername(clean)
                                isSaving = false
                                if (saveRes.isSuccess) {
                                    Toast.makeText(context, "Username set", Toast.LENGTH_SHORT).show()
                                    onUsernameCreated()
                                } else {
                                    errorMessage = "Failed to save: ${saveRes.exceptionOrNull()?.message}"
                                }
                            }
                        }
                    },
                    enabled = !isChecking && !isSaving && usernameInput.length >= 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving...", color = Color.White)
                    } else {
                        Text(
                            text = "Continue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PRIVACY NOTE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DuoLinkSurface)
                .padding(14.dp)
        ) {
            Text(
                text = "Your email address and Google account details are never shared.",
                fontSize = 12.sp,
                color = DuoLinkTextMuted,
                lineHeight = 17.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

