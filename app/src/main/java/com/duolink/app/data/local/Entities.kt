package com.duolink.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "pairing_config")
@Serializable
data class PairingEntity(
    @PrimaryKey val id: Int = 1,
    val peerDeviceId: String,
    val peerDisplayName: String, // Custom private nickname set locally (e.g. "My Person ❤️")
    val peerPublicKeyBase64: String,
    val sharedKeyEncryptedBase64: String,
    val pairedAt: Long,
    val isConfirmed: Boolean,
    val signalingServerUrl: String,
    val stunServerUrl: String = "stun:stun.l.google.com:19302",
    val turnServerUrl: String = "",
    val turnUsername: String = "",
    val turnCredential: String = "",
    val myGoogleUserId: String = "",
    val peerGoogleUserId: String = ""
)

enum class MessageType {
    TEXT,
    AUDIO,
    IMAGE,
    VIDEO,
    FILE
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

@Entity(tableName = "messages")
@Serializable
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderDeviceId: String,
    val recipientDeviceId: String,
    val content: String, // Encrypted text
    val messageType: MessageType = MessageType.TEXT,
    val mediaUri: String? = null,
    val mediaFileName: String? = null,
    val mediaFileSize: Long = 0L,
    val mediaMimeType: String? = null,
    val audioDurationMs: Long = 0L,
    val audioWaveform: String? = null,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENDING,
    val replyToMessageId: String? = null,
    val replyToPreview: String? = null,
    val isEdited: Boolean = false
)

enum class CallType {
    VOICE,
    VIDEO
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

enum class CallStatus {
    COMPLETED,
    MISSED,
    REJECTED,
    FAILED
}

@Entity(tableName = "call_logs")
@Serializable
data class CallLogEntity(
    @PrimaryKey val id: String,
    val peerDeviceId: String,
    val peerDisplayName: String,
    val callType: CallType,
    val direction: CallDirection,
    val status: CallStatus,
    val startedAt: Long,
    val durationSeconds: Long
)

@Entity(tableName = "deletion_tombstones")
@Serializable
data class DeletionTombstoneEntity(
    @PrimaryKey val messageId: String,
    val deletedAt: Long
)

@Entity(tableName = "history_tombstones")
@Serializable
data class HistoryTombstoneEntity(
    @PrimaryKey val id: Int = 1,
    val chatClearedBefore: Long = 0L,
    val callsClearedBefore: Long = 0L
)

@Entity(tableName = "pending_deletions")
@Serializable
data class PendingDeletionEntity(
    @PrimaryKey val id: String,
    val targetMessageId: String? = null,
    val actionType: String, // "DELETE_MESSAGE", "CLEAR_CHAT", "CLEAR_CALLS", "CLEAR_ALL"
    val timestamp: Long,
    val retryCount: Int = 0
)

@Entity(tableName = "outbox_queue")
@Serializable
data class OutboxEntity(
    @PrimaryKey val messageId: String,
    val payloadJson: String,
    val queuedAt: Long,
    val retryCount: Int = 0,
    val status: String = "PENDING"
)
