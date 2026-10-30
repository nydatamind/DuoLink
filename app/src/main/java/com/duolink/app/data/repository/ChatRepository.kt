package com.duolink.app.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.duolink.app.data.crypto.CryptoManager
import com.duolink.app.data.crypto.EncryptedEnvelope
import com.duolink.app.data.local.AppDatabase
import com.duolink.app.data.local.DeletionTombstoneEntity
import com.duolink.app.data.local.HistoryTombstoneEntity
import com.duolink.app.data.local.MessageEntity
import com.duolink.app.data.local.MessageStatus
import com.duolink.app.data.local.MessageType
import com.duolink.app.data.local.OutboxEntity
import com.duolink.app.data.local.PendingDeletionEntity
import com.duolink.app.data.signaling.SignalingClient
import com.duolink.app.data.signaling.SignalingMessage
import com.duolink.app.data.signaling.jsonSerializer
import com.duolink.app.data.webrtc.WebRtcManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

@Serializable
data class WireChatMessage(
    val messageId: String,
    val senderDeviceId: String,
    val recipientDeviceId: String,
    val text: String,
    val messageType: String,
    val mediaFileName: String? = null,
    val mediaFileSize: Long = 0L,
    val mediaMimeType: String? = null,
    val audioDurationMs: Long = 0L,
    val audioWaveform: String? = null,
    val timestamp: Long,
    val replyToMessageId: String? = null,
    val replyToPreview: String? = null,
    val isEditAction: Boolean = false,
    val isDeleteForEveryoneAction: Boolean = false,
    val isClearChatAction: Boolean = false,
    val clearedBeforeTimestamp: Long = 0L
)

class ChatRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val cryptoManager: CryptoManager,
    private val signalingClient: SignalingClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "ChatRepository"
    }

    var webRtcManager: WebRtcManager? = null

    private val _isPeerTyping = MutableStateFlow(false)
    val isPeerTyping: StateFlow<Boolean> = _isPeerTyping.asStateFlow()

    init {
        scope.launch {
            signalingClient.incomingMessages.collect { msg ->
                handleIncomingSignaling(msg)
            }
        }
    }

    val messages: Flow<List<MessageEntity>> = database.messageDao().getAllMessages().map { list ->
        list.map { entity ->
            if (entity.content.isNotEmpty()) {
                entity.copy(content = cryptoManager.decryptLocalString(entity.content))
            } else {
                entity
            }
        }
    }

    val latestMessage: Flow<MessageEntity?> = database.messageDao().getLatestMessage().map { entity ->
        entity?.let {
            if (it.content.isNotEmpty()) {
                it.copy(content = cryptoManager.decryptLocalString(it.content))
            } else {
                it
            }
        }
    }

    fun getUnreadCount(): Flow<Int> = database.messageDao().getUnreadCount(cryptoManager.getDeviceId())

    suspend fun searchMessages(query: String): List<MessageEntity> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<MessageEntity>()
            val raw = database.messageDao().searchMessages("")
            for (item in raw) {
                val decrypted = cryptoManager.decryptLocalString(item.content)
                if (decrypted.contains(query, ignoreCase = true)) {
                    list.add(item.copy(content = decrypted))
                }
            }
            list
        }
    }

    suspend fun sendTextMessage(
        text: String,
        replyToId: String? = null,
        replyToPreview: String? = null
    ): MessageEntity {
        val pairing = database.pairingDao().getPairingOnce()
            ?: throw IllegalStateException("Not paired with any device.")

        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val myDeviceId = cryptoManager.getDeviceId()

        val encryptedLocalContent = cryptoManager.encryptLocalString(text)

        val entity = MessageEntity(
            id = messageId,
            senderDeviceId = myDeviceId,
            recipientDeviceId = pairing.peerDeviceId,
            content = encryptedLocalContent,
            messageType = MessageType.TEXT,
            timestamp = now,
            status = MessageStatus.SENDING,
            replyToMessageId = replyToId,
            replyToPreview = replyToPreview
        )

        database.messageDao().insertMessage(entity)

        val wireMessage = WireChatMessage(
            messageId = messageId,
            senderDeviceId = myDeviceId,
            recipientDeviceId = pairing.peerDeviceId,
            text = text,
            messageType = MessageType.TEXT.name,
            timestamp = now,
            replyToMessageId = replyToId,
            replyToPreview = replyToPreview
        )

        dispatchWireMessage(wireMessage, pairing.sharedKeyEncryptedBase64, pairing.peerDeviceId)
        return entity.copy(content = text)
    }

    suspend fun sendVoiceMessage(
        audioPath: String,
        durationMs: Long,
        waveform: List<Int>
    ): MessageEntity {
        val pairing = database.pairingDao().getPairingOnce()
            ?: throw IllegalStateException("Not paired with any device.")

        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val myDeviceId = cryptoManager.getDeviceId()

        val waveString = waveform.joinToString(",")
        val encryptedNotice = cryptoManager.encryptLocalString("Voice Message (${durationMs / 1000}s)")

        val entity = MessageEntity(
            id = messageId,
            senderDeviceId = myDeviceId,
            recipientDeviceId = pairing.peerDeviceId,
            content = encryptedNotice,
            messageType = MessageType.AUDIO,
            mediaUri = audioPath,
            mediaFileName = "voice_$now.m4a",
            mediaMimeType = "audio/mp4",
            audioDurationMs = durationMs,
            audioWaveform = waveString,
            timestamp = now,
            status = MessageStatus.SENDING
        )

        database.messageDao().insertMessage(entity)

        val wireMessage = WireChatMessage(
            messageId = messageId,
            senderDeviceId = myDeviceId,
            recipientDeviceId = pairing.peerDeviceId,
            text = "[Voice Message]",
            messageType = MessageType.AUDIO.name,
            mediaFileName = "voice_$now.m4a",
            mediaMimeType = "audio/mp4",
            audioDurationMs = durationMs,
            audioWaveform = waveString,
            timestamp = now
        )

        dispatchWireMessage(wireMessage, pairing.sharedKeyEncryptedBase64, pairing.peerDeviceId)
        return entity
    }

    suspend fun sendMediaMessage(
        filePath: String,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        type: MessageType
    ): MessageEntity {
        val pairing = database.pairingDao().getPairingOnce()
            ?: throw IllegalStateException("Not paired with any device.")

        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val myDeviceId = cryptoManager.getDeviceId()

        val encryptedContent = cryptoManager.encryptLocalString("File: $fileName")

        val entity = MessageEntity(
            id = messageId,
            senderDeviceId = myDeviceId,
            recipientDeviceId = pairing.peerDeviceId,
            content = encryptedContent,
            messageType = type,
            mediaUri = filePath,
            mediaFileName = fileName,
            mediaFileSize = fileSize,
            mediaMimeType = mimeType,
            timestamp = now,
            status = MessageStatus.SENDING
        )

        database.messageDao().insertMessage(entity)

        val wireMessage = WireChatMessage(
            messageId = messageId,
            senderDeviceId = myDeviceId,
            recipientDeviceId = pairing.peerDeviceId,
            text = "Shared a file: $fileName",
            messageType = type.name,
            mediaFileName = fileName,
            mediaFileSize = fileSize,
            mediaMimeType = mimeType,
            timestamp = now
        )

        dispatchWireMessage(wireMessage, pairing.sharedKeyEncryptedBase64, pairing.peerDeviceId)
        return entity
    }

    suspend fun editMessage(messageId: String, newText: String) {
        val pairing = database.pairingDao().getPairingOnce() ?: return
        val encryptedContent = cryptoManager.encryptLocalString(newText)
        database.messageDao().editMessage(messageId, encryptedContent)

        val wireMessage = WireChatMessage(
            messageId = messageId,
            senderDeviceId = cryptoManager.getDeviceId(),
            recipientDeviceId = pairing.peerDeviceId,
            text = newText,
            messageType = MessageType.TEXT.name,
            timestamp = System.currentTimeMillis(),
            isEditAction = true
        )

        dispatchWireMessage(wireMessage, pairing.sharedKeyEncryptedBase64, pairing.peerDeviceId)
    }

    /**
     * Strictly DELETES FOR EVERYONE on BOTH DEVICES.
     * Removes message and attachments completely, and records a permanent tombstone.
     */
    suspend fun deleteForEveryone(messageId: String) {
        val pairing = database.pairingDao().getPairingOnce() ?: return
        val existing = database.messageDao().getMessageById(messageId)

        // 1. Delete associated local media file
        existing?.mediaUri?.let { path ->
            try { File(path).delete() } catch (e: Exception) {}
        }

        // 2. Remove permanently from local SQLite database
        database.messageDao().deletePermanently(messageId)

        // 3. Insert permanent deletion tombstone
        database.tombstoneDao().insertTombstone(
            DeletionTombstoneEntity(messageId = messageId, deletedAt = System.currentTimeMillis())
        )

        // 4. Dispatch synchronized deletion signal to peer
        val deleteSignal = SignalingMessage(
            type = "DELETE_MESSAGE",
            deviceId = cryptoManager.getDeviceId(),
            recipientDeviceId = pairing.peerDeviceId,
            messageId = messageId,
            timestamp = System.currentTimeMillis()
        )

        val sent = signalingClient.send(deleteSignal)
        if (!sent) {
            // Queue in pending deletions for offline sync
            database.pendingDeletionDao().insertPending(
                PendingDeletionEntity(
                    id = UUID.randomUUID().toString(),
                    targetMessageId = messageId,
                    actionType = "DELETE_MESSAGE",
                    timestamp = System.currentTimeMillis()
                )
            )
            Log.d(TAG, "Deletion for message $messageId queued in pending deletions.")
        }
    }

    /**
     * Clears complete chat history for BOTH USERS (Telegram-style).
     * Deletes all local messages and attachments, and records a history tombstone.
     */
    suspend fun clearChatHistoryForBoth() {
        val pairing = database.pairingDao().getPairingOnce() ?: return
        val now = System.currentTimeMillis()

        // 1. Clean up local files
        try {
            val audioDir = File(context.filesDir, "voice_messages")
            audioDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {}

        // 2. Wipe messages from Room database
        database.messageDao().clearAllMessages()

        // 3. Save History Tombstone
        val currentHistory = database.tombstoneDao().getHistoryTombstone()
        val updated = currentHistory?.copy(chatClearedBefore = now)
            ?: HistoryTombstoneEntity(id = 1, chatClearedBefore = now, callsClearedBefore = 0L)
        database.tombstoneDao().saveHistoryTombstone(updated)

        // 4. Transmit CLEAR_CHAT signal to peer
        val clearSignal = SignalingMessage(
            type = "CLEAR_CHAT",
            deviceId = cryptoManager.getDeviceId(),
            recipientDeviceId = pairing.peerDeviceId,
            clearedBeforeTimestamp = now,
            timestamp = now
        )

        val sent = signalingClient.send(clearSignal)
        if (!sent) {
            database.pendingDeletionDao().insertPending(
                PendingDeletionEntity(
                    id = UUID.randomUUID().toString(),
                    actionType = "CLEAR_CHAT",
                    timestamp = now
                )
            )
        }
    }

    /**
     * Clears Call History for BOTH USERS.
     */
    suspend fun clearCallHistoryForBoth() {
        val pairing = database.pairingDao().getPairingOnce() ?: return
        val now = System.currentTimeMillis()

        database.callLogDao().clearAllCallLogs()

        val currentHistory = database.tombstoneDao().getHistoryTombstone()
        val updated = currentHistory?.copy(callsClearedBefore = now)
            ?: HistoryTombstoneEntity(id = 1, chatClearedBefore = 0L, callsClearedBefore = now)
        database.tombstoneDao().saveHistoryTombstone(updated)

        val clearSignal = SignalingMessage(
            type = "CLEAR_CALLS",
            deviceId = cryptoManager.getDeviceId(),
            recipientDeviceId = pairing.peerDeviceId,
            clearedBeforeTimestamp = now,
            timestamp = now
        )

        val sent = signalingClient.send(clearSignal)
        if (!sent) {
            database.pendingDeletionDao().insertPending(
                PendingDeletionEntity(
                    id = UUID.randomUUID().toString(),
                    actionType = "CLEAR_CALLS",
                    timestamp = now
                )
            )
        }
    }

    /**
     * Clears ALL History (Chat + Calls + Media) for BOTH USERS.
     */
    suspend fun clearAllHistoryForBoth() {
        clearChatHistoryForBoth()
        clearCallHistoryForBoth()
    }

    suspend fun markIncomingAsRead() {
        val myDeviceId = cryptoManager.getDeviceId()
        database.messageDao().markAllIncomingAsRead(myDeviceId)
        val pairing = database.pairingDao().getPairingOnce() ?: return
        signalingClient.sendReadReceipt(pairing.peerDeviceId, "ALL_READ")
    }

    fun setTyping(isTyping: Boolean) {
        scope.launch {
            val pairing = database.pairingDao().getPairingOnce() ?: return@launch
            signalingClient.sendTyping(pairing.peerDeviceId, isTyping)
        }
    }

    private suspend fun dispatchWireMessage(
        wire: WireChatMessage,
        sharedKeyEncryptedBase64: String,
        peerDeviceId: String
    ) {
        val wireJson = jsonSerializer.encodeToString(WireChatMessage.serializer(), wire)
        val rawSharedKeyBytes = Base64.decode(
            cryptoManager.decryptLocalString(sharedKeyEncryptedBase64),
            Base64.NO_WRAP
        )
        val secretKey = SecretKeySpec(rawSharedKeyBytes, "AES")

        val envelope = cryptoManager.encryptAesGcm(secretKey, wireJson.toByteArray(StandardCharsets.UTF_8))
        val finalEnvelope = envelope.copy(
            senderDeviceId = cryptoManager.getDeviceId(),
            recipientDeviceId = peerDeviceId
        )

        // WebRTC DataChannel direct attempt
        val dataChannel = webRtcManager
        if (dataChannel != null && dataChannel.isDataChannelOpen.value) {
            val envelopeJson = jsonSerializer.encodeToString(EncryptedEnvelope.serializer(), finalEnvelope)
            val sent = dataChannel.sendDataChannelString(envelopeJson)
            if (sent) {
                database.messageDao().updateMessageStatus(wire.messageId, MessageStatus.SENT)
                return
            }
        }

        // Signaling Relay attempt
        val relayMsg = SignalingMessage(
            type = "RELAY_MESSAGE",
            deviceId = cryptoManager.getDeviceId(),
            recipientDeviceId = peerDeviceId,
            messageId = wire.messageId,
            envelope = finalEnvelope
        )

        val sentOverRelay = signalingClient.send(relayMsg)
        if (sentOverRelay) {
            database.messageDao().updateMessageStatus(wire.messageId, MessageStatus.SENT)
        } else {
            val outboxEntity = OutboxEntity(
                messageId = wire.messageId,
                payloadJson = jsonSerializer.encodeToString(WireChatMessage.serializer(), wire),
                queuedAt = System.currentTimeMillis()
            )
            database.outboxDao().enqueue(outboxEntity)
        }
    }

    suspend fun flushOutboxAndPendingDeletions() {
        val pairing = database.pairingDao().getPairingOnce() ?: return

        // 1. Flush pending deletions first
        val pendingDeletions = database.pendingDeletionDao().getAllPending()
        for (item in pendingDeletions) {
            val signal = SignalingMessage(
                type = item.actionType,
                deviceId = cryptoManager.getDeviceId(),
                recipientDeviceId = pairing.peerDeviceId,
                messageId = item.targetMessageId,
                clearedBeforeTimestamp = item.timestamp,
                timestamp = item.timestamp
            )
            if (signalingClient.send(signal)) {
                database.pendingDeletionDao().removePending(item.id)
            }
        }

        // 2. Flush pending outbox messages
        val pending = database.outboxDao().getPendingOutbox()
        for (item in pending) {
            try {
                // Ensure message wasn't deleted while in outbox
                if (database.tombstoneDao().isMessageTombstoned(item.messageId) > 0) {
                    database.outboxDao().remove(item.messageId)
                    continue
                }
                val wire = jsonSerializer.decodeFromString<WireChatMessage>(item.payloadJson)
                dispatchWireMessage(wire, pairing.sharedKeyEncryptedBase64, pairing.peerDeviceId)
                database.outboxDao().remove(item.messageId)
            } catch (e: Exception) {
                database.outboxDao().updateRetry(item.messageId, "RETRYING")
            }
        }
    }

    suspend fun handleReceivedEnvelope(envelope: EncryptedEnvelope) {
        val pairing = database.pairingDao().getPairingOnce() ?: return
        if (envelope.senderDeviceId != pairing.peerDeviceId) return

        try {
            val rawSharedKeyBytes = Base64.decode(
                cryptoManager.decryptLocalString(pairing.sharedKeyEncryptedBase64),
                Base64.NO_WRAP
            )
            val secretKey = SecretKeySpec(rawSharedKeyBytes, "AES")

            val decryptedBytes = cryptoManager.decryptAesGcm(secretKey, envelope)
            val wireJson = String(decryptedBytes, StandardCharsets.UTF_8)
            val wire = jsonSerializer.decodeFromString<WireChatMessage>(wireJson)

            // Verify if message was already tombstoned/deleted or falls before history clear
            if (database.tombstoneDao().isMessageTombstoned(wire.messageId) > 0) {
                Log.d(TAG, "Ignoring tombstoned message ${wire.messageId}")
                return
            }

            val historyTombstone = database.tombstoneDao().getHistoryTombstone()
            if (historyTombstone != null && wire.timestamp <= historyTombstone.chatClearedBefore) {
                Log.d(TAG, "Ignoring message prior to chat clear point: ${wire.messageId}")
                return
            }

            if (wire.isEditAction) {
                val encContent = cryptoManager.encryptLocalString(wire.text)
                database.messageDao().editMessage(wire.messageId, encContent)
                return
            }

            val encLocalContent = cryptoManager.encryptLocalString(wire.text)
            val entity = MessageEntity(
                id = wire.messageId,
                senderDeviceId = wire.senderDeviceId,
                recipientDeviceId = wire.recipientDeviceId,
                content = encLocalContent,
                messageType = MessageType.valueOf(wire.messageType),
                mediaFileName = wire.mediaFileName,
                mediaFileSize = wire.mediaFileSize,
                mediaMimeType = wire.mediaMimeType,
                audioDurationMs = wire.audioDurationMs,
                audioWaveform = wire.audioWaveform,
                timestamp = wire.timestamp,
                status = MessageStatus.DELIVERED,
                replyToMessageId = wire.replyToMessageId,
                replyToPreview = wire.replyToPreview
            )

            database.messageDao().insertMessage(entity)

            signalingClient.send(
                SignalingMessage(
                    type = "DELIVERY_ACK",
                    deviceId = cryptoManager.getDeviceId(),
                    recipientDeviceId = pairing.peerDeviceId,
                    messageId = wire.messageId
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt incoming envelope: ${e.message}")
        }
    }

    private fun handleIncomingSignaling(msg: SignalingMessage) {
        scope.launch {
            when (msg.type) {
                "RELAY_MESSAGE" -> {
                    msg.envelope?.let { handleReceivedEnvelope(it) }
                }
                "DELETE_MESSAGE" -> {
                    msg.messageId?.let { id ->
                        val existing = database.messageDao().getMessageById(id)
                        existing?.mediaUri?.let { path ->
                            try { File(path).delete() } catch (e: Exception) {}
                        }
                        database.messageDao().deletePermanently(id)
                        database.tombstoneDao().insertTombstone(
                            DeletionTombstoneEntity(id, msg.timestamp)
                        )
                        Log.d(TAG, "Applied synchronized deletion for message $id")
                    }
                }
                "CLEAR_CHAT" -> {
                    val cutoff = msg.clearedBeforeTimestamp ?: msg.timestamp
                    try {
                        val audioDir = File(context.filesDir, "voice_messages")
                        audioDir.listFiles()?.forEach { it.delete() }
                    } catch (e: Exception) {}
                    database.messageDao().clearMessagesBefore(cutoff)
                    val history = database.tombstoneDao().getHistoryTombstone()
                    val updated = history?.copy(chatClearedBefore = cutoff)
                        ?: HistoryTombstoneEntity(id = 1, chatClearedBefore = cutoff, callsClearedBefore = 0L)
                    database.tombstoneDao().saveHistoryTombstone(updated)
                    Log.d(TAG, "Applied synchronized CLEAR_CHAT up to $cutoff")
                }
                "CLEAR_CALLS" -> {
                    val cutoff = msg.clearedBeforeTimestamp ?: msg.timestamp
                    database.callLogDao().clearCallsBefore(cutoff)
                    val history = database.tombstoneDao().getHistoryTombstone()
                    val updated = history?.copy(callsClearedBefore = cutoff)
                        ?: HistoryTombstoneEntity(id = 1, chatClearedBefore = 0L, callsClearedBefore = cutoff)
                    database.tombstoneDao().saveHistoryTombstone(updated)
                    Log.d(TAG, "Applied synchronized CLEAR_CALLS up to $cutoff")
                }
                "DELIVERY_ACK" -> {
                    msg.messageId?.let { id ->
                        database.messageDao().updateMessageStatus(id, MessageStatus.DELIVERED)
                    }
                }
                "READ_RECEIPT" -> {
                    val myDeviceId = cryptoManager.getDeviceId()
                    database.messageDao().markAllIncomingAsRead(myDeviceId)
                }
                "TYPING" -> {
                    _isPeerTyping.value = msg.isTyping == true
                }
                "PEER_STATUS", "REGISTER" -> {
                    if (msg.isOnline == true || msg.type == "REGISTER") {
                        flushOutboxAndPendingDeletions()
                    }
                }
            }
        }
    }
}
