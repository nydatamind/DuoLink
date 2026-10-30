package com.duolink.app.ui.updater

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duolink.app.BuildConfig
import com.duolink.app.data.updater.UpdateChecker
import com.duolink.app.data.updater.UpdateDownloadManager
import com.duolink.app.data.updater.UpdateInfo
import com.duolink.app.ui.mascot.OwlEyeState
import com.duolink.app.ui.mascot.OwlMascotView
import com.duolink.app.ui.theme.DuoLinkCharcoalBg
import com.duolink.app.ui.theme.DuoLinkRedPrimary
import com.duolink.app.ui.theme.DuoLinkSurface
import com.duolink.app.ui.theme.DuoLinkTextMuted
import com.duolink.app.ui.theme.DuoLinkTextWhite
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val downloadManager = remember { UpdateDownloadManager(context) }
    val updateChecker = remember { UpdateChecker(context) }
    val progress by downloadManager.downloadProgress.collectAsState()

    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var isDownloading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isDownloading) {
                updateChecker.dismissVersion(updateInfo.versionName)
                onDismiss()
            }
        },
        containerColor = DuoLinkSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OwlMascotView(state = OwlEyeState.OPEN, size = 42.dp)
                Column {
                    Text(
                        text = "Update Available",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DuoLinkTextWhite
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} to ${updateInfo.versionName}",
                        fontSize = 12.sp,
                        color = DuoLinkRedPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (updateInfo.apkSizeBytes > 0) {
                    val mb = updateInfo.apkSizeBytes / (1024 * 1024.0)
                    Text(
                        text = "Download Size: %.1f MB".format(mb),
                        fontSize = 12.sp,
                        color = DuoLinkTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = "Changes in ${updateInfo.versionName}:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DuoLinkTextWhite
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DuoLinkCharcoalBg)
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = updateInfo.changelog.ifBlank { "Performance improvements, enhanced security, and bug fixes." },
                        fontSize = 12.sp,
                        color = DuoLinkTextMuted,
                        lineHeight = 16.sp
                    )
                }

                if (isDownloading) {
                    Spacer(modifier = Modifier.height(16.dp))

                    if (progress.isVerifying) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = DuoLinkRedPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verifying SHA-256 & Signature...",
                                fontSize = 12.sp,
                                color = DuoLinkRedPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        LinearProgressIndicator(
                            progress = { progress.percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = DuoLinkRedPrimary,
                            trackColor = DuoLinkCharcoalBg,
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val readMb = progress.bytesRead / (1024 * 1024.0)
                        val totalMb = progress.totalBytes / (1024 * 1024.0)
                        Text(
                            text = "${progress.percentage}% • %.1f / %.1f MB".format(readMb, totalMb),
                            fontSize = 11.sp,
                            color = DuoLinkTextMuted
                        )
                    }
                }

                if (progress.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Error: ${progress.error}",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }
            }
        },
        confirmButton = {
            if (downloadedFile != null) {
                Button(
                    onClick = {
                        if (!downloadManager.canInstallPackages()) {
                            Toast.makeText(context, "Please allow DuoLink to install unknown apps", Toast.LENGTH_LONG).show()
                            downloadManager.openInstallPermissionSettings()
                        } else {
                            downloadManager.launchPackageInstaller(downloadedFile!!)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Install")
                }
            } else {
                Button(
                    onClick = {
                        if (!isDownloading) {
                            isDownloading = true
                            scope.launch {
                                val result = downloadManager.downloadAndVerifyApk(updateInfo)
                                if (result.isSuccess) {
                                    downloadedFile = result.getOrNull()
                                    if (!downloadManager.canInstallPackages()) {
                                        Toast.makeText(context, "Please enable permission to install updates", Toast.LENGTH_LONG).show()
                                        downloadManager.openInstallPermissionSettings()
                                    } else {
                                        downloadManager.launchPackageInstaller(downloadedFile!!)
                                    }
                                } else {
                                    isDownloading = false
                                }
                            }
                        }
                    },
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = DuoLinkRedPrimary)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isDownloading) "Downloading..." else "Download")
                }
            }
        },
        dismissButton = {
            if (!isDownloading) {
                TextButton(
                    onClick = {
                        updateChecker.dismissVersion(updateInfo.versionName)
                        onDismiss()
                    }
                ) {
                    Text("Later", color = DuoLinkTextMuted)
                }
            } else {
                TextButton(
                    onClick = {
                        downloadManager.cancelDownload()
                        isDownloading = false
                    }
                ) {
                    Text("Cancel", color = Color.Red)
                }
            }
        }
    )
}

