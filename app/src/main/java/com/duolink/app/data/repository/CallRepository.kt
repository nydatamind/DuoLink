package com.duolink.app.data.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.duolink.app.data.local.AppDatabase
import com.duolink.app.data.local.CallDirection
import com.duolink.app.data.local.CallLogEntity
import com.duolink.app.data.local.CallStatus
import com.duolink.app.data.local.CallType
import com.duolink.app.data.signaling.SignalingClient
import com.duolink.app.data.signaling.SignalingMessage
import com.duolink.app.data.webrtc.AudioSwitchManager
import com.duolink.app.data.webrtc.WebRtcEvents
import com.duolink.app.data.webrtc.WebRtcManager
import com.duolink.app.service.DuoLinkCallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import java.util.UUID

enum class CallState {
    IDLE,
    OUTGOING_CALLING,
    INCOMING_RINGING,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ENDED,
    FAILED
}

class CallRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val signalingClient: SignalingClient,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    companion object {
        private const val TAG = "CallRepository"
        private const val RINGING_TIMEOUT_MS = 45000L
    }

    val audioSwitchManager = AudioSwitchManager(context)
    var webRtcManager: WebRtcManager? = null
        private set

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _currentCallType = MutableStateFlow(CallType.VOICE)
    val currentCallType: StateFlow<CallType> = _currentCallType.asStateFlow()

    private val _currentCallId = MutableStateFlow<String?>(null)
    val currentCallId: StateFlow<String?> = _currentCallId.asStateFlow()

    private val _callDurationSeconds = MutableStateFlow(0L)
    val callDurationSeconds: StateFlow<Long> = _callDurationSeconds.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    private var durationTimerJob: Job? = null
    private var ringingTimeoutJob: Job? = null
    private var currentCallStartTime = 0L
    private var currentDirection = CallDirection.OUTGOING
    private var pendingIceCandidates = mutableListOf<IceCandidate>()

    init {
        scope.launch(Dispatchers.IO) {
            signalingClient.incomingMessages.collect { msg ->
                handleSignalingCallMessage(msg)
            }
        }
    }

    private val webRtcEvents = object : WebRtcEvents {
        override fun onLocalDescription(sdp: SessionDescription) {
            val callId = _currentCallId.value ?: return
            scope.launch(Dispatchers.IO) {
                val pairing = database.pairingDao().getPairingOnce() ?: return@launch
                val msgType = if (sdp.type == SessionDescription.Type.OFFER) "OFFER" else "ANSWER"
                val msg = SignalingMessage(
                    type = msgType,
                    deviceId = signalingClient.state.value.name,
                    recipientDeviceId = pairing.peerDeviceId,
                    callId = callId,
                    sdp = sdp.description,
                    sdpType = sdp.type.canonicalForm()
                )
                signalingClient.send(msg)
            }
        }

        override fun onIceCandidate(candidate: IceCandidate) {
            val callId = _currentCallId.value ?: return
            scope.launch(Dispatchers.IO) {
                val pairing = database.pairingDao().getPairingOnce() ?: return@launch
                val msg = SignalingMessage(
                    type = "ICE_CANDIDATE",
                    recipientDeviceId = pairing.peerDeviceId,
                    callId = callId,
                    candidate = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex
                )
                signalingClient.send(msg)
            }
        }

        override fun onDataChannelMessage(data: ByteArray) {
            // Forward to chat or file receiver
        }

        override fun onDataChannelStateChange(isOpen: Boolean) {
            Log.d(TAG, "WebRTC DataChannel isOpen: $isOpen")
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            scope.launch(Dispatchers.Main) {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        _callState.value = CallState.CONNECTED
                        startCallDurationTimer()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        _callState.value = CallState.RECONNECTING
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        endCall(CallStatus.FAILED)
                    }
                    PeerConnection.IceConnectionState.CLOSED -> {
                        if (_callState.value != CallState.ENDED) {
                            _callState.value = CallState.ENDED
                        }
                    }
                    else -> {}
                }
            }
        }

        override fun onRemoteVideoTrack(track: VideoTrack) {
            scope.launch(Dispatchers.Main) {
                _remoteVideoTrack.value = track
            }
        }
    }

    suspend fun startOutgoingCall(type: CallType) {
        val pairing = database.pairingDao().getPairingOnce()
            ?: throw IllegalStateException("Device not paired.")

        val callId = UUID.randomUUID().toString()
        _currentCallId.value = callId
        _currentCallType.value = type
        _callState.value = CallState.OUTGOING_CALLING
        currentDirection = CallDirection.OUTGOING
        currentCallStartTime = System.currentTimeMillis()

        audioSwitchManager.startCallAudio(enableSpeaker = type == CallType.VIDEO)
        initWebRtc(isCaller = true, enableVideo = type == CallType.VIDEO, pairing = pairing)

        // Start DuoLink Call Foreground Service
        startForegroundCallService("Outgoing ${type.name.lowercase()} call...")

        // Send CALL_RING to peer
        val ringMsg = SignalingMessage(
            type = "CALL_RING",
            recipientDeviceId = pairing.peerDeviceId,
            callId = callId,
            callType = type.name
        )
        signalingClient.send(ringMsg)

        // WebRTC Offer
        webRtcManager?.createOffer()

        // Ringing timeout
        startRingingTimeout()
    }

    suspend fun acceptIncomingCall() {
        val callId = _currentCallId.value ?: return
        val pairing = database.pairingDao().getPairingOnce() ?: return

        ringingTimeoutJob?.cancel()
        _callState.value = CallState.CONNECTING

        val isVideo = _currentCallType.value == CallType.VIDEO
        audioSwitchManager.startCallAudio(enableSpeaker = isVideo)
        initWebRtc(isCaller = false, enableVideo = isVideo, pairing = pairing)

        // Send CALL_ACCEPT to peer
        val acceptMsg = SignalingMessage(
            type = "CALL_ACCEPT",
            recipientDeviceId = pairing.peerDeviceId,
            callId = callId
        )
        signalingClient.send(acceptMsg)

        // Process any queued ICE candidates
        for (cand in pendingIceCandidates) {
            webRtcManager?.addIceCandidate(cand)
        }
        pendingIceCandidates.clear()

        // Create WebRTC Answer
        webRtcManager?.createAnswer()

        startForegroundCallService("Active ${_currentCallType.value.name.lowercase()} call")
    }

    fun rejectIncomingCall() {
        ringingTimeoutJob?.cancel()
        val callId = _currentCallId.value
        scope.launch(Dispatchers.IO) {
            val pairing = database.pairingDao().getPairingOnce()
            if (callId != null && pairing != null) {
                signalingClient.send(
                    SignalingMessage(
                        type = "CALL_REJECT",
                        recipientDeviceId = pairing.peerDeviceId,
                        callId = callId,
                        reason = "DECLINED"
                    )
                )
            }
        }
        endCall(CallStatus.REJECTED)
    }

    fun endCall(status: CallStatus = CallStatus.COMPLETED) {
        ringingTimeoutJob?.cancel()
        durationTimerJob?.cancel()

        val callId = _currentCallId.value
        val duration = _callDurationSeconds.value

        scope.launch(Dispatchers.IO) {
            val pairing = database.pairingDao().getPairingOnce()
            if (callId != null && pairing != null) {
                signalingClient.send(
                    SignalingMessage(
                        type = "CALL_END",
                        recipientDeviceId = pairing.peerDeviceId,
                        callId = callId
                    )
                )

                // Log call
                val log = CallLogEntity(
                    id = callId,
                    peerDeviceId = pairing.peerDeviceId,
                    peerDisplayName = pairing.peerDisplayName,
                    callType = _currentCallType.value,
                    direction = currentDirection,
                    status = status,
                    startedAt = currentCallStartTime,
                    durationSeconds = duration
                )
                database.callLogDao().insertCallLog(log)
            }
        }

        cleanupCallResources()
        _callState.value = CallState.ENDED
        stopForegroundCallService()
    }

    private fun initWebRtc(isCaller: Boolean, enableVideo: Boolean, pairing: com.duolink.app.data.local.PairingEntity) {
        webRtcManager?.release()
        val manager = WebRtcManager(context, webRtcEvents)
        val iceServers = mutableListOf(
            PeerConnection.IceServer.builder(pairing.stunServerUrl).createIceServer()
        )
        if (pairing.turnServerUrl.isNotEmpty()) {
            val turnBuilder = PeerConnection.IceServer.builder(pairing.turnServerUrl)
            if (pairing.turnUsername.isNotEmpty()) {
                turnBuilder.setUsername(pairing.turnUsername)
                turnBuilder.setPassword(pairing.turnCredential)
            }
            iceServers.add(turnBuilder.createIceServer())
        }

        manager.initPeerConnection(iceServers, isCaller, enableVideo)
        webRtcManager = manager
    }

    private fun handleSignalingCallMessage(msg: SignalingMessage) {
        scope.launch(Dispatchers.Main) {
            when (msg.type) {
                "CALL_RING" -> {
                    if (_callState.value == CallState.IDLE) {
                        _currentCallId.value = msg.callId
                        _currentCallType.value = if (msg.callType == "VIDEO") CallType.VIDEO else CallType.VOICE
                        _callState.value = CallState.INCOMING_RINGING
                        currentDirection = CallDirection.INCOMING
                        currentCallStartTime = System.currentTimeMillis()
                        startRingingTimeout()
                        startForegroundCallService("Incoming ${_currentCallType.value.name.lowercase()} call...")
                    } else {
                        // Busy signal
                        val pairing = database.pairingDao().getPairingOnce()
                        if (pairing != null && msg.callId != null) {
                            signalingClient.send(
                                SignalingMessage(
                                    type = "CALL_REJECT",
                                    recipientDeviceId = msg.deviceId ?: pairing.peerDeviceId,
                                    callId = msg.callId,
                                    reason = "BUSY"
                                )
                            )
                        }
                    }
                }

                "CALL_ACCEPT" -> {
                    if (_callState.value == CallState.OUTGOING_CALLING) {
                        ringingTimeoutJob?.cancel()
                        _callState.value = CallState.CONNECTING
                    }
                }

                "CALL_REJECT" -> {
                    endCall(CallStatus.REJECTED)
                }

                "CALL_END" -> {
                    endCall(CallStatus.COMPLETED)
                }

                "OFFER" -> {
                    msg.sdp?.let { sdpText ->
                        val desc = SessionDescription(SessionDescription.Type.OFFER, sdpText)
                        webRtcManager?.setRemoteDescription(desc)
                    }
                }

                "ANSWER" -> {
                    msg.sdp?.let { sdpText ->
                        val desc = SessionDescription(SessionDescription.Type.ANSWER, sdpText)
                        webRtcManager?.setRemoteDescription(desc)
                    }
                }

                "ICE_CANDIDATE" -> {
                    val sdp = msg.candidate ?: return@launch
                    val mid = msg.sdpMid ?: ""
                    val lineIndex = msg.sdpMLineIndex ?: 0
                    val candidate = IceCandidate(mid, lineIndex, sdp)

                    if (webRtcManager != null) {
                        webRtcManager?.addIceCandidate(candidate)
                    } else {
                        pendingIceCandidates.add(candidate)
                    }
                }
            }
        }
    }

    private fun startRingingTimeout() {
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = scope.launch {
            delay(RINGING_TIMEOUT_MS)
            if (_callState.value == CallState.OUTGOING_CALLING || _callState.value == CallState.INCOMING_RINGING) {
                endCall(CallStatus.MISSED)
            }
        }
    }

    private fun startCallDurationTimer() {
        durationTimerJob?.cancel()
        _callDurationSeconds.value = 0L
        durationTimerJob = scope.launch {
            while (isActive && _callState.value == CallState.CONNECTED) {
                delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    private fun cleanupCallResources() {
        audioSwitchManager.stopCallAudio()
        webRtcManager?.release()
        webRtcManager = null
        _remoteVideoTrack.value = null
        pendingIceCandidates.clear()
    }

    private fun startForegroundCallService(title: String) {
        val intent = Intent(context, DuoLinkCallService::class.java).apply {
            action = DuoLinkCallService.ACTION_START_CALL
            putExtra(DuoLinkCallService.EXTRA_CALL_TITLE, title)
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call service: ${e.message}")
        }
    }

    private fun stopForegroundCallService() {
        val intent = Intent(context, DuoLinkCallService::class.java).apply {
            action = DuoLinkCallService.ACTION_STOP_CALL
        }
        try {
            context.stopService(intent)
        } catch (e: Exception) {}
    }
}

