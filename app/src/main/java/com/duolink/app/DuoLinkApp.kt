package com.duolink.app

import android.app.Application
import android.util.Log
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import com.duolink.app.data.audio.VoicePlayerManager
import com.duolink.app.data.audio.VoiceRecorderManager
import com.duolink.app.data.auth.SupabaseAuthManager
import com.duolink.app.data.crypto.CryptoManager
import com.duolink.app.data.local.AppDatabase
import com.duolink.app.data.repository.CallRepository
import com.duolink.app.data.repository.ChatRepository
import com.duolink.app.data.repository.PairingRepository
import com.duolink.app.data.signaling.SignalingClient
import com.duolink.app.service.DuoLinkConnectionService
import com.duolink.app.service.VibrationAlertManager
import com.duolink.app.ui.mascot.OwlEyeStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DuoLinkApp : Application() {

    companion object {
        lateinit var instance: DuoLinkApp
            private set
    }

    lateinit var cryptoManager: CryptoManager
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var authManager: SupabaseAuthManager
        private set
    lateinit var profileManager: com.duolink.app.data.auth.SupabaseProfileManager
        private set
    lateinit var signalingClient: SignalingClient
        private set
    lateinit var pairingRepository: PairingRepository
        private set
    lateinit var chatRepository: ChatRepository
        private set
    lateinit var callRepository: CallRepository
        private set
    lateinit var voiceRecorderManager: VoiceRecorderManager
        private set
    lateinit var voicePlayerManager: VoicePlayerManager
        private set
    lateinit var vibrationAlertManager: VibrationAlertManager
        private set
    lateinit var owlEyeStateManager: OwlEyeStateManager
        private set

    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        try {
            EmojiCompat.init(BundledEmojiCompatConfig(this).setReplaceAll(true))
        } catch (e: Exception) {
            Log.e("DuoLinkApp", "Error initializing EmojiCompat: ${e.message}")
        }

        cryptoManager = CryptoManager(this)
        database = AppDatabase.getInstance(this)
        authManager = SupabaseAuthManager(this)
        signalingClient = SignalingClient()
        vibrationAlertManager = VibrationAlertManager(this)
        owlEyeStateManager = OwlEyeStateManager(this, signalingClient)
        pairingRepository = PairingRepository(cryptoManager, database, signalingClient)
        profileManager = com.duolink.app.data.auth.SupabaseProfileManager(
            this,
            authManager,
            cryptoManager,
            database,
            signalingClient,
            pairingRepository
        )
        chatRepository = ChatRepository(this, database, cryptoManager, signalingClient)
        callRepository = CallRepository(this, database, signalingClient)
        voiceRecorderManager = VoiceRecorderManager(this)
        voicePlayerManager = VoicePlayerManager(this)

        // WebRTC DataChannel connection with chat
        callRepository.webRtcManager?.let {
            chatRepository.webRtcManager = it
        }

        // Silent vibration alert system hook & dynamic owl eye state activity sync
        appScope.launch {
            signalingClient.incomingMessages.collect { msg ->
                val myDeviceId = cryptoManager.getDeviceId()
                if (msg.deviceId != null && msg.deviceId != myDeviceId) {
                    owlEyeStateManager.onPeerActivityDetected()
                }

                when (msg.type) {
                    "RELAY_MESSAGE" -> {
                        msg.messageId?.let { id ->
                            vibrationAlertManager.onNewMessageReceived(id)
                        }
                    }
                    "CALL_RING" -> {
                        if (msg.deviceId != myDeviceId) {
                            vibrationAlertManager.startCallVibration()
                        }
                    }
                    "CALL_ACCEPT", "CALL_REJECT", "CALL_END" -> {
                        vibrationAlertManager.stopCallVibration()
                    }
                }
            }
        }

        // Auto-connect to signaling (Supabase or self-hosted) and start foreground connection service
        appScope.launch {
            val pairing = database.pairingDao().getPairingOnce()
            if (pairing != null && pairing.isConfirmed) {
                val urlToConnect = pairing.signalingServerUrl.ifEmpty { SignalingClient.DEFAULT_SUPABASE_URL }
                Log.d("DuoLinkApp", "Connecting signaling: $urlToConnect")
                signalingClient.connect(urlToConnect, cryptoManager.getDeviceId())

                try {
                    val serviceIntent = android.content.Intent(this@DuoLinkApp, DuoLinkConnectionService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e("DuoLinkApp", "Error starting background service: ${e.message}")
                }
            }
        }
    }
}
