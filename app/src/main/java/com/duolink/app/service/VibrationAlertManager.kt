package com.duolink.app.service

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VibrationAlertManager(private val context: Context) {

    companion object {
        private const val TAG = "VibrationAlertManager"
        // Message pattern: buzz 200ms, pause 150ms, buzz 250ms, pause 1500ms
        val MESSAGE_VIBRATION_PATTERN = longArrayOf(0, 200, 150, 250, 1500)
        // Call pattern: buzz 1000ms, pause 600ms
        val CALL_VIBRATION_PATTERN = longArrayOf(0, 1000, 600)
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private var messageVibrationJob: Job? = null
    private var callVibrationJob: Job? = null
    private var lastVibratedMessageId: String? = null

    private val _isVibratingForMessage = MutableStateFlow(false)
    val isVibratingForMessage: StateFlow<Boolean> = _isVibratingForMessage.asStateFlow()

    private val _isVibratingForCall = MutableStateFlow(false)
    val isVibratingForCall: StateFlow<Boolean> = _isVibratingForCall.asStateFlow()

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DuoLink:VibrationAlertLock").apply {
                    setReferenceCounted(false)
                }
            }
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max safeguard
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock: ${e.message}")
        }
    }

    /**
     * Triggers repeating vibration when a new message arrives.
     * Repeats pattern continuously until acknowledged or chat is opened.
     */
    @Synchronized
    fun onNewMessageReceived(messageId: String) {
        if (messageId == lastVibratedMessageId) return
        lastVibratedMessageId = messageId

        stopMessageVibration()
        _isVibratingForMessage.value = true
        acquireWakeLock()

        messageVibrationJob = scope.launch {
            try {
                var cycles = 0
                // Repeat continuously until user opens the chat or stops vibration (max 250 cycles = ~10 mins)
                while (isActive && _isVibratingForMessage.value && cycles < 250) {
                    performSingleVibrate(longArrayOf(0, 220, 140, 260), -1)
                    delay(2200)
                    cycles++
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during message vibration: ${e.message}")
            } finally {
                _isVibratingForMessage.value = false
                if (!_isVibratingForCall.value) {
                    releaseWakeLock()
                }
            }
        }
    }

    /**
     * Stops message vibration immediately upon opening chat or user action.
     */
    @Synchronized
    fun stopMessageVibration() {
        messageVibrationJob?.cancel()
        messageVibrationJob = null
        _isVibratingForMessage.value = false
        if (!_isVibratingForCall.value) {
            releaseWakeLock()
        }
        try {
            vibrator.cancel()
        } catch (e: Exception) {}
    }

    /**
     * Starts urgent repeating vibration when peer calls.
     * Continues until answered, declined, or call ends.
     */
    @Synchronized
    fun startCallVibration() {
        stopCallVibration()
        _isVibratingForCall.value = true
        acquireWakeLock()

        callVibrationJob = scope.launch {
            try {
                while (isActive && _isVibratingForCall.value) {
                    performSingleVibrate(longArrayOf(0, 1000), -1)
                    delay(1600)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during call vibration: ${e.message}")
            } finally {
                _isVibratingForCall.value = false
                if (!_isVibratingForMessage.value) {
                    releaseWakeLock()
                }
            }
        }
    }

    /**
     * Stops call vibration immediately when call is answered, declined, or ended.
     */
    @Synchronized
    fun stopCallVibration() {
        callVibrationJob?.cancel()
        callVibrationJob = null
        _isVibratingForCall.value = false
        if (!_isVibratingForMessage.value) {
            releaseWakeLock()
        }
        try {
            vibrator.cancel()
        } catch (e: Exception) {}
    }

    fun testMessageVibration() {
        scope.launch {
            performSingleVibrate(longArrayOf(0, 220, 140, 260), -1)
        }
    }

    fun testCallVibration() {
        scope.launch {
            performSingleVibrate(longArrayOf(0, 1000, 600, 1000), -1)
        }
    }

    private fun performSingleVibrate(timings: LongArray, repeat: Int) {
        try {
            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, repeat)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, repeat)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate: ${e.message}")
        }
    }
}

