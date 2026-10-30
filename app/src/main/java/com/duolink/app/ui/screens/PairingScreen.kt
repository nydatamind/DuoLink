package com.duolink.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.duolink.app.DuoLinkApp
import com.duolink.app.data.signaling.SignalingClient
import com.duolink.app.ui.components.CameraScannerView
import com.duolink.app.ui.components.DuoLinkTopBar
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun PairingScreen(
    onPairingSuccess: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = DuoLinkApp.instance
    val pairingRepo = app.pairingRepository
    val crypto = app.cryptoManager

    var selectedTab by remember { mutableIntStateOf(0) }
    var partnerNickname by remember { mutableStateOf("My Person ❤️") }
    var signalingUrl by remember { mutableStateOf(SignalingClient.DEFAULT_SUPABASE_URL) }

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentToken by remember { mutableStateOf("") }
    var timeLeftSeconds by remember { mutableLongStateOf(600L) }
    var isProcessing by remember { mutableStateOf(false) }

    val myGoogleUserId = app.authManager.getInternalUserId() ?: ""

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    fun generateNewQr() {
        val payload = pairingRepo.createPairingQrPayload(myGoogleUserId, signalingUrl)
        val token = payload.substringAfter("\"token\":\"").substringBefore("\"")
        currentToken = token
        qrBitmap = pairingRepo.generateQrBitmap(payload)
        timeLeftSeconds = 600L

        app.signalingClient.connect(signalingUrl, crypto.getDeviceId())
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0 && qrBitmap == null) {
            generateNewQr()
        }
    }

    LaunchedEffect(qrBitmap) {
        while (isActive && timeLeftSeconds > 0) {
            delay(1000)
            timeLeftSeconds--
        }
    }

    LaunchedEffect(currentToken) {
        app.signalingClient.incomingMessages.collect { msg ->
            if (msg.type == "PAIR_CONFIRM" && msg.token == currentToken) {
                val result = pairingRepo.handlePairConfirmReceived(
                    msg = msg,
                    expectedToken = currentToken,
                    privateNicknameForPartner = partnerNickname,
                    signalingUrl = signalingUrl,
                    myInternalGoogleUserId = myGoogleUserId
                )
                if (result.isSuccess) {
                    Toast.makeText(context, "Pairing Complete with $partnerNickname!", Toast.LENGTH_SHORT).show()
                    onPairingSuccess()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
    ) {
        DuoLinkTopBar(
            title = "Device Pairing",
            subtitle = "Sovereign Link",
            onBackClick = onBackClick
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DuoLinkSurface,
            contentColor = DuoLinkRedPrimary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Display My QR", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) DuoLinkRedPrimary else DuoLinkTextMuted) },
                icon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = if (selectedTab == 0) DuoLinkRedPrimary else DuoLinkTextMuted) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    if (!hasCameraPermission) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                text = { Text("Scan Partner QR", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) DuoLinkRedPrimary else DuoLinkTextMuted) },
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = if (selectedTab == 1) DuoLinkRedPrimary else DuoLinkTextMuted) }
            )
        }

        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = partnerNickname,
                    onValueChange = { partnerNickname = it },
                    label = { Text("Private Nickname for Partner") },
                    supportingText = { Text("e.g. 'My Person ❤️' — 100% private to your phone", color = DuoLinkTextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = signalingUrl,
                    onValueChange = { signalingUrl = it },
                    label = { Text("Signaling Server / Supabase URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .border(2.dp, DuoLinkRedPrimary, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap!!.asImageBitmap(),
                            contentDescription = "Pairing QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        CircularProgressIndicator(color = DuoLinkRedPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val minutes = timeLeftSeconds / 60
                val seconds = timeLeftSeconds % 60
                Text(
                    text = "Code expires in %02d:%02d".format(minutes, seconds),
                    color = if (timeLeftSeconds < 60) Color.Red else DuoLinkRedPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Identity Fingerprint:",
                    fontSize = 11.sp,
                    color = DuoLinkTextMuted
                )
                Text(
                    text = crypto.getFingerprint(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = DuoLinkTextWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { generateNewQr() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DuoLinkSurface)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = DuoLinkRedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regenerate Pairing Code", color = DuoLinkTextWhite)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DuoLinkSurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = partnerNickname,
                            onValueChange = { partnerNickname = it },
                            label = { Text("Private Nickname for Partner") },
                            supportingText = { Text("e.g. 'Nitin ❤️' — saved only on this phone", color = DuoLinkTextMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    if (hasCameraPermission) {
                        CameraScannerView(
                            onQrCodeScanned = { scannedJson ->
                                if (!isProcessing) {
                                    isProcessing = true
                                    scope.launch {
                                        val result = pairingRepo.verifyAndCompletePairingFromQr(
                                            qrJson = scannedJson,
                                            privateNicknameForPartner = partnerNickname,
                                            myInternalGoogleUserId = myGoogleUserId
                                        )
                                        if (result.isSuccess) {
                                            Toast.makeText(context, "Pairing Complete with $partnerNickname!", Toast.LENGTH_SHORT).show()
                                            onPairingSuccess()
                                        } else {
                                            isProcessing = false
                                            Toast.makeText(
                                                context,
                                                "Pairing Failed: ${result.exceptionOrNull()?.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )

                        if (isProcessing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.75f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = DuoLinkRedPrimary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Establishing Cryptographic Session...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = DuoLinkRedPrimary,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Camera Permission Required",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DuoLinkTextWhite
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "DuoLink needs camera access to scan your partner's secure pairing QR code.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DuoLinkTextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                            ) {
                                Text("Grant Camera Permission", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
