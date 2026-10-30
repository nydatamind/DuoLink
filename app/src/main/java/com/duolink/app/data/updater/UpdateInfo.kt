package com.duolink.app.data.updater

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val sha256Checksum: String? = null,
    val publishedAt: String = ""
)

object VersionComparator {

    /**
     * Compares installed semantic version against latest release version.
     * e.g. "1.0.1" > "1.0.0" -> true
     * "v1.1.0" > "1.0.0" -> true
     * "1.0.0" > "1.0.0" -> false
     */
    fun isUpdateAvailable(installedVersion: String, latestVersion: String): Boolean {
        val cleanInstalled = cleanVersion(installedVersion)
        val cleanLatest = cleanVersion(latestVersion)

        val installedParts = cleanInstalled.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = cleanLatest.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(installedParts.size, latestParts.size)

        for (i in 0 until maxLen) {
            val inst = installedParts.getOrElse(i) { 0 }
            val lat = latestParts.getOrElse(i) { 0 }
            if (lat > inst) return true
            if (lat < inst) return false
        }
        return false
    }

    private fun cleanVersion(version: String): String {
        return version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-") // strip beta/alpha suffix if comparing base
    }
}

