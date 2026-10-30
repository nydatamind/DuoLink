package com.duolink.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.content.ContextCompat
import com.duolink.app.ui.components.DuoLinkTopBar
import com.duolink.app.ui.mascot.OwlEyeState
import com.duolink.app.ui.mascot.OwlMascotView
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite

@Composable
fun PermissionsSetupScreen(
    onContinue: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var hasMic by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var hasNotifications by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    // Android runtime permission launchers - directly request system dialogs
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCamera = isGranted
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMic = isGranted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifications = isGranted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
    ) {
        DuoLinkTopBar(
            title = "Permissions",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Small DuoLink owl mascot
                OwlMascotView(
                    state = OwlEyeState.OPEN,
                    size = 54.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = DuoLinkTextWhite
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Tap any item to grant access",
                    fontSize = 13.sp,
                    color = DuoLinkTextMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // MICROPHONE CARD
                SimplePermissionRow(
                    icon = Icons.Default.Mic,
                    title = "Microphone",
                    subtitle = "Voice notes and calls",
                    isAllowed = hasMic,
                    onClick = {
                        if (!hasMic) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // CAMERA CARD
                SimplePermissionRow(
                    icon = Icons.Default.CameraAlt,
                    title = "Camera",
                    subtitle = "Video calls and QR code",
                    isAllowed = hasCamera,
                    onClick = {
                        if (!hasCamera) cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // NOTIFICATIONS CARD
                SimplePermissionRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Call and message alerts",
                    isAllowed = hasNotifications,
                    onClick = {
                        if (!hasNotifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // VIBRATION CARD (Hardware enabled)
                SimplePermissionRow(
                    icon = Icons.Default.Vibration,
                    title = "Vibration",
                    subtitle = "Silent alerts",
                    isAllowed = true,
                    isFixed = true,
                    onClick = {}
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp)
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                ) {
                    Text(
                        text = "Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Optional Settings link if user needs to change OS settings
                TextButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = DuoLinkTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "App Settings",
                        color = DuoLinkTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SimplePermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isAllowed: Boolean,
    isFixed: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isAllowed && !isFixed) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DuoLinkCharcoalBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isAllowed) DuoLinkRedPrimary else DuoLinkTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DuoLinkTextWhite
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = DuoLinkTextMuted
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isAllowed) Color(0xFF4CAF50).copy(alpha = 0.15f)
                        else DuoLinkRedPrimary.copy(alpha = 0.15f)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isAllowed) "Allowed" else "Enable",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAllowed) Color(0xFF4CAF50) else DuoLinkRedPrimary
                )
            }
        }
    }
}
