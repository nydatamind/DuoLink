package com.duolink.app.data.repository

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.duolink.app.data.crypto.CryptoManager
import com.duolink.app.data.crypto.PairingQrPayload
import com.duolink.app.data.local.AppDatabase
import com.duolink.app.data.local.PairingEntity
import com.duolink.app.data.signaling.SignalingClient
import com.duolink.app.data.signaling.SignalingMessage
import com.duolink.app.data.signaling.jsonSerializer
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.Flow
import java.nio.charset.StandardCharsets

class PairingRepository(
    private val cryptoManager: CryptoManager,
    private val database: AppDatabase,
    private val signalingClient: SignalingClient
) {
    val pairingState: Flow<PairingEntity?> = database.pairingDao().getPairing()

    suspend fun getPairingOnce(): PairingEntity? = database.pairingDao().getPairingOnce()

    suspend fun isPaired(): Boolean = getPairingOnce() != null

    /**
     * Generates a secure, signed, one-time pairing QR code payload.
     * ZERO Google email or personal account details are exposed.
     */
    fun createPairingQrPayload(myInternalGoogleUserId: String, signalingUrl: String): String {
        val deviceId = cryptoManager.getDeviceId()
        val publicKeyBase64 = cryptoManager.getLocalPublicKeyBase64()
        val token = cryptoManager.generatePairingToken()
        val now = System.currentTimeMillis()
        val expiresAt = now + (10 * 60 * 1000L) // 10 minutes

        val dataToSign = "$deviceId|$publicKeyBase64|$token|$now|$expiresAt"
        val signatureBase64 = cryptoManager.sign(dataToSign.toByteArray(StandardCharsets.UTF_8))

        val payload = PairingQrPayload(
            deviceId = deviceId,
            displayName = "", // Left blank intentionally for complete privacy
            publicKeyBase64 = publicKeyBase64,
            signalingUrl = signalingUrl,
            token = token,
            timestamp = now,
            expiresAt = expiresAt,
            signatureBase64 = signatureBase64
        )

        return jsonSerializer.encodeToString(PairingQrPayload.serializer(), payload)
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    /**
     * Scans and verifies User A's QR code.
     * Sets private custom nickname locally (never leaks Google profile info).
     */
    suspend fun verifyAndCompletePairingFromQr(
        qrJson: String,
        privateNicknameForPartner: String,
        myInternalGoogleUserId: String
    ): Result<PairingEntity> {
        return try {
            val qrPayload = jsonSerializer.decodeFromString<PairingQrPayload>(qrJson)

            val now = System.currentTimeMillis()
            if (now > qrPayload.expiresAt) {
                return Result.failure(IllegalStateException("Pairing code has expired. Please regenerate a new QR code."))
            }

            if (qrPayload.deviceId == cryptoManager.getDeviceId()) {
                return Result.failure(IllegalArgumentException("Cannot pair with your own device identity."))
            }

            val peerPubKey = cryptoManager.decodePublicKey(qrPayload.publicKeyBase64)
            val dataToVerify = "${qrPayload.deviceId}|${qrPayload.publicKeyBase64}|${qrPayload.token}|${qrPayload.timestamp}|${qrPayload.expiresAt}"
            val isValidSignature = cryptoManager.verifySignature(
                peerPubKey,
                dataToVerify.toByteArray(StandardCharsets.UTF_8),
                qrPayload.signatureBase64
            )

            if (!isValidSignature) {
                return Result.failure(SecurityException("Cryptographic verification failed: invalid signature on pairing token."))
            }

            val sharedSecretKey = cryptoManager.deriveSharedSessionKey(peerPubKey)
            val sharedKeyEncryptedBase64 = cryptoManager.encryptLocalString(
                Base64.encodeToString(sharedSecretKey.encoded, Base64.NO_WRAP)
            )

            // Save private nickname locally
            val nickname = privateNicknameForPartner.ifBlank { "Partner" }

            val pairing = PairingEntity(
                id = 1,
                peerDeviceId = qrPayload.deviceId,
                peerDisplayName = nickname,
                peerPublicKeyBase64 = qrPayload.publicKeyBase64,
                sharedKeyEncryptedBase64 = sharedKeyEncryptedBase64,
                pairedAt = now,
                isConfirmed = true,
                signalingServerUrl = qrPayload.signalingUrl,
                myGoogleUserId = myInternalGoogleUserId,
                peerGoogleUserId = ""
            )
            database.pairingDao().savePairing(pairing)

            signalingClient.connect(qrPayload.signalingUrl, cryptoManager.getDeviceId())

            val confirmationSignature = cryptoManager.sign(
                "CONFIRM|${qrPayload.token}|${cryptoManager.getDeviceId()}".toByteArray(StandardCharsets.UTF_8)
            )

            val confirmMsg = SignalingMessage(
                type = "PAIR_CONFIRM",
                deviceId = cryptoManager.getDeviceId(),
                recipientDeviceId = qrPayload.deviceId,
                googleUserId = myInternalGoogleUserId,
                username = myInternalGoogleUserId,
                publicKeyBase64 = cryptoManager.getLocalPublicKeyBase64(),
                token = qrPayload.token,
                signature = confirmationSignature
            )
            signalingClient.send(confirmMsg)

            Result.success(pairing)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun handlePairConfirmReceived(
        msg: SignalingMessage,
        expectedToken: String,
        privateNicknameForPartner: String,
        signalingUrl: String,
        myInternalGoogleUserId: String
    ): Result<PairingEntity> {
        return try {
            val peerDeviceId = msg.deviceId ?: return Result.failure(IllegalArgumentException("Missing peer deviceId"))
            val peerPubKeyBase64 = msg.publicKeyBase64 ?: return Result.failure(IllegalArgumentException("Missing peer public key"))
            val peerPubKey = cryptoManager.decodePublicKey(peerPubKeyBase64)

            val dataToVerify = "CONFIRM|${msg.token}|$peerDeviceId"
            val isValid = cryptoManager.verifySignature(
                peerPubKey,
                dataToVerify.toByteArray(StandardCharsets.UTF_8),
                msg.signature ?: ""
            )

            if (!isValid) {
                return Result.failure(SecurityException("Invalid confirmation signature from peer."))
            }

            val sharedSecretKey = cryptoManager.deriveSharedSessionKey(peerPubKey)
            val sharedKeyEncryptedBase64 = cryptoManager.encryptLocalString(
                Base64.encodeToString(sharedSecretKey.encoded, Base64.NO_WRAP)
            )

            val nickname = privateNicknameForPartner.ifBlank { "Partner" }

            val pairing = PairingEntity(
                id = 1,
                peerDeviceId = peerDeviceId,
                peerDisplayName = nickname,
                peerPublicKeyBase64 = peerPubKeyBase64,
                sharedKeyEncryptedBase64 = sharedKeyEncryptedBase64,
                pairedAt = System.currentTimeMillis(),
                isConfirmed = true,
                signalingServerUrl = signalingUrl,
                myGoogleUserId = myInternalGoogleUserId,
                peerGoogleUserId = msg.googleUserId ?: ""
            )

            database.pairingDao().savePairing(pairing)
            Result.success(pairing)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun establishMutualPairing(
        peerDeviceId: String,
        peerUsername: String,
        peerPublicKeyBase64: String,
        privateNicknameForPartner: String,
        myInternalGoogleUserId: String,
        signalingUrl: String = SignalingClient.DEFAULT_SUPABASE_URL
    ): Result<PairingEntity> {
        return try {
            if (peerDeviceId == cryptoManager.getDeviceId()) {
                return Result.failure(IllegalArgumentException("Cannot pair with your own device identity."))
            }

            val peerPubKey = cryptoManager.decodePublicKey(peerPublicKeyBase64)
            val sharedSecretKey = cryptoManager.deriveSharedSessionKey(peerPubKey)
            val sharedKeyEncryptedBase64 = cryptoManager.encryptLocalString(
                Base64.encodeToString(sharedSecretKey.encoded, Base64.NO_WRAP)
            )

            val nickname = privateNicknameForPartner.ifBlank { peerUsername.ifBlank { "Partner" } }
            val now = System.currentTimeMillis()

            val pairing = PairingEntity(
                id = 1,
                peerDeviceId = peerDeviceId,
                peerDisplayName = nickname,
                peerPublicKeyBase64 = peerPublicKeyBase64,
                sharedKeyEncryptedBase64 = sharedKeyEncryptedBase64,
                pairedAt = now,
                isConfirmed = true,
                signalingServerUrl = signalingUrl,
                myGoogleUserId = myInternalGoogleUserId,
                peerGoogleUserId = ""
            )
            database.pairingDao().savePairing(pairing)
            signalingClient.connect(signalingUrl, cryptoManager.getDeviceId())

            Result.success(pairing)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLocalNickname(newNickname: String) {
        database.pairingDao().updatePeerNickname(newNickname.trim())
    }

    suspend fun unpair() {
        database.pairingDao().clearPairing()
        database.messageDao().clearAllMessages()
        database.callLogDao().clearAllCallLogs()
        database.outboxDao().clearOutbox()
        database.tombstoneDao().clearTombstones()
        database.pendingDeletionDao().clearAllPending()
        signalingClient.disconnect()
    }
}
