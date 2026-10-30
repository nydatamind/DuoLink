package com.duolink.app.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.duolink.app.DuoLinkApp
import com.duolink.app.data.local.CallType
import com.duolink.app.data.local.MessageEntity
import com.duolink.app.data.local.MessageType
import com.duolink.app.ui.components.AudioMessagePlayer
import com.duolink.app.ui.components.AvatarView
import com.duolink.app.ui.emoji.EmojiPickerView
import com.duolink.app.ui.mascot.OwlEyeState
import com.duolink.app.ui.mascot.OwlMascotView
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkDateSeparator
import com.duolink.app.ui.theme.DuoLinkIncomingBubble
import com.duolink.app.ui.theme.DuoLinkOutgoingBubble
import com.duolink.app.ui.theme.DuoLinkPillInput
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextSubtle
import com.duolink.app.ui.theme.DuoLinkTextWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    onBackClick: () -> Unit,
    onNavigateToCall: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = DuoLinkApp.instance
    val chatRepo = app.chatRepository
    val voiceRecorder = app.voiceRecorderManager
    val voicePlayer = app.voicePlayerManager
    val myDeviceId = app.cryptoManager.getDeviceId()

    val pairingState by app.pairingRepository.pairingState.collectAsState(initial = null)
    // Custom private nickname set locally (e.g. "My Person ❤️")
    val partnerNickname = pairingState?.peerDisplayName ?: "Partner"
    val isOnline by app.signalingClient.isPeerOnline.collectAsState()
    val isPeerTyping by chatRepo.isPeerTyping.collectAsState()
    val owlEyeState by app.owlEyeStateManager.owlEyeState.collectAsState()

    val allMessages by chatRepo.messages.collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    var textInput by remember { mutableStateOf("") }
    var isEmojiPickerOpen by remember { mutableStateOf(false) }
    var replyingToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageForAction by remember { mutableStateOf<MessageEntity?>(null) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val isRecording by voiceRecorder.isRecording.collectAsState()
    val recordingDurationMs by voiceRecorder.recordingDurationMs.collectAsState()
    val isPlayingAudio by voicePlayer.isPlaying.collectAsState()
    val currentAudioUri by voicePlayer.currentUri.collectAsState()
    val currentAudioPos by voicePlayer.currentPositionMs.collectAsState()
    val currentAudioDuration by voicePlayer.durationMs.collectAsState()
    val currentAudioSpeed by voicePlayer.playbackSpeed.collectAsState()

    LaunchedEffect(Unit) {
        app.vibrationAlertManager.stopMessageVibration()
        chatRepo.markIncomingAsRead()
    }

    DisposableEffect(Unit) {
        onDispose {
            app.vibrationAlertManager.stopMessageVibration()
            voicePlayer.stop()
        }
    }

    LaunchedEffect(allMessages.size) {
        if (allMessages.isNotEmpty()) {
            listState.animateScrollToItem(allMessages.size - 1)
        }
    }

    LaunchedEffect(textInput) {
        if (textInput.isNotEmpty()) {
            chatRepo.setTyping(true)
            delay(2500)
            chatRepo.setTyping(false)
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceRecorder.startRecording()
        } else {
            Toast.makeText(context, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val fileName = "attachment_${System.currentTimeMillis()}"
                chatRepo.sendMediaMessage(
                    filePath = it.toString(),
                    fileName = fileName,
                    fileSize = 0L,
                    mimeType = context.contentResolver.getType(it) ?: "application/octet-stream",
                    type = MessageType.FILE
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DuoLinkCharcoalBg)
            .statusBarsPadding()
            .imePadding()
    ) {
        // TOP APP BAR (FAITHFUL TO SCREENSHOT)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DuoLinkRedPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                OwlMascotView(
                    state = owlEyeState,
                    size = 42.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = partnerNickname,
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DuoLinkTextWhite
                        )
                    )
                    Text(
                        text = if (isPeerTyping) "Typing..." else if (isOnline) "Active (Alert)" else when (owlEyeState) {
                            OwlEyeState.OPEN -> "Recently Active"
                            OwlEyeState.HALF -> "Sleepy"
                            OwlEyeState.CLOSED -> "Resting"
                        },
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = if (isPeerTyping || isOnline) Color(0xFF10B981) else DuoLinkTextMuted
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Call Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = CircleShape,
                            spotColor = Color.Black.copy(alpha = 0.5f)
                        )
                        .clip(CircleShape)
                        .background(DuoLinkSurface)
                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                        .combinedClickable(
                            onClick = {
                                scope.launch {
                                    app.callRepository.startOutgoingCall(CallType.VOICE)
                                    onNavigateToCall()
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Voice Call",
                        tint = DuoLinkRedPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Menu (Clear History for Both)
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = DuoLinkTextMuted)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DuoLinkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear History for Both", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = DuoLinkRedPrimary) },
                            onClick = {
                                showMenu = false
                                showClearHistoryConfirm = true
                            }
                        )
                    }
                }
            }
        }

        // MESSAGE TIMELINE
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Today",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = DuoLinkDateSeparator
                        )
                    )
                }
            }

            items(allMessages, key = { it.id }) { msg ->
                val isMe = msg.senderDeviceId == myDeviceId

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(14.dp),
                                spotColor = if (isMe) DuoLinkRedPrimary.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.4f)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isMe) DuoLinkOutgoingBubble else DuoLinkIncomingBubble)
                            .border(
                                width = 1.dp,
                                color = if (isMe) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { selectedMessageForAction = msg }
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            if (msg.replyToPreview != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = msg.replyToPreview,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            when (msg.messageType) {
                                MessageType.AUDIO -> {
                                    val uri = msg.mediaUri ?: ""
                                    val isThisPlaying = isPlayingAudio && currentAudioUri == uri
                                    AudioMessagePlayer(
                                        audioUri = uri,
                                        durationMs = msg.audioDurationMs,
                                        waveformString = msg.audioWaveform,
                                        isPlaying = isThisPlaying,
                                        currentPositionMs = if (isThisPlaying) currentAudioPos else 0L,
                                        playbackSpeed = currentAudioSpeed,
                                        onPlayPauseClick = {
                                            if (isThisPlaying) voicePlayer.pause() else voicePlayer.play(uri)
                                        },
                                        onSpeedToggleClick = { voicePlayer.togglePlaybackSpeed() }
                                    )
                                }
                                MessageType.FILE, MessageType.IMAGE, MessageType.VIDEO -> {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.AttachFile,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = msg.mediaFileName ?: "Attachment",
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                else -> {
                                    Row(
                                        verticalAlignment = Alignment.Bottom,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = msg.content,
                                            style = TextStyle(
                                                fontSize = 15.sp,
                                                color = if (isMe) DuoLinkTextWhite else Color(0xFFE2E8F0),
                                                lineHeight = 20.sp
                                            ),
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Text(
                                            text = formatMessageTime(msg.timestamp),
                                            style = TextStyle(
                                                fontSize = 11.sp,
                                                color = if (isMe) Color.White.copy(alpha = 0.75f) else DuoLinkTextMuted
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // REPLY BANNER
        if (replyingToMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DuoLinkSurface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Replying to:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DuoLinkRedPrimary)
                    Text(replyingToMessage!!.content, fontSize = 13.sp, maxLines = 1, color = DuoLinkTextMuted)
                }
                IconButton(onClick = { replyingToMessage = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = DuoLinkTextMuted)
                }
            }
        }

        // EDIT BANNER
        if (editingMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DuoLinkRedPrimary.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Editing message", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DuoLinkRedPrimary, modifier = Modifier.weight(1f))
                IconButton(onClick = { editingMessage = null; textInput = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = DuoLinkRedPrimary)
                }
            }
        }

        // BOTTOM INPUT BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isRecording) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(DuoLinkPillInput)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(DuoLinkRedPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val seconds = (recordingDurationMs / 1000)
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60),
                            fontWeight = FontWeight.Bold,
                            color = DuoLinkTextWhite
                        )
                    }

                    TextButton(onClick = { voiceRecorder.cancelRecording() }) {
                        Text("Cancel", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(elevation = 8.dp, shape = CircleShape, spotColor = DuoLinkRedPrimary.copy(alpha = 0.6f))
                        .clip(CircleShape)
                        .background(DuoLinkRedPrimary)
                        .combinedClickable(
                            onClick = {
                                val result = voiceRecorder.stopRecording()
                                if (result != null) {
                                    scope.launch {
                                        chatRepo.sendVoiceMessage(
                                            audioPath = result.file.absolutePath,
                                            durationMs = result.durationMs,
                                            waveform = result.waveform
                                        )
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Voice",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                        .clip(RoundedCornerShape(26.dp))
                        .background(DuoLinkPillInput)
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(26.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isEmojiPickerOpen = !isEmojiPickerOpen },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfied,
                            contentDescription = "Emoji Picker",
                            tint = if (isEmojiPickerOpen) Color.White else DuoLinkRedPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (textInput.isEmpty()) {
                            Text(
                                text = "Type a message",
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = DuoLinkTextSubtle
                                )
                            )
                        }
                        BasicTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = DuoLinkTextWhite
                            ),
                            cursorBrush = SolidColor(DuoLinkRedPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach",
                            tint = DuoLinkRedPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { filePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = DuoLinkRedPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = DuoLinkRedPrimary.copy(alpha = 0.6f)
                        )
                        .clip(CircleShape)
                        .background(DuoLinkRedPrimary)
                        .combinedClickable(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    val textToSend = textInput.trim()
                                    textInput = ""
                                    scope.launch {
                                        val editTarget = editingMessage
                                        if (editTarget != null) {
                                            chatRepo.editMessage(editTarget.id, textToSend)
                                            editingMessage = null
                                        } else {
                                            chatRepo.sendTextMessage(
                                                text = textToSend,
                                                replyToId = replyingToMessage?.id,
                                                replyToPreview = replyingToMessage?.content
                                            )
                                            replyingToMessage = null
                                        }
                                    }
                                } else {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        voiceRecorder.startRecording()
                                    } else {
                                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (textInput.isNotBlank()) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                        contentDescription = "Mic/Send",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (isEmojiPickerOpen) {
            EmojiPickerView(
                onEmojiSelected = { emoji ->
                    textInput += emoji
                },
                onBackspace = {
                    if (textInput.isNotEmpty()) {
                        textInput = textInput.dropLast(1)
                    }
                }
            )
        }
    }

    // STRICT DELETE FOR EVERYONE DIALOG (NO "Delete for me" option!)
    if (selectedMessageForAction != null) {
        val msg = selectedMessageForAction!!
        val isMe = msg.senderDeviceId == myDeviceId

        AlertDialog(
            onDismissRequest = { selectedMessageForAction = null },
            title = { Text("Message Options", fontWeight = FontWeight.Bold, color = DuoLinkTextWhite) },
            containerColor = DuoLinkSurface,
            text = {
                Column {
                    TextButton(
                        onClick = {
                            replyingToMessage = msg
                            selectedMessageForAction = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reply", modifier = Modifier.fillMaxWidth(), color = DuoLinkTextWhite)
                    }

                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("DuoLink Message", msg.content))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            selectedMessageForAction = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy", modifier = Modifier.fillMaxWidth(), color = DuoLinkTextWhite)
                    }

                    if (isMe && msg.messageType == MessageType.TEXT) {
                        TextButton(
                            onClick = {
                                editingMessage = msg
                                textInput = msg.content
                                selectedMessageForAction = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Edit", modifier = Modifier.fillMaxWidth(), color = DuoLinkRedPrimary)
                        }
                    }

                    // Strict "Delete for Everyone" (Deletes from BOTH devices)
                    TextButton(
                        onClick = {
                            scope.launch {
                                chatRepo.deleteForEveryone(msg.id)
                                Toast.makeText(context, "Message deleted for both users", Toast.LENGTH_SHORT).show()
                            }
                            selectedMessageForAction = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete for Everyone (Both Devices)", modifier = Modifier.fillMaxWidth(), color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForAction = null }) {
                    Text("Close", color = DuoLinkTextMuted)
                }
            }
        )
    }

    // CLEAR CHAT HISTORY FOR BOTH CONFIRMATION DIALOG
    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear History for Both?", fontWeight = FontWeight.Bold, color = DuoLinkTextWhite) },
            containerColor = DuoLinkSurface,
            text = {
                Text(
                    "Are you sure you want to clear chat history for BOTH $partnerNickname and yourself? All messages, voice notes, and media will be permanently deleted from both devices.",
                    color = DuoLinkTextMuted,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryConfirm = false
                        scope.launch {
                            chatRepo.clearChatHistoryForBoth()
                            Toast.makeText(context, "Chat history cleared for both users", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear for Both", color = DuoLinkRedPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Cancel", color = DuoLinkTextMuted)
                }
            }
        )
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp)).lowercase()
}
