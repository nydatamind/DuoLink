package com.duolink.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.duolink.app.DuoLinkApp
import com.duolink.app.data.auth.PairingRequest
import com.duolink.app.data.auth.PartnerSearchResult
import com.duolink.app.data.signaling.SignalingClient
import com.duolink.app.ui.components.CameraScannerView
import com.duolink.app.ui.components.DuoLinkTopBar
import com.duolink.app.ui.mascot.OwlEyeState
import com.duolink.app.ui.mascot.OwlMascotView
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
    val clipboardManager = LocalClipboardManager.current
    val app = DuoLinkApp.instance
    val profileManager = app.profileManager
    val pairingRepo = app.pairingRepository
    val authManager = app.authManager

    val myUsername = authManager.getUsername() ?: "user"
    val incomingRequests by profileManager.incomingRequests.collectAsState()
    val outgoingRequest by profileManager.outgoingRequest.collectAsState()
    val discoveredPartners by profileManager.discoveredPartners.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<PartnerSearchResult>>(emptyList()) }
    var partnerCustomNickname by remember { mutableStateOf("") }

    // Alternative offline QR tab
    var showQrMode by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isScanningQr by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) isScanningQr = true
    }

    // Auto-navigate on mutual pairing confirmation & active discovery loop
    LaunchedEffect(Unit) {
        val devId = app.cryptoManager.getDeviceId()
        app.signalingClient.connect(SignalingClient.DEFAULT_SUPABASE_URL, devId)

        launch {
            profileManager.pairingSuccessEvent.collect {
                Toast.makeText(context, "Pairing successful! Connected permanently.", Toast.LENGTH_SHORT).show()
                onPairingSuccess()
            }
        }

        // Active discovery broadcast loop while on PairingScreen
        while (isActive) {
            profileManager.broadcastPresence()
            profileManager.broadcastQuery("*")
            delay(2000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
    ) {
        DuoLinkTopBar(
            title = "Connect Partner",
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // MY USERNAME BADGE
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OwlMascotView(state = OwlEyeState.OPEN, size = 42.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Username",
                            fontSize = 11.sp,
                            color = DuoLinkTextMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "@$myUsername",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DuoLinkTextWhite
                        )
                    }
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(myUsername))
                        Toast.makeText(context, "Copied @$myUsername", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Username",
                            tint = DuoLinkRedPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // INCOMING PAIRING REQUESTS (IF ANY)
            if (incomingRequests.isNotEmpty()) {
                Text(
                    text = "Incoming Requests",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DuoLinkRedPrimary
                )

                incomingRequests.forEach { req ->
                    IncomingRequestCard(
                        request = req,
                        partnerNickname = partnerCustomNickname,
                        onNicknameChange = { partnerCustomNickname = it },
                        onAccept = {
                            scope.launch {
                                val res = profileManager.acceptPairingRequest(req, partnerCustomNickname)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Connected with @${req.senderUsername}", Toast.LENGTH_SHORT).show()
                                    onPairingSuccess()
                                } else {
                                    Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        onReject = {
                            scope.launch {
                                profileManager.rejectPairingRequest(req)
                                Toast.makeText(context, "Declined", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // OUTGOING PENDING REQUEST (IF ANY)
            outgoingRequest?.let { outgoing ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = DuoLinkRedPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Request Sent",
                                fontWeight = FontWeight.Bold,
                                color = DuoLinkTextWhite,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Waiting for @${outgoing.receiverUsername} to accept.",
                            fontSize = 13.sp,
                            color = DuoLinkTextMuted,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    profileManager.cancelOutgoingRequest()
                                    Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = DuoLinkRedPrimary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // USERNAME SEARCH & ADD PARTNER CARD
            if (!showQrMode) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Find Partner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = DuoLinkTextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Search for your partner by their username.",
                            fontSize = 12.sp,
                            color = DuoLinkTextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                if (it.trim().length >= 2) {
                                    isSearching = true
                                    scope.launch {
                                        val res = profileManager.searchPartner(it.trim())
                                        isSearching = false
                                        searchResults = res.getOrNull() ?: emptyList()
                                    }
                                } else {
                                    searchResults = emptyList()
                                }
                            },
                            placeholder = { Text("Enter partner username", color = DuoLinkTextMuted) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = DuoLinkRedPrimary)
                            },
                            trailingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = DuoLinkRedPrimary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            },
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // COMBINED REAL-TIME DISCOVERY & SEARCH RESULTS
                        val combinedUsers = remember(searchQuery, searchResults, discoveredPartners) {
                            val q = searchQuery.trim().lowercase()
                            val all = (searchResults + discoveredPartners).distinctBy { it.username }
                            if (q.isBlank()) {
                                discoveredPartners
                            } else {
                                all.filter { it.username.contains(q) }
                            }
                        }

                        if (combinedUsers.isNotEmpty()) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "Search Results" else "Active Partners Online",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DuoLinkRedPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            combinedUsers.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DuoLinkCharcoalBg)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OwlMascotView(state = OwlEyeState.OPEN, size = 36.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "@${user.username}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = DuoLinkTextWhite
                                        )
                                        Text(
                                            text = "Ready to connect",
                                            fontSize = 11.sp,
                                            color = DuoLinkTextMuted
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val res = profileManager.sendPairingRequest(user, user.username)
                                                if (res.isSuccess) {
                                                    Toast.makeText(context, "Request sent to @${user.username}", Toast.LENGTH_SHORT).show()
                                                    searchQuery = ""
                                                    searchResults = emptyList()
                                                } else {
                                                    Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                                    ) {
                                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else if (searchQuery.trim().length >= 2 && !isSearching) {
                            Text(
                                text = "No partner found for '@$searchQuery'.",
                                fontSize = 12.sp,
                                color = DuoLinkTextMuted
                            )
                        } else if (!isSearching) {
                            Text(
                                text = "Open DuoLink on your partner's phone to connect automatically.",
                                fontSize = 11.sp,
                                color = DuoLinkTextMuted
                            )
                        }
                    }
                }
            }

            // OPTIONAL OFFLINE QR CODE SECTION (ALTERNATIVE)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQrMode = !showQrMode },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = DuoLinkRedPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Pair via QR Code",
                                fontWeight = FontWeight.Bold,
                                color = DuoLinkTextWhite,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = if (showQrMode) "Hide" else "Show",
                            color = DuoLinkRedPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    if (showQrMode) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val payload = pairingRepo.createPairingQrPayload(myUsername, SignalingClient.DEFAULT_SUPABASE_URL)
                                    qrBitmap = pairingRepo.generateQrBitmap(payload)
                                    isScanningQr = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Show QR", fontSize = 12.sp, color = DuoLinkTextWhite)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (hasCameraPermission) {
                                        isScanningQr = true
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Scan QR", fontSize = 12.sp, color = DuoLinkRedPrimary)
                            }
                        }

                        if (qrBitmap != null && !isScanningQr) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = "Pairing QR",
                                    modifier = Modifier.size(200.dp)
                                )
                            }
                        }

                        if (isScanningQr) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                CameraScannerView(
                                    onQrCodeScanned = { qrText ->
                                        isScanningQr = false
                                        scope.launch {
                                            val res = pairingRepo.verifyAndCompletePairingFromQr(
                                                qrJson = qrText,
                                                privateNicknameForPartner = "Partner",
                                                myInternalGoogleUserId = myUsername
                                            )
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Pairing verified", Toast.LENGTH_SHORT).show()
                                                onPairingSuccess()
                                            } else {
                                                Toast.makeText(context, "QR Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // PRIVACY NOTE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DuoLinkSurface)
                    .padding(14.dp)
            ) {
                Text(
                    text = "DuoLink is private and exclusive to two users.",
                    fontSize = 12.sp,
                    color = DuoLinkTextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun IncomingRequestCard(
    request: PairingRequest,
    partnerNickname: String,
    onNicknameChange: (String) -> Unit,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DuoLinkSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OwlMascotView(state = OwlEyeState.OPEN, size = 42.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "@${request.senderUsername}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DuoLinkTextWhite
                    )
                    Text(
                        text = "wants to connect",
                        fontSize = 12.sp,
                        color = DuoLinkRedPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = partnerNickname,
                onValueChange = onNicknameChange,
                label = { Text("Partner Nickname") },
                placeholder = { Text("e.g. Partner") },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Decline", color = DuoLinkTextMuted, fontSize = 13.sp)
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                ) {
                    Text("Accept", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
