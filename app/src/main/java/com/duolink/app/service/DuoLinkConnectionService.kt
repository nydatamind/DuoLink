package com.duolink.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

class DuoLinkConnectionService : Service() {

    companion object {
        private const val TAG = "DuoLinkConnService"
    }

    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notificationHelper.buildSyncNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                }
                startForeground(NotificationHelper.SYNC_NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NotificationHelper.SYNC_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting connection service: ${e.message}")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

