package com.duolink.app.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager(private val context: Context) {

    private val secureRandom = SecureRandom()
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val prefs = context.getSharedPreferences("duolink_crypto_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val ANDROID_KEYSTORE_ALIAS = "DuoLinkMasterKey_v1"
        private const val LOCAL_DB_KEY_ALIAS = "DuoLinkLocalDbKey_v1"
        private const val EC_CURVE = "secp256r1"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PREF_PRIVATE_KEY_ENC = "pref_enc_private_key"
        private const val PREF_PUBLIC_KEY = "pref_public_key"
        private const val PREF_DEVICE_ID = "pref_device_id"
    }

    private var cachedKeyPair: KeyPair? = null

    init {
        ensureLocalIdentity()
    }

    /**
     * Initializes or loads the cryptographic identity for this device.
     */
    @Synchronized
    fun ensureLocalIdentity(): KeyPair {
        cachedKeyPair?.let { return it }

        val storedPubBase64 = prefs.getString(PREF_PUBLIC_KEY, null)
        val storedEncPriv = prefs.getString(PREF_PRIVATE_KEY_ENC, null)

        if (storedPubBase64 != null && storedEncPriv != null) {
            try {
                val pubKey = decodePublicKey(storedPubBase64)
                val privKeyBytes = decryptWithKeystore(Base64.decode(storedEncPriv, Base64.NO_WRAP))
                val kf = KeyFactory.getInstance("EC")
                val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(privKeyBytes))
                val pair = KeyPair(pubKey, privKey)
                cachedKeyPair = pair
                return pair
            } catch (e: Exception) {
                // If corrupted, re-generate identity
            }
        }

        // Generate fresh EC P-256 keypair
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(EC_CURVE), secureRandom)
        val keyPair = kpg.generateKeyPair()

        val pubBase64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        val encPrivBytes = encryptWithKeystore(keyPair.private.encoded)
        val encPrivBase64 = Base64.encodeToString(encPrivBytes, Base64.NO_WRAP)
        val deviceId = generateDeviceId(keyPair.public)

        prefs.edit()
            .putString(PREF_PUBLIC_KEY, pubBase64)
            .putString(PREF_PRIVATE_KEY_ENC, encPrivBase64)
            .putString(PREF_DEVICE_ID, deviceId)
            .apply()

        cachedKeyPair = keyPair
        return keyPair
    }

    fun getLocalPublicKey(): PublicKey = ensureLocalIdentity().public

    fun getLocalPrivateKey(): PrivateKey = ensureLocalIdentity().private

    fun getLocalPublicKeyBase64(): String {
        return Base64.encodeToString(getLocalPublicKey().encoded, Base64.NO_WRAP)
    }

    fun getDeviceId(): String {
        return prefs.getString(PREF_DEVICE_ID, null) ?: generateDeviceId(getLocalPublicKey()).also {
            prefs.edit().putString(PREF_DEVICE_ID, it).apply()
        }
    }

    fun generateDeviceId(publicKey: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKey.encoded)
        return "DL-" + digest.take(6).joinToString("") { "%02X".format(it) }
    }

    fun getFingerprint(publicKey: PublicKey = getLocalPublicKey()): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKey.encoded)
        return digest.take(16).joinToString(":") { "%02X".format(it) }
    }

    fun decodePublicKey(base64: String): PublicKey {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("EC").generatePublic(spec)
    }

    /**
     * Signs data using the local private key with SHA256withECDSA.
     */
    fun sign(data: ByteArray): String {
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(getLocalPrivateKey(), secureRandom)
        signer.update(data)
        return Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
    }

    /**
     * Verifies a digital signature against the peer's public key.
     */
    fun verifySignature(publicKey: PublicKey, data: ByteArray, signatureBase64: String): Boolean {
        return try {
            val signatureBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(publicKey)
            verifier.update(data)
            verifier.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Performs Elliptic Curve Diffie-Hellman (ECDH) key agreement and derives
     * an AES-256 encryption key using HKDF-SHA256.
     */
    fun deriveSharedSessionKey(peerPublicKey: PublicKey, salt: ByteArray = "DuoLink-Salt-v1".toByteArray(StandardCharsets.UTF_8)): SecretKey {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(getLocalPrivateKey())
        keyAgreement.doPhase(peerPublicKey, true)
        val rawSharedSecret = keyAgreement.generateSecret()

        // Derive 256-bit symmetric key using HKDF (Extract + Expand)
        val derivedKeyBytes = hkdfSha256(rawSharedSecret, salt, "DuoLink-AES256-GCM-Session".toByteArray(StandardCharsets.UTF_8), 32)
        return SecretKeySpec(derivedKeyBytes, "AES")
    }

    /**
     * Encrypts plaintext using AES-256-GCM with a fresh random 12-byte IV.
     */
    fun encryptAesGcm(secretKey: SecretKey, plaintext: ByteArray): EncryptedEnvelope {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)
        val ciphertext = cipher.doFinal(plaintext)

        return EncryptedEnvelope(
            senderDeviceId = getDeviceId(),
            recipientDeviceId = "",
            sequenceNumber = System.currentTimeMillis(),
            timestamp = System.currentTimeMillis(),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
    }

    /**
     * Decrypts ciphertext using AES-256-GCM.
     */
    fun decryptAesGcm(secretKey: SecretKey, envelope: EncryptedEnvelope): ByteArray {
        val iv = Base64.decode(envelope.ivBase64, Base64.NO_WRAP)
        val ciphertext = Base64.decode(envelope.ciphertextBase64, Base64.NO_WRAP)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(ciphertext)
    }

    /**
     * Encrypts string for local secure storage.
     */
    fun encryptLocalString(plainText: String): String {
        val localKey = getOrCreateKeystoreAesKey(LOCAL_DB_KEY_ALIAS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, localKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    /**
     * Decrypts string from local secure storage.
     */
    fun decryptLocalString(cipherTextBase64: String): String {
        return try {
            val localKey = getOrCreateKeystoreAesKey(LOCAL_DB_KEY_ALIAS)
            val combined = Base64.decode(cipherTextBase64, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return cipherTextBase64
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, localKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            cipherTextBase64
        }
    }

    fun generatePairingToken(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun clearIdentity() {
        prefs.edit().clear().apply()
        cachedKeyPair = null
    }

    // --- Private Helper Utilities ---

    private fun getOrCreateKeystoreAesKey(alias: String): SecretKey {
        if (!keyStore.containsAlias(alias)) {
            val keyGenerator = javax.crypto.KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore"
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    private fun encryptWithKeystore(data: ByteArray): ByteArray {
        val key = getOrCreateKeystoreAesKey(ANDROID_KEYSTORE_ALIAS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)
        return iv + ciphertext
    }

    private fun decryptWithKeystore(data: ByteArray): ByteArray {
        val key = getOrCreateKeystoreAesKey(ANDROID_KEYSTORE_ALIAS)
        val iv = data.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = data.copyOfRange(GCM_IV_LENGTH, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    /**
     * Standard RFC 5869 HKDF-SHA256
     */
    private fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
        // Extract
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)

        // Expand
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        var t = ByteArray(0)
        val okm = ByteArray(length)
        var generated = 0
        var round: Byte = 1

        while (generated < length) {
            mac.reset()
            mac.update(t)
            mac.update(info)
            mac.update(round)
            t = mac.doFinal()
            val toCopy = minOf(t.size, length - generated)
            System.arraycopy(t, 0, okm, generated, toCopy)
            generated += toCopy
            round++
        }
        return okm
    }
}

