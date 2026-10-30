package com.duolink.app.ui.mascot

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.duolink.app.data.signaling.SignalingClient
import com.duolink.app.data.signaling.SignalingMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OwlEyeStateManager(
    private val context: Context,
    private val signalingClient: SignalingClient
) {
    companion object {
        private const val TAG = "OwlEyeStateManager"
        private const val PREFS_NAME = "duolink_owl_state"
        private const val KEY_PEER_LAST_ACTIVE = "peer_last_active_timestamp"

        // Configurable thresholds in minutes
        const val THRESHOLD_OPEN_MINUTES = 5L
        const val THRESHOLD_HALF_MINUTES = 60L
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _owlEyeState = MutableStateFlow(OwlEyeState.OPEN)
    val owlEyeState: StateFlow<OwlEyeState> = _owlEyeState.asStateFlow()

    private val _peerLastActive = MutableStateFlow(prefs.getLong(KEY_PEER_LAST_ACTIVE, System.currentTimeMillis()))
    val peerLastActive: StateFlow<Long> = _peerLastActive.asStateFlow()

    private var evaluationJob: Job? = null
    private var heartbeatJob: Job? = null

    init {
        recalculateState()
        startEvaluationTicker()
        startHeartbeatSender()
    }

    /**
     * Call whenever any message, typing indicator, call packet, or heartbeat is received from the peer.
     */
    fun onPeerActivityDetected(timestamp: Long = System.currentTimeMillis()) {
        _peerLastActive.value = timestamp
        prefs.edit().putLong(KEY_PEER_LAST_ACTIVE, timestamp).apply()
        recalculateState()
    }

    /**
     * Calculates the eye state based on elapsed time since partner was last active.
     * 0 - 5 min: FULLY OPEN
     * 5 min - 60 min: HALF CLOSED
     * > 60 min: COMPLETELY CLOSED
     */
    fun recalculateState() {
        val lastActive = _peerLastActive.value
        val now = System.currentTimeMillis()
        val elapsedMinutes = (now - lastActive).coerceAtLeast(0L) / (1000 * 60)

        val newState = when {
            elapsedMinutes < THRESHOLD_OPEN_MINUTES -> OwlEyeState.OPEN
            elapsedMinutes < THRESHOLD_HALF_MINUTES -> OwlEyeState.HALF
            else -> OwlEyeState.CLOSED
        }

        if (_owlEyeState.value != newState) {
            _owlEyeState.value = newState
            Log.d(TAG, "OwlEyeState transitioned to: $newState (elapsed: $elapsedMinutes min)")
            DynamicIconManager.setLauncherIcon(context, newState)
        }
    }

    /**
     * Periodically ticks to transition eyes from OPEN -> HALF -> CLOSED as time passes.
     */
    private fun startEvaluationTicker() {
        evaluationJob?.cancel()
        evaluationJob = scope.launch {
            while (isActive) {
                delay(15_000) // check every 15s
                recalculateState()
            }
        }
    }

    /**
     * Broadcasts a lightweight activity heartbeat via Supabase Realtime / WebSocket every 45 seconds.
     */
    private fun startHeartbeatSender() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(45_000)
                try {
                    val heartbeat = SignalingMessage(
                        type = "HEARTBEAT",
                        timestamp = System.currentTimeMillis()
                    )
                    signalingClient.send(heartbeat)
                } catch (e: Exception) {
                    // Ignored if offline
                }
            }
        }
    }

    /**
     * Allows manually testing/previewing states in diagnostics or settings.
     */
    fun forceTestState(state: OwlEyeState) {
        val simulatedTimestamp = when (state) {
            OwlEyeState.OPEN -> System.currentTimeMillis()
            OwlEyeState.HALF -> System.currentTimeMillis() - (15 * 60 * 1000L) // 15 mins ago
            OwlEyeState.CLOSED -> System.currentTimeMillis() - (120 * 60 * 1000L) // 2 hours ago
        }
        onPeerActivityDetected(simulatedTimestamp)
    }
}

