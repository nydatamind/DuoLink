package com.duolink.app.data.updater

import android.content.Context
import android.util.Log
import com.duolink.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class UpdateChecker(private val context: Context) {

    companion object {
        private const val TAG = "UpdateChecker"
        const val GITHUB_REPO_OWNER = "nydatamind"
        const val GITHUB_REPO_NAME = "DuoLink"
        const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/$GITHUB_REPO_OWNER/$GITHUB_REPO_NAME/releases/latest"

        private const val PREFS_NAME = "duolink_updates_prefs"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Checks if user previously tapped "Later" for this specific version.
     */
    fun isVersionDismissed(version: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DISMISSED_VERSION, "") == version
    }

    fun dismissVersion(version: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DISMISSED_VERSION, version).apply()
    }

    fun clearDismissedVersion() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_DISMISSED_VERSION).apply()
    }

    /**
     * Queries GitHub Releases API asynchronously.
     * Returns UpdateInfo if a newer version exists, null if already on latest or offline.
     */
    suspend fun checkForUpdate(ignoreDismissed: Boolean = false): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "DuoLink-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                if (response.code == 404) {
                    Log.d(TAG, "No releases published yet on GitHub repository.")
                    return@withContext Result.success(null)
                }
                return@withContext Result.failure(Exception("GitHub API error: ${response.code} ${response.message}"))
            }

            val bodyString = response.body?.string() ?: return@withContext Result.success(null)
            val releaseJson = json.parseToJsonElement(bodyString).jsonObject

            val tagName = releaseJson["tag_name"]?.jsonPrimitive?.content ?: ""
            val releaseTitle = releaseJson["name"]?.jsonPrimitive?.content ?: "DuoLink $tagName"
            val changelog = releaseJson["body"]?.jsonPrimitive?.content ?: "Bug fixes and performance improvements."
            val publishedAt = releaseJson["published_at"]?.jsonPrimitive?.content ?: ""

            // Check if update is newer than currently installed version
            val isNewer = VersionComparator.isUpdateAvailable(BuildConfig.VERSION_NAME, tagName)
            if (!isNewer) {
                Log.d(TAG, "Already up to date. Installed: ${BuildConfig.VERSION_NAME}, Latest: $tagName")
                return@withContext Result.success(null)
            }

            if (!ignoreDismissed && isVersionDismissed(tagName)) {
                Log.d(TAG, "Update $tagName was previously dismissed by user.")
                return@withContext Result.success(null)
            }

            // Find APK asset in assets array
            val assetsArray = releaseJson["assets"]?.jsonArray
            var apkUrl: String? = null
            var apkSize = 0L

            assetsArray?.forEach { assetElem ->
                val assetObj = assetElem.jsonObject
                val name = assetObj["name"]?.jsonPrimitive?.content ?: ""
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = assetObj["browser_download_url"]?.jsonPrimitive?.content
                    apkSize = assetObj["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                }
            }

            if (apkUrl == null) {
                Log.w(TAG, "Release $tagName exists but no APK asset found.")
                return@withContext Result.success(null)
            }

            // Attempt to parse SHA-256 checksum from release notes if provided (e.g. SHA256: [0-9a-fA-F]{64})
            val sha256Pattern = Pattern.compile("([a-fA-F0-9]{64})")
            val matcher = sha256Pattern.matcher(changelog)
            val sha256Checksum = if (matcher.find()) matcher.group(1) else null

            val updateInfo = UpdateInfo(
                versionName = tagName,
                releaseTitle = releaseTitle,
                changelog = changelog,
                apkDownloadUrl = apkUrl,
                apkSizeBytes = apkSize,
                sha256Checksum = sha256Checksum,
                publishedAt = publishedAt
            )

            Result.success(updateInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update: ${e.message}")
            Result.failure(e)
        }
    }
}
