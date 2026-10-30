package com.duolink.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.duolink.app.MainActivity
import com.duolink.app.R

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val SILENT_CALL_CHANNEL_ID = "duolink_channel_silent_call"
        const val SILENT_SYNC_CHANNEL_ID = "duolink_channel_silent_sync"

        const val CALL_NOTIFICATION_ID = 2001
        const val SYNC_NOTIFICATION_ID = 2002
    }

    init {
        createSilentChannels()
    }

    private fun createSilentChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Completely silent channel: No sound, no banner, no vibration from NotificationManager
            val silentCallChannel = NotificationChannel(
                SILENT_CALL_CHANNEL_ID,
                "DuoLink Active Link",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hardware background sync"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            val silentSyncChannel = NotificationChannel(
                SILENT_SYNC_CHANNEL_ID,
                "DuoLink Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Silent background connectivity"
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }

            notificationManager.createNotificationChannel(silentCallChannel)
            notificationManager.createNotificationChannel(silentSyncChannel)
        }
    }

    fun buildCallNotification(title: String, isIncoming: Boolean): Notification {
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SILENT_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("DuoLink")
            .setContentText("Active call in progress")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSound(null)
            .setVibrate(null)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .build()
    }

    fun buildSyncNotification(): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, SILENT_SYNC_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("DuoLink")
            .setContentText("Private link active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSound(null)
            .setVibrate(null)
            .setContentIntent(pendingIntent)
            .build()
    }
}
