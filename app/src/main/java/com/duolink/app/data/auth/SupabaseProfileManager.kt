package com.duolink.app.data.auth

import android.content.Context
import android.util.Log
import com.duolink.app.data.crypto.CryptoManager
import com.duolink.app.data.local.AppDatabase
import com.duolink.app.data.local.PairingEntity
import com.duolink.app.data.repository.PairingRepository
import com.duolink.app.data.signaling.SignalingClient
import com.duolink.app.data.signaling.SignalingMessage
import com.duolink.app.data.signaling.jsonSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

@Serializable
data class PartnerSearchResult(
    val userId: String,
    val username: String,
    val deviceId: String,
    val publicKeyBase64: String
)

@Serializable
data class PairingRequest(
    val id: String,
    val senderUserId: String,
    val senderUsername: String,
    val senderDeviceId: String,
    val senderPublicKey: String,
    val receiverUsername: String,
    val status: String = "PENDING",
    val timestamp: Long = System.currentTimeMillis()
)

class SupabaseProfileManager(
    private val context: Context,
    private val authManager: SupabaseAuthManager,
    private val cryptoManager: CryptoManager,
    private val database: AppDatabase,
    private val signalingClient: SignalingClient,
    private val pairingRepository: PairingRepository
) {
    companion object {
        private const val TAG = "SupabaseProfileManager"
        private const val PREFS_NAME = "duolink_profile_prefs"
        private const val KEY_OUTGOING_REQUEST = "pending_outgoing_request"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = OkHttpClient()
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _incomingRequests = MutableStateFlow<List<PairingRequest>>(emptyList())
    val incomingRequests: StateFlow<List<PairingRequest>> = _incomingRequests.asStateFlow()

    private val _outgoingRequest = MutableStateFlow<PairingRequest?>(null)
    val outgoingRequest: StateFlow<PairingRequest?> = _outgoingRequest.asStateFlow()

    private val _pairingSuccessEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val pairingSuccessEvent: SharedFlow<Unit> = _pairingSuccessEvent.asSharedFlow()

    private val _discoveredPartners = MutableStateFlow<List<PartnerSearchResult>>(emptyList())
    val discoveredPartners: StateFlow<List<PartnerSearchResult>> = _discoveredPartners.asStateFlow()

    // Realtime discovery cache
    private val cachedDiscoveryUsers = mutableMapOf<String, PartnerSearchResult>()

    init {
        loadPendingOutgoingRequest()
        listenForSignalingEvents()
    }

    fun broadcastPresence() {
        val myUsername = authManager.getUsername()?.lowercase() ?: return
        val myDeviceId = cryptoManager.getDeviceId()
        val myPubKey = cryptoManager.getLocalPublicKeyBase64()
        val myUserId = authManager.getInternalUserId() ?: myDeviceId
        signalingClient.send(
            SignalingMessage(
                type = "USER_PRESENCE",
                deviceId = myDeviceId,
                googleUserId = myUserId,
                username = myUsername,
                publicKeyBase64 = myPubKey
            )
        )
    }

    fun broadcastQuery(query: String = "*") {
        val myUsername = authManager.getUsername()?.lowercase() ?: ""
        val myDeviceId = cryptoManager.getDeviceId()
        signalingClient.send(
            SignalingMessage(
                type = "QUERY_USER",
                query = query,
                username = myUsername,
                deviceId = myDeviceId
            )
        )
    }

    private fun loadPendingOutgoingRequest() {
        val storedJson = prefs.getString(KEY_OUTGOING_REQUEST, null)
        if (!storedJson.isNullOrBlank()) {
            try {
                _outgoingRequest.value = jsonSerializer.decodeFromString<PairingRequest>(storedJson)
            } catch (e: Exception) {
                Log.w(TAG, "Error restoring outgoing request: ${e.message}")
            }
        }
    }

    private fun saveOutgoingRequest(req: PairingRequest?) {
        _outgoingRequest.value = req
        if (req != null) {
            prefs.edit().putString(KEY_OUTGOING_REQUEST, jsonSerializer.encodeToString(PairingRequest.serializer(), req)).apply()
        } else {
            prefs.edit().remove(KEY_OUTGOING_REQUEST).apply()
        }
    }

    /**
     * Listens for Realtime broadcast signals for pairing discovery, requests, and acceptances.
     */
    private fun listenForSignalingEvents() {
        scope.launch {
            signalingClient.incomingMessages.collect { msg ->
                val myUsername = authManager.getUsername()?.lowercase() ?: return@collect
                val myDeviceId = cryptoManager.getDeviceId()

                // Ignore messages sent by self
                if (msg.deviceId == myDeviceId) return@collect

                when (msg.type) {
                    "QUERY_USER" -> {
                        val q = msg.query?.lowercase() ?: ""
                        if (q.isBlank() || myUsername.contains(q) || q == "*") {
                            broadcastPresence()
                        }
                    }

                    "USER_PRESENCE" -> {
                        val uName = msg.username?.lowercase()
                        val pKey = msg.publicKeyBase64
                        val devId = msg.deviceId
                        if (!uName.isNullOrBlank() && !pKey.isNullOrBlank() && !devId.isNullOrBlank()) {
                            cachedDiscoveryUsers[uName] = PartnerSearchResult(
                                userId = msg.googleUserId ?: devId,
                                username = uName,
                                deviceId = devId,
                                publicKeyBase64 = pKey
                            )
                            _discoveredPartners.value = cachedDiscoveryUsers.values.filter { it.username != myUsername && it.deviceId != myDeviceId }
                        }
                    }

                    "PAIR_REQUEST" -> {
                        val target = msg.targetUsername?.removePrefix("@")?.trim()?.lowercase() ?: ""
                        val isForMe = target == myUsername || msg.recipientDeviceId == myDeviceId
                        if (isForMe) {
                            val senderName = msg.username ?: "Partner"
                            val senderDevId = msg.deviceId ?: ""
                            val senderKey = msg.publicKeyBase64 ?: ""
                            val senderUid = msg.googleUserId ?: senderDevId

                            if (senderDevId.isNotBlank() && senderKey.isNotBlank()) {
                                val req = PairingRequest(
                                    id = msg.token ?: UUID.randomUUID().toString(),
                                    senderUserId = senderUid,
                                    senderUsername = senderName,
                                    senderDeviceId = senderDevId,
                                    senderPublicKey = senderKey,
                                    receiverUsername = myUsername,
                                    status = "PENDING",
                                    timestamp = msg.timestamp
                                )
                                // Avoid duplicate entries
                                val current = _incomingRequests.value.filter { it.senderDeviceId != senderDevId }
                                _incomingRequests.value = current + req
                            }
                        }
                    }

                    "PAIR_ACCEPT" -> {
                        val target = msg.targetUsername?.removePrefix("@")?.trim()?.lowercase() ?: ""
                        val isForMe = target == myUsername || msg.recipientDeviceId == myDeviceId
                        if (isForMe) {
                            val currentOutgoing = _outgoingRequest.value
                            val peerDevId = msg.deviceId ?: currentOutgoing?.senderDeviceId ?: ""
                            val peerPubKey = msg.publicKeyBase64 ?: currentOutgoing?.senderPublicKey ?: ""
                            val peerName = msg.username ?: currentOutgoing?.receiverUsername ?: "Partner"

                            if (peerDevId.isNotBlank() && peerPubKey.isNotBlank()) {
                                pairingRepository.establishMutualPairing(
                                    peerDeviceId = peerDevId,
                                    peerUsername = peerName,
                                    peerPublicKeyBase64 = peerPubKey,
                                    privateNicknameForPartner = peerName,
                                    myInternalGoogleUserId = authManager.getInternalUserId() ?: ""
                                )

                                saveOutgoingRequest(null)
                                _pairingSuccessEvent.tryEmit(Unit)
                            }
                        }
                    }

                    "PAIR_CONFIRM" -> {
                        val isForMe = msg.recipientDeviceId == myDeviceId
                        if (isForMe) {
                            val peerDevId = msg.deviceId ?: ""
                            val peerPubKey = msg.publicKeyBase64 ?: ""
                            val peerName = msg.username ?: "Partner"

                            if (peerDevId.isNotBlank() && peerPubKey.isNotBlank()) {
                                pairingRepository.establishMutualPairing(
                                    peerDeviceId = peerDevId,
                                    peerUsername = peerName,
                                    peerPublicKeyBase64 = peerPubKey,
                                    privateNicknameForPartner = peerName,
                                    myInternalGoogleUserId = authManager.getInternalUserId() ?: ""
                                )
                                _pairingSuccessEvent.tryEmit(Unit)
                            }
                        }
                    }

                    "PAIR_REJECT" -> {
                        val target = msg.targetUsername?.removePrefix("@")?.trim()?.lowercase() ?: ""
                        val isForMe = target == myUsername || msg.recipientDeviceId == myDeviceId
                        if (isForMe) {
                            saveOutgoingRequest(null)
                        }
                    }

                    "PAIR_CANCEL" -> {
                        val target = msg.targetUsername?.removePrefix("@")?.trim()?.lowercase() ?: ""
                        val isForMe = target == myUsername || msg.recipientDeviceId == myDeviceId
                        if (isForMe) {
                            val senderDev = msg.deviceId
                            _incomingRequests.value = _incomingRequests.value.filter { it.senderDeviceId != senderDev }
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if a username is available in Supabase profiles.
     */
    suspend fun checkUsernameAvailability(username: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val clean = username.trim().lowercase()
        if (!clean.matches(Regex("^[a-zA-Z0-9_]{3,20}$"))) {
            return@withContext Result.failure(IllegalArgumentException("Username must be 3-20 characters long and contain only letters, numbers, and underscores."))
        }

        try {
            val url = "${SignalingClient.DEFAULT_SUPABASE_URL}/rest/v1/duolink_profiles?username=eq.$clean&select=username"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SignalingClient.DEFAULT_SUPABASE_KEY)
                .header("Authorization", "Bearer ${SignalingClient.DEFAULT_SUPABASE_KEY}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val json = jsonSerializer.parseToJsonElement(body).jsonArray
                val isAvailable = json.isEmpty()
                Result.success(isAvailable)
            } else if (response.code == 404) {
                // Table not created yet in Supabase - check local memory cache
                val existsInCache = cachedDiscoveryUsers.containsKey(clean)
                Result.success(!existsInCache)
            } else {
                Result.failure(Exception("Supabase check error: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Username availability check network exception: ${e.message}")
            // Offline fallback
            val existsInCache = cachedDiscoveryUsers.containsKey(clean)
            Result.success(!existsInCache)
        }
    }

    /**
     * Saves username permanently to local storage and Supabase database.
     */
    suspend fun registerUsername(username: String): Result<Unit> = withContext(Dispatchers.IO) {
        val clean = username.trim().lowercase()
        val userId = authManager.getInternalUserId() ?: UUID.randomUUID().toString()
        val deviceId = cryptoManager.getDeviceId()
        val pubKey = cryptoManager.getLocalPublicKeyBase64()

        authManager.setUsername(clean)

        try {
            val payload = buildJsonObject {
                put("user_id", userId)
                put("username", clean)
                put("device_id", deviceId)
                put("public_key", pubKey)
            }

            val request = Request.Builder()
                .url("${SignalingClient.DEFAULT_SUPABASE_URL}/rest/v1/duolink_profiles")
                .header("apikey", SignalingClient.DEFAULT_SUPABASE_KEY)
                .header("Authorization", "Bearer ${SignalingClient.DEFAULT_SUPABASE_KEY}")
                .header("Prefer", "resolution=merge-duplicates")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            Log.w(TAG, "Error posting profile to Supabase: ${e.message}")
        }

        // Broadcast presence over Realtime
        signalingClient.connect(SignalingClient.DEFAULT_SUPABASE_URL, deviceId)
        signalingClient.send(
            SignalingMessage(
                type = "USER_PRESENCE",
                deviceId = deviceId,
                googleUserId = userId,
                username = clean,
                publicKeyBase64 = pubKey
            )
        )

        Result.success(Unit)
    }

    /**
     * Searches for partner by username in Supabase and active Realtime peers.
     */
    suspend fun searchPartner(query: String): Result<List<PartnerSearchResult>> = withContext(Dispatchers.IO) {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return@withContext Result.success(emptyList())

        val myUsername = authManager.getUsername()?.lowercase() ?: ""
        val myDeviceId = cryptoManager.getDeviceId()
        val results = mutableListOf<PartnerSearchResult>()

        // 1. Broadcast real-time query over WebSocket channel so active peers reply immediately
        broadcastQuery(clean)
        broadcastPresence()

        // Allow rapid WebSocket echo to arrive
        kotlinx.coroutines.delay(200)

        // 2. Query Supabase REST if table exists
        try {
            val url = "${SignalingClient.DEFAULT_SUPABASE_URL}/rest/v1/duolink_profiles?username=ilike.$clean%25&select=user_id,username,device_id,public_key&limit=8"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SignalingClient.DEFAULT_SUPABASE_KEY)
                .header("Authorization", "Bearer ${SignalingClient.DEFAULT_SUPABASE_KEY}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: "[]"
                val jsonArr = jsonSerializer.parseToJsonElement(body).jsonArray
                jsonArr.forEach { elem ->
                    val obj = elem.jsonObject
                    val uName = obj["username"]?.jsonPrimitive?.content ?: ""
                    val devId = obj["device_id"]?.jsonPrimitive?.content ?: ""
                    val uId = obj["user_id"]?.jsonPrimitive?.content ?: devId
                    val pubKey = obj["public_key"]?.jsonPrimitive?.content ?: ""

                    if (uName != myUsername && devId != myDeviceId && pubKey.isNotBlank()) {
                        results.add(PartnerSearchResult(uId, uName, devId, pubKey))
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Search query REST exception: ${e.message}")
        }

        // 3. Search in Realtime cache for any discovered peers
        cachedDiscoveryUsers.values.forEach { cached ->
            if (cached.username.contains(clean) && cached.username != myUsername && cached.deviceId != myDeviceId) {
                if (results.none { it.username == cached.username }) {
                    results.add(cached)
                }
            }
        }

        Result.success(results)
    }

    /**
     * Sends a pairing request to the selected partner.
     */
    suspend fun sendPairingRequest(target: PartnerSearchResult, partnerCustomNickname: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (pairingRepository.isPaired()) {
            return@withContext Result.failure(IllegalStateException("Already paired with a partner. Unpair first to link another device."))
        }

        val myUsername = authManager.getUsername() ?: "User"
        val myUserId = authManager.getInternalUserId() ?: cryptoManager.getDeviceId()
        val myDeviceId = cryptoManager.getDeviceId()
        val myPubKey = cryptoManager.getLocalPublicKeyBase64()
        val reqId = UUID.randomUUID().toString()

        val req = PairingRequest(
            id = reqId,
            senderUserId = myUserId,
            senderUsername = myUsername,
            senderDeviceId = myDeviceId,
            senderPublicKey = myPubKey,
            receiverUsername = target.username,
            status = "PENDING"
        )
        saveOutgoingRequest(req)

        // Broadcast over Supabase Realtime
        if (signalingClient.state.value != com.duolink.app.data.signaling.SignalingState.REGISTERED &&
            signalingClient.state.value != com.duolink.app.data.signaling.SignalingState.CONNECTED) {
            signalingClient.connect(SignalingClient.DEFAULT_SUPABASE_URL, myDeviceId)
            kotlinx.coroutines.delay(300)
        }
        signalingClient.send(
            SignalingMessage(
                type = "PAIR_REQUEST",
                deviceId = myDeviceId,
                recipientDeviceId = target.deviceId,
                googleUserId = myUserId,
                username = myUsername,
                targetUsername = target.username,
                publicKeyBase64 = myPubKey,
                token = reqId
            )
        )

        // Also post to Supabase database if table exists
        try {
            val payload = buildJsonObject {
                put("id", reqId)
                put("sender_user_id", myUserId)
                put("sender_username", myUsername)
                put("sender_device_id", myDeviceId)
                put("sender_public_key", myPubKey)
                put("receiver_username", target.username)
                put("status", "PENDING")
            }

            val dbReq = Request.Builder()
                .url("${SignalingClient.DEFAULT_SUPABASE_URL}/rest/v1/duolink_pairing_requests")
                .header("apikey", SignalingClient.DEFAULT_SUPABASE_KEY)
                .header("Authorization", "Bearer ${SignalingClient.DEFAULT_SUPABASE_KEY}")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(dbReq).execute()
        } catch (e: Exception) {
            Log.w(TAG, "Notice: pairing request recorded via Realtime channel: ${e.message}")
        }

        Result.success(Unit)
    }

    /**
     * Accepts an incoming pairing request.
     */
    suspend fun acceptPairingRequest(request: PairingRequest, partnerCustomNickname: String): Result<PairingEntity> = withContext(Dispatchers.IO) {
        val myUsername = authManager.getUsername() ?: "User"
        val myUserId = authManager.getInternalUserId() ?: cryptoManager.getDeviceId()
        val myDeviceId = cryptoManager.getDeviceId()
        val myPubKey = cryptoManager.getLocalPublicKeyBase64()

        val result = pairingRepository.establishMutualPairing(
            peerDeviceId = request.senderDeviceId,
            peerUsername = request.senderUsername,
            peerPublicKeyBase64 = request.senderPublicKey,
            privateNicknameForPartner = partnerCustomNickname.ifBlank { request.senderUsername },
            myInternalGoogleUserId = myUserId
        )

        if (result.isSuccess) {
            // Broadcast acceptance to sender
            if (signalingClient.state.value != com.duolink.app.data.signaling.SignalingState.REGISTERED &&
                signalingClient.state.value != com.duolink.app.data.signaling.SignalingState.CONNECTED) {
                signalingClient.connect(SignalingClient.DEFAULT_SUPABASE_URL, myDeviceId)
                kotlinx.coroutines.delay(300)
            }
            signalingClient.send(
                SignalingMessage(
                    type = "PAIR_ACCEPT",
                    deviceId = myDeviceId,
                    recipientDeviceId = request.senderDeviceId,
                    googleUserId = myUserId,
                    username = myUsername,
                    targetUsername = request.senderUsername,
                    publicKeyBase64 = myPubKey,
                    token = request.id
                )
            )

            // Remove from incoming requests
            _incomingRequests.value = _incomingRequests.value.filter { it.id != request.id }
            _pairingSuccessEvent.tryEmit(Unit)
        }

        result
    }

    /**
     * Rejects an incoming pairing request.
     */
    suspend fun rejectPairingRequest(request: PairingRequest) = withContext(Dispatchers.IO) {
        val myUsername = authManager.getUsername() ?: "User"
        val myDeviceId = cryptoManager.getDeviceId()

        signalingClient.send(
            SignalingMessage(
                type = "PAIR_REJECT",
                deviceId = myDeviceId,
                recipientDeviceId = request.senderDeviceId,
                username = myUsername,
                targetUsername = request.senderUsername,
                token = request.id
            )
        )

        _incomingRequests.value = _incomingRequests.value.filter { it.id != request.id }
    }

    /**
     * Cancels an active outgoing pairing request.
     */
    suspend fun cancelOutgoingRequest() = withContext(Dispatchers.IO) {
        val current = _outgoingRequest.value ?: return@withContext
        val myUsername = authManager.getUsername() ?: "User"
        val myDeviceId = cryptoManager.getDeviceId()

        signalingClient.send(
            SignalingMessage(
                type = "PAIR_CANCEL",
                deviceId = myDeviceId,
                recipientDeviceId = current.senderDeviceId,
                username = myUsername,
                targetUsername = current.receiverUsername,
                token = current.id
            )
        )

        saveOutgoingRequest(null)
    }
}

