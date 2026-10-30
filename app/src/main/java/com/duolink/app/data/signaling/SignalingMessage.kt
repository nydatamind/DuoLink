package com.duolink.app.data.signaling

import com.duolink.app.data.crypto.EncryptedEnvelope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val jsonSerializer = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

@Serializable
data class SignalingMessage(
    val type: String,
    val deviceId: String? = null,
    val recipientDeviceId: String? = null,
    val targetDeviceId: String? = null,
    val peerDeviceId: String? = null,
    val displayName: String? = null, // Custom local nickname, never Google email or profile
    val googleUserId: String? = null, // Internal account ID only
    val publicKeyBase64: String? = null,
    val token: String? = null,
    val signature: String? = null,
    val sdp: String? = null,
    val sdpType: String? = null,
    val candidate: String? = null,
    val sdpMid: String? = null,
    val sdpMLineIndex: Int? = null,
    val callId: String? = null,
    val callType: String? = null, // "VOICE" or "VIDEO"
    val reason: String? = null,
    val isOnline: Boolean? = null,
    val lastSeen: Long? = null,
    val isTyping: Boolean? = null,
    val messageId: String? = null,
    val clearedBeforeTimestamp: Long? = null,
    val envelope: EncryptedEnvelope? = null,
    val timestamp: Long = System.currentTimeMillis()
)
