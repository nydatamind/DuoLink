package com.duolink.app.data.webrtc

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

interface WebRtcEvents {
    fun onLocalDescription(sdp: SessionDescription)
    fun onIceCandidate(candidate: IceCandidate)
    fun onDataChannelMessage(data: ByteArray)
    fun onDataChannelStateChange(isOpen: Boolean)
    fun onIceConnectionChange(state: PeerConnection.IceConnectionState)
    fun onRemoteVideoTrack(track: VideoTrack)
}

class WebRtcManager(
    private val context: Context,
    private val events: WebRtcEvents
) {
    companion object {
        private const val TAG = "WebRtcManager"
        private const val LOCAL_AUDIO_TRACK_ID = "duolink_audio_track"
        private const val LOCAL_VIDEO_TRACK_ID = "duolink_video_track"
        private const val DATA_CHANNEL_LABEL = "duolink_p2p_channel"
    }

    val rootEglBase: EglBase = EglBase.create()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null

    private var audioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var cameraCapturer: CameraVideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    private val _isMicrophoneMuted = MutableStateFlow(false)
    val isMicrophoneMuted: StateFlow<Boolean> = _isMicrophoneMuted.asStateFlow()

    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()

    private val _isDataChannelOpen = MutableStateFlow(false)
    val isDataChannelOpen: StateFlow<Boolean> = _isDataChannelOpen.asStateFlow()

    private var isFrontFacing = true

    init {
        initPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)

        val encoderFactory = DefaultVideoEncoderFactory(
            rootEglBase.eglBaseContext,
            true,
            true
        )
        val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
    }

    fun initPeerConnection(
        iceServers: List<PeerConnection.IceServer>,
        isCaller: Boolean,
        enableVideo: Boolean
    ) {
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signaling state: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE Connection state: $state")
                state?.let { events.onIceConnectionChange(it) }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}

            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE Gathering state: $state")
            }

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { events.onIceCandidate(it) }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onAddStream(stream: MediaStream?) {}

            override fun onRemoveStream(stream: MediaStream?) {}

            override fun onDataChannel(dc: DataChannel?) {
                Log.d(TAG, "Remote DataChannel received: ${dc?.label()}")
                dataChannel = dc
                setupDataChannelObserver(dc)
            }

            override fun onRenegotiationNeeded() {
                Log.d(TAG, "Renegotiation needed")
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    Log.d(TAG, "Remote Video Track added")
                    events.onRemoteVideoTrack(track)
                }
            }
        })

        // Create local audio track
        val audioConstraints = MediaConstraints()
        audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack(LOCAL_AUDIO_TRACK_ID, audioSource)
        localAudioTrack?.setEnabled(true)
        localAudioTrack?.let { peerConnection?.addTrack(it) }

        // Create local video track if enabled
        if (enableVideo) {
            setupCameraVideoTrack()
        }

        // Caller creates DataChannel
        if (isCaller) {
            val dcInit = DataChannel.Init().apply {
                ordered = true
            }
            dataChannel = peerConnection?.createDataChannel(DATA_CHANNEL_LABEL, dcInit)
            setupDataChannelObserver(dataChannel)
        }
    }

    private fun setupCameraVideoTrack() {
        try {
            surfaceTextureHelper = SurfaceTextureHelper.create("SurfaceTextureHelper", rootEglBase.eglBaseContext)
            val enumerator = Camera2Enumerator(context)
            val deviceNames = enumerator.deviceNames

            var selectedName: String? = null
            for (name in deviceNames) {
                if (enumerator.isFrontFacing(name)) {
                    selectedName = name
                    isFrontFacing = true
                    break
                }
            }
            if (selectedName == null && deviceNames.isNotEmpty()) {
                selectedName = deviceNames[0]
                isFrontFacing = false
            }

            selectedName?.let { name ->
                cameraCapturer = enumerator.createCapturer(name, null)
                videoSource = peerConnectionFactory?.createVideoSource(cameraCapturer?.isScreencast == true)
                cameraCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
                cameraCapturer?.startCapture(1280, 720, 30)

                localVideoTrack = peerConnectionFactory?.createVideoTrack(LOCAL_VIDEO_TRACK_ID, videoSource)
                localVideoTrack?.setEnabled(true)
                localVideoTrack?.let { peerConnection?.addTrack(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera: ${e.message}")
        }
    }

    private fun setupDataChannelObserver(dc: DataChannel?) {
        dc?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}

            override fun onStateChange() {
                val isOpen = dc.state() == DataChannel.State.OPEN
                Log.d(TAG, "DataChannel state: ${dc.state()}")
                _isDataChannelOpen.value = isOpen
                events.onDataChannelStateChange(isOpen)
            }

            override fun onMessage(buffer: DataChannel.Buffer) {
                val data = ByteArray(buffer.data.remaining())
                buffer.data.get(data)
                events.onDataChannelMessage(data)
            }
        })
    }

    fun sendDataChannelMessage(bytes: ByteArray): Boolean {
        val dc = dataChannel ?: return false
        if (dc.state() != DataChannel.State.OPEN) return false

        val buffer = DataChannel.Buffer(ByteBuffer.wrap(bytes), false)
        return dc.send(buffer)
    }

    fun sendDataChannelString(text: String): Boolean {
        return sendDataChannelMessage(text.toByteArray(StandardCharsets.UTF_8))
    }

    fun createOffer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { sdp ->
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            events.onLocalDescription(sdp)
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, sdp)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(err: String?) {
                Log.e(TAG, "Create offer failed: $err")
            }
            override fun onSetFailure(err: String?) {}
        }, constraints)
    }

    fun createAnswer() {
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { sdp ->
                    peerConnection?.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            events.onLocalDescription(sdp)
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(p0: String?) {}
                    }, sdp)
                }
            }

            override fun onSetSuccess() {}
            override fun onCreateFailure(err: String?) {
                Log.e(TAG, "Create answer failed: $err")
            }
            override fun onSetFailure(err: String?) {}
        }, constraints)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set successfully")
            }
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(err: String?) {
                Log.e(TAG, "Set remote description failed: $err")
            }
        }, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun attachLocalVideoRenderer(renderer: SurfaceViewRenderer) {
        renderer.init(rootEglBase.eglBaseContext, null)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(isFrontFacing)
        localVideoTrack?.addSink(renderer)
    }

    fun attachRemoteVideoRenderer(renderer: SurfaceViewRenderer, track: VideoTrack) {
        renderer.init(rootEglBase.eglBaseContext, null)
        renderer.setEnableHardwareScaler(true)
        renderer.setMirror(false)
        track.addSink(renderer)
    }

    fun switchCamera() {
        cameraCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                isFrontFacing = isFront
            }

            override fun onCameraSwitchError(err: String?) {
                Log.e(TAG, "Camera switch error: $err")
            }
        })
    }

    fun setMicrophoneMuted(muted: Boolean) {
        _isMicrophoneMuted.value = muted
        localAudioTrack?.setEnabled(!muted)
    }

    fun setVideoEnabled(enabled: Boolean) {
        _isVideoEnabled.value = enabled
        localVideoTrack?.setEnabled(enabled)
    }

    fun release() {
        try {
            cameraCapturer?.stopCapture()
        } catch (e: Exception) {}
        try {
            cameraCapturer?.dispose()
        } catch (e: Exception) {}
        cameraCapturer = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        localVideoTrack?.setEnabled(false)
        localVideoTrack?.dispose()
        localVideoTrack = null

        videoSource?.dispose()
        videoSource = null

        localAudioTrack?.setEnabled(false)
        localAudioTrack?.dispose()
        localAudioTrack = null

        audioSource?.dispose()
        audioSource = null

        dataChannel?.unregisterObserver()
        dataChannel?.close()
        dataChannel?.dispose()
        dataChannel = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        _isDataChannelOpen.value = false
    }
}

