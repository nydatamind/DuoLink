package com.duolink.app.data.crypto

import kotlinx.serialization.Serializable

/**
 * Data structure for the Pairing QR Code and Handshake.
 * Exactly two users: Alice (User A) and Bob (User B).
 */
@Serializable
data class PairingQrPayload(
    val version: Int = 1,
    val deviceId: String,
    val displayName: String,
    val publicKeyBase64: String,
    val signalingUrl: String,
    val token: String,
    val timestamp: Long,
    val expiresAt: Long,
    val signatureBase64: String
)

@Serializable
data class PairingConfirmationPayload(
    val version: Int = 1,
    val deviceId: String,
    val displayName: String,
    val publicKeyBase64: String,
    val token: String,
    val confirmationSignature: String
)

/**
 * Encrypted Envelope for P2P and Relay messages.
 */
@Serializable
data class EncryptedEnvelope(
    val senderDeviceId: String,
    val recipientDeviceId: String,
    val sequenceNumber: Long,
    val timestamp: Long,
    val ivBase64: String,
    val ciphertextBase64: String,
    val tagBase64: String = ""
)

