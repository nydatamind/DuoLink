package com.duolink.app.data.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

class AudioSwitchManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalMode = AudioManager.MODE_NORMAL
    private var originalSpeakerphone = false
    private var originalMicrophoneMute = false

    companion object {
        private const val TAG = "AudioSwitchManager"
    }

    fun startCallAudio(enableSpeaker: Boolean = false) {
        try {
            originalMode = audioManager.mode
            originalSpeakerphone = audioManager.isSpeakerphoneOn
            originalMicrophoneMute = audioManager.isMicrophoneMute

            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            setSpeakerphone(enableSpeaker)
            setMicrophoneMute(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting call audio: ${e.message}")
        }
    }

    fun setSpeakerphone(enabled: Boolean) {
        try {
            audioManager.isSpeakerphoneOn = enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speakerphone: ${e.message}")
        }
    }

    fun isSpeakerphoneOn(): Boolean {
        return audioManager.isSpeakerphoneOn
    }

    fun setMicrophoneMute(muted: Boolean) {
        try {
            audioManager.isMicrophoneMute = muted
        } catch (e: Exception) {
            Log.e(TAG, "Error setting mic mute: ${e.message}")
        }
    }

    fun stopCallAudio() {
        try {
            audioManager.isSpeakerphoneOn = originalSpeakerphone
            audioManager.isMicrophoneMute = originalMicrophoneMute
            audioManager.mode = originalMode
        } catch (e: Exception) {
            Log.e(TAG, "Error resetting audio mode: ${e.message}")
        }
    }
}

