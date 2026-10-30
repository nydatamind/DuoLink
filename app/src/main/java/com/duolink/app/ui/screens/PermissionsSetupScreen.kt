package com.duolink.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
    var hasBluetooth by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var isBatteryExempt by remember {
        mutableStateOf(checkBatteryExemption(context))
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCamera = results[Manifest.permission.CAMERA] ?: hasCamera
        hasMic = results[Manifest.permission.RECORD_AUDIO] ?: hasMic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotifications = results[Manifest.permission.POST_NOTIFICATIONS] ?: hasNotifications
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasBluetooth = results[Manifest.permission.BLUETOOTH_CONNECT] ?: hasBluetooth
        }
    }

    // Refresh state on resume
    LaunchedEffect(Unit) {
        isBatteryExempt = checkBatteryExemption(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
    ) {
        DuoLinkTopBar(
            title = "Permission Setup",
            subtitle = "Security & Reliability",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OwlMascotView(
                state = OwlEyeState.OPEN,
                size = 72.dp
            )

            Text(
                text = "Required DuoLink Permissions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DuoLinkTextWhite
            )

            Text(
                text = "DuoLink is an encrypted peer-to-peer communication app. It never displays visible notification previews or audible ringtones. To ensure real-time calls and repeating silent vibrations work reliably, please grant the following permissions:",
                fontSize = 13.sp,
                color = DuoLinkTextMuted,
                lineHeight = 18.sp
            )

            // 1. SILENT VIBRATION & NOTIFICATIONS
            PermissionItemCard(
                icon = Icons.Default.Vibration,
                title = "Silent Vibration & Notifications",
                description = "Enables hardware-level repeating vibration alerts when a new message or incoming call arrives, without audible ringtones or visible lock-screen previews.",
                isGranted = hasNotifications,
                onGrantClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                    }
                }
            )

            // 2. MICROPHONE
            PermissionItemCard(
                icon = Icons.Default.Mic,
                title = "Microphone Access",
                description = "Required for crystal-clear end-to-end encrypted voice calls and interactive voice recordings.",
                isGranted = hasMic,
                onGrantClick = {
                    permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                }
            )

            // 3. CAMERA
            PermissionItemCard(
                icon = Icons.Default.CameraAlt,
                title = "Camera Access",
                description = "Used strictly for scanning your partner's secure pairing QR code and two-way encrypted HD video calling.",
                isGranted = hasCamera,
                onGrantClick = {
                    permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                }
            )

            // 4. BLUETOOTH & AUDIO ROUTING
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PermissionItemCard(
                    icon = Icons.Default.Bluetooth,
                    title = "Bluetooth & Audio Routing",
                    description = "Enables seamless switching between handset, speakerphone, and Bluetooth earbuds during active voice and video calls.",
                    isGranted = hasBluetooth,
                    onGrantClick = {
                        permissionsLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
                    }
                )
            }

            // 5. BATTERY OPTIMIZATION EXEMPTION
            PermissionItemCard(
                icon = Icons.Default.BatteryAlert,
                title = "Background Battery Exemption",
                description = "Prevents Android from putting DuoLink's peer-to-peer link to sleep, guaranteeing repeating vibration alerts even when your phone is locked or in your pocket.",
                isGranted = isBatteryExempt,
                onGrantClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CONTINUE BUTTON
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
            ) {
                Text(
                    text = if (hasCamera && hasMic) "Continue to DuoLink" else "Proceed with Current Permissions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

private fun checkBatteryExemption(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    } else true
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) Color(0xFF10B981).copy(alpha = 0.15f) else DuoLinkRedPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF10B981) else DuoLinkRedPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = DuoLinkTextWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = DuoLinkTextMuted,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (isGranted) {
                    Text(
                        text = "✓ Granted & Active",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                } else {
                    OutlinedButton(
                        onClick = onGrantClick,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant Permission", fontSize = 12.sp, color = DuoLinkRedPrimary)
                    }
                }
            }
        }
    }
}

