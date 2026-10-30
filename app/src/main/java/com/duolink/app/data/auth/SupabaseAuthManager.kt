package com.duolink.app.data.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.duolink.app.data.signaling.SignalingClient
import com.duolink.app.data.signaling.jsonSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

@Serializable
data class UserSession(
    val userId: String,
    val email: String, // Kept STRICTLY INTERNAL, never sent to peer
    val accessToken: String,
    val refreshToken: String,
    val authenticatedAt: Long
)

class SupabaseAuthManager(private val context: Context) {

    companion object {
        private const val TAG = "SupabaseAuthManager"
        private const val PREFS_NAME = "duolink_auth_prefs"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_INTERNAL_EMAIL = "auth_internal_email"
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_AUTH_TIME = "auth_time"

        const val REDIRECT_URI = "duolink://auth/callback"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient()

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    init {
        loadStoredSession()
    }

    private fun loadStoredSession() {
        val uid = prefs.getString(KEY_USER_ID, null)
        val email = prefs.getString(KEY_INTERNAL_EMAIL, null)
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refresh = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        val time = prefs.getLong(KEY_AUTH_TIME, 0L)

        if (uid != null && email != null && token != null) {
            _currentSession.value = UserSession(
                userId = uid,
                email = email,
                accessToken = token,
                refreshToken = refresh,
                authenticatedAt = time
            )
        }
    }

    fun isAuthenticated(): Boolean = _currentSession.value != null

    fun getInternalUserId(): String? = _currentSession.value?.userId

    /**
     * Warning: Internal only. Never expose to peer or UI screens.
     */
    fun getInternalEmail(): String? = _currentSession.value?.email

    /**
     * Initiates Google OAuth using Supabase Auth with custom tabs redirect.
     */
    fun startGoogleOAuth(context: Context) {
        val authUrl = "${SignalingClient.DEFAULT_SUPABASE_URL}/auth/v1/authorize?provider=google&redirect_to=$REDIRECT_URI"
        try {
            val customTabsIntent = CustomTabsIntent.Builder().build()
            customTabsIntent.launchUrl(context, Uri.parse(authUrl))
        } catch (e: Exception) {
            // Fallback to standard browser intent
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
        }
    }

    /**
     * Handles the OAuth callback redirect: duolink://auth/callback#access_token=...
     */
    suspend fun handleOAuthCallback(uri: Uri): Result<UserSession> {
        return withContext(Dispatchers.IO) {
            try {
                // Fragment: access_token=...&refresh_token=...
                val fragment = uri.fragment ?: uri.query ?: ""
                val params = fragment.split("&").associate {
                    val parts = it.split("=")
                    if (parts.size == 2) parts[0] to parts[1] else "" to ""
                }

                val accessToken = params["access_token"]
                val refreshToken = params["refresh_token"] ?: ""

                if (accessToken.isNullOrEmpty()) {
                    return@withContext Result.failure(IllegalArgumentException("No access token found in callback"))
                }

                // Verify user with Supabase Auth endpoint
                val userReq = Request.Builder()
                    .url("${SignalingClient.DEFAULT_SUPABASE_URL}/auth/v1/user")
                    .header("apikey", SignalingClient.DEFAULT_SUPABASE_KEY)
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                val resp = httpClient.newCall(userReq).execute()
                val respBody = resp.body?.string() ?: ""

                if (!resp.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Failed to verify user: $respBody"))
                }

                val userObj = jsonSerializer.parseToJsonElement(respBody).jsonObject
                val userId = userObj["id"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString()
                val email = userObj["email"]?.jsonPrimitive?.content ?: "user@gmail.com"

                val session = UserSession(
                    userId = userId,
                    email = email,
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    authenticatedAt = System.currentTimeMillis()
                )

                saveSession(session)
                Result.success(session)
            } catch (e: Exception) {
                Log.e(TAG, "OAuth callback handling failed: ${e.message}")
                Result.failure(e)
            }
        }
    }

    /**
     * Instant Google Sign-In for testing on two phones or emulators.
     * Generates or restores an authenticated Google identity.
     */
    fun signInWithGoogleAccount(googleEmail: String): UserSession {
        val existingUid = prefs.getString(KEY_USER_ID, null)
        val userId = existingUid ?: UUID.nameUUIDFromBytes(googleEmail.toByteArray()).toString()
        val session = UserSession(
            userId = userId,
            email = googleEmail,
            accessToken = "token_google_${UUID.randomUUID()}",
            refreshToken = "refresh_${UUID.randomUUID()}",
            authenticatedAt = System.currentTimeMillis()
        )
        saveSession(session)
        return session
    }

    private fun saveSession(session: UserSession) {
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_INTERNAL_EMAIL, session.email)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putLong(KEY_AUTH_TIME, session.authenticatedAt)
            .apply()

        _currentSession.value = session
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _currentSession.value = null
    }
}

