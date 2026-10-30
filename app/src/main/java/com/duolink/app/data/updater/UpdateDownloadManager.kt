package com.duolink.app.data.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class DownloadProgress(
    val bytesRead: Long = 0L,
    val totalBytes: Long = 0L,
    val percentage: Int = 0,
    val isCompleted: Boolean = false,
    val isVerifying: Boolean = false,
    val error: String? = null
)

class UpdateDownloadManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateDownloadManager"
        private const val UPDATE_FILE_NAME = "DuoLink_update.apk"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _downloadProgress = MutableStateFlow(DownloadProgress())
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private var activeCall: okhttp3.Call? = null

    /**
     * Downloads APK from GitHub release, computes SHA-256 and verifies signature.
     */
    suspend fun downloadAndVerifyApk(updateInfo: UpdateInfo): Result<File> = withContext(Dispatchers.IO) {
        _downloadProgress.value = DownloadProgress(totalBytes = updateInfo.apkSizeBytes)

        val destinationDir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
        val destinationFile = File(destinationDir, UPDATE_FILE_NAME)
        if (destinationFile.exists()) destinationFile.delete()

        try {
            val request = Request.Builder()
                .url(updateInfo.apkDownloadUrl)
                .build()

            val call = httpClient.newCall(request)
            activeCall = call
            val response = call.execute()

            if (!response.isSuccessful) {
                val errorMsg = "Download failed: HTTP ${response.code} ${response.message}"
                _downloadProgress.value = DownloadProgress(error = errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val contentLength = if (updateInfo.apkSizeBytes > 0) updateInfo.apkSizeBytes else body.contentLength()

            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(destinationFile)
            val buffer = ByteArray(8192)
            var bytesReadTotal = 0L
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                bytesReadTotal += read
                val percentage = if (contentLength > 0) {
                    ((bytesReadTotal * 100) / contentLength).toInt().coerceIn(0, 100)
                } else 0

                _downloadProgress.value = DownloadProgress(
                    bytesRead = bytesReadTotal,
                    totalBytes = contentLength,
                    percentage = percentage
                )
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // 1. Verify SHA-256 Checksum if present
            _downloadProgress.value = _downloadProgress.value.copy(isVerifying = true)
            val calculatedSha256 = calculateFileSha256(destinationFile)
            Log.d(TAG, "Downloaded APK SHA-256: $calculatedSha256")

            if (!updateInfo.sha256Checksum.isNullOrBlank()) {
                val expectedSha = updateInfo.sha256Checksum.trim().lowercase()
                if (calculatedSha256.lowercase() != expectedSha) {
                    destinationFile.delete()
                    val errorMsg = "Checksum verification failed! Expected: $expectedSha, Found: $calculatedSha256"
                    _downloadProgress.value = DownloadProgress(error = errorMsg)
                    return@withContext Result.failure(SecurityException(errorMsg))
                }
            }

            // 2. Verify Signing Certificate matches currently installed app
            val isCertValid = verifyApkSignatureMatch(destinationFile)
            if (!isCertValid) {
                destinationFile.delete()
                val errorMsg = "Signing certificate mismatch! The update was signed with a different key."
                _downloadProgress.value = DownloadProgress(error = errorMsg)
                return@withContext Result.failure(SecurityException(errorMsg))
            }

            _downloadProgress.value = DownloadProgress(
                bytesRead = bytesReadTotal,
                totalBytes = contentLength,
                percentage = 100,
                isCompleted = true
            )

            Result.success(destinationFile)
        } catch (e: Exception) {
            destinationFile.delete()
            Log.e(TAG, "Download error: ${e.message}")
            _downloadProgress.value = DownloadProgress(error = e.message ?: "Download failed")
            Result.failure(e)
        } finally {
            activeCall = null
        }
    }

    fun cancelDownload() {
        activeCall?.cancel()
        _downloadProgress.value = DownloadProgress(error = "Download cancelled")
    }

    /**
     * Checks if Unknown App Sources permission is granted.
     */
    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    /**
     * Launches Android's official package installer via secure FileProvider URI.
     */
    fun launchPackageInstaller(apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(installIntent)
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Calculates SHA-256 hash of a file.
     */
    fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies that the downloaded APK's signing certificate matches currently installed app.
     */
    @Suppress("DEPRECATION")
    private fun verifyApkSignatureMatch(apkFile: File): Boolean {
        try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_SIGNATURES
            }

            val installedPackageInfo = pm.getPackageInfo(context.packageName, flags)
            val archivePackageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false

            val installedSignatures = getSignatures(installedPackageInfo)
            val archiveSignatures = getSignatures(archivePackageInfo)

            if (installedSignatures.isEmpty() || archiveSignatures.isEmpty()) return false

            // Compare certificate byte arrays
            return installedSignatures.any { inst ->
                archiveSignatures.any { arch -> inst.contentEquals(arch) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification exception: ${e.message}")
            return false
        }
    }

    @Suppress("DEPRECATION")
    private fun getSignatures(packageInfo: PackageInfo): List<ByteArray> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray() }
                ?: packageInfo.signingInfo?.signingCertificateHistory?.map { it.toByteArray() }
                ?: emptyList()
        } else {
            packageInfo.signatures?.map { it.toByteArray() } ?: emptyList()
        }
    }
}

