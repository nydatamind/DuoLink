package com.duolink.app.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

class DuoLinkCallService : Service() {

    companion object {
        private const val TAG = "DuoLinkCallService"
        const val ACTION_START_CALL = "com.duolink.app.ACTION_START_CALL"
        const val ACTION_STOP_CALL = "com.duolink.app.ACTION_STOP_CALL"
        const val EXTRA_CALL_TITLE = "extra_call_title"
    }

    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_CALL) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_CALL_TITLE) ?: "Active Call"
        val notification = notificationHelper.buildCallNotification(title, isIncoming = false)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                }
                startForeground(NotificationHelper.CALL_NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NotificationHelper.CALL_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting call foreground service: ${e.message}")
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

