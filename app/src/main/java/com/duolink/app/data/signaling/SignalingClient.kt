package com.duolink.app.data.signaling

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

enum class SignalingState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    REGISTERED
}

class SignalingClient(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "SignalingClient"
        private const val HEARTBEAT_INTERVAL_MS = 25000L
        private const val RECONNECT_DELAY_MS = 3000L

        // Default Supabase Configuration provided by User
        const val DEFAULT_SUPABASE_URL = "https://kypyymlubmylmomoqyoq.supabase.co"
        const val DEFAULT_SUPABASE_KEY = "sb_publishable_wJt7PY1A73Od_VLvVMdJkw_89vY-6pM"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null
    private var currentDeviceId: String? = null
    private var isIntentionalDisconnect = false
    private var isSupabaseMode = false
    private var supabaseTopic = "realtime:duolink_p2p"
    private val refCounter = AtomicLong(1)

    private var heartbeatJob: Job? = null
    private var reconnectJob: Job? = null

    private val _state = MutableStateFlow(SignalingState.DISCONNECTED)
    val state: StateFlow<SignalingState> = _state.asStateFlow()

    private val _isPeerOnline = MutableStateFlow(false)
    val isPeerOnline: StateFlow<Boolean> = _isPeerOnline.asStateFlow()

    private val _peerLastSeen = MutableStateFlow(0L)
    val peerLastSeen: StateFlow<Long> = _peerLastSeen.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<SignalingMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<SignalingMessage> = _incomingMessages.asSharedFlow()

    fun connect(url: String, deviceId: String) {
        isIntentionalDisconnect = false
        currentUrl = url
        currentDeviceId = deviceId
        reconnectJob?.cancel()

        isSupabaseMode = url.contains("supabase.co") || url.startsWith("https://")
        val connectUrl = if (isSupabaseMode) {
            val baseUrl = if (url.startsWith("http")) url else DEFAULT_SUPABASE_URL
            val wsHost = baseUrl.replace("https://", "wss://").replace("http://", "ws://")
            "$wsHost/realtime/v1/websocket?apikey=$DEFAULT_SUPABASE_KEY&vsn=1.0.0"
        } else {
            url
        }

        initiateConnection(connectUrl)
    }

    private fun initiateConnection(wsUrl: String) {
        _state.value = SignalingState.CONNECTING
        Log.d(TAG, "Connecting to signaling at $wsUrl (SupabaseMode=$isSupabaseMode)")

        try {
            val request = Request.Builder().url(wsUrl).build()
            webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create WebSocket: ${e.message}")
            _state.value = SignalingState.DISCONNECTED
            scheduleReconnect()
        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Signaling WebSocket connected.")
                _state.value = SignalingState.CONNECTED
                startHeartbeat()

                if (isSupabaseMode) {
                    // Join Phoenix Realtime Channel for DuoLink
                    val joinMsg = buildJsonObject {
                        put("topic", supabaseTopic)
                        put("event", "phx_join")
                        put("payload", buildJsonObject {})
                        put("ref", refCounter.getAndIncrement().toString())
                    }
                    webSocket.send(joinMsg.toString())
                    _state.value = SignalingState.REGISTERED

                    // Announce presence
                    currentDeviceId?.let { id ->
                        send(SignalingMessage(type = "REGISTER", deviceId = id, isOnline = true))
                    }
                } else {
                    currentDeviceId?.let { id ->
                        sendRegister(id)
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    if (isSupabaseMode) {
                        parseSupabaseMessage(text)
                    } else {
                        val message = jsonSerializer.decodeFromString<SignalingMessage>(text)
                        handleIncomingMessage(message)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing signaling message: ${e.message}")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _state.value = SignalingState.DISCONNECTED
                stopHeartbeat()
                if (!isIntentionalDisconnect) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Signaling failure: ${t.message}")
                _state.value = SignalingState.DISCONNECTED
                stopHeartbeat()
                if (!isIntentionalDisconnect) {
                    scheduleReconnect()
                }
            }
        }
    }

    private fun parseSupabaseMessage(text: String) {
        try {
            val json = jsonSerializer.parseToJsonElement(text).jsonObject
            val event = json["event"]?.jsonPrimitive?.content

            if (event == "broadcast") {
                val payloadObj = json["payload"]?.jsonObject
                val innerPayload = payloadObj?.get("payload")?.jsonObject
                if (innerPayload != null) {
                    val msg = jsonSerializer.decodeFromJsonElement(SignalingMessage.serializer(), innerPayload)
                    // Ignore broadcasts sent by self
                    if (msg.deviceId != currentDeviceId) {
                        handleIncomingMessage(msg)
                    }
                }
            } else if (event == "phx_reply") {
                _state.value = SignalingState.REGISTERED
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding Supabase message: ${e.message}")
        }
    }

    private fun handleIncomingMessage(msg: SignalingMessage) {
        when (msg.type) {
            "REGISTER_ACK" -> {
                _state.value = SignalingState.REGISTERED
            }
            "REGISTER" -> {
                // Peer registered / came online
                _isPeerOnline.value = true
                _peerLastSeen.value = System.currentTimeMillis()
            }
            "PEER_STATUS" -> {
                msg.isOnline?.let { _isPeerOnline.value = it }
                msg.lastSeen?.let { if (it > 0) _peerLastSeen.value = it }
            }
        }

        scope.launch {
            _incomingMessages.emit(msg)
        }
    }

    fun send(message: SignalingMessage): Boolean {
        val socket = webSocket ?: return false
        return try {
            if (isSupabaseMode) {
                val signalJson = jsonSerializer.encodeToJsonElement(SignalingMessage.serializer(), message)
                val broadcastMsg = buildJsonObject {
                    put("topic", supabaseTopic)
                    put("event", "broadcast")
                    put("payload", buildJsonObject {
                        put("type", "broadcast")
                        put("event", "signal")
                        put("payload", signalJson)
                    })
                    put("ref", refCounter.getAndIncrement().toString())
                }
                socket.send(broadcastMsg.toString())
            } else {
                val json = jsonSerializer.encodeToString(SignalingMessage.serializer(), message)
                socket.send(json)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}")
            false
        }
    }

    fun sendRegister(deviceId: String) {
        send(SignalingMessage(type = "REGISTER", deviceId = deviceId))
    }

    fun sendTyping(recipientDeviceId: String, isTyping: Boolean) {
        send(SignalingMessage(type = "TYPING", deviceId = currentDeviceId, recipientDeviceId = recipientDeviceId, isTyping = isTyping))
    }

    fun sendReadReceipt(recipientDeviceId: String, messageId: String) {
        send(SignalingMessage(type = "READ_RECEIPT", deviceId = currentDeviceId, recipientDeviceId = recipientDeviceId, messageId = messageId))
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                if (_state.value == SignalingState.REGISTERED || _state.value == SignalingState.CONNECTED) {
                    if (isSupabaseMode) {
                        val heartbeat = buildJsonObject {
                            put("topic", "phoenix")
                            put("event", "heartbeat")
                            put("payload", buildJsonObject {})
                            put("ref", refCounter.getAndIncrement().toString())
                        }
                        webSocket?.send(heartbeat.toString())
                    } else {
                        send(SignalingMessage(type = "PING"))
                    }
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            currentUrl?.let { url ->
                initiateConnection(url)
            }
        }
    }

    fun disconnect() {
        isIntentionalDisconnect = true
        reconnectJob?.cancel()
        stopHeartbeat()
        try {
            webSocket?.close(1000, "User initiated disconnect")
        } catch (e: Exception) {}
        webSocket = null
        _state.value = SignalingState.DISCONNECTED
        _isPeerOnline.value = false
    }
}
