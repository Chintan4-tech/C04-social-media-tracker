package com.ideasi.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class LimitCheckService : Service() {

    companion object {
        private const val CHANNEL_ID = "ideas_i_monitoring"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = createNotification()

        startForeground(
            NOTIFICATION_ID,
            notification
        )
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        // Stage 2 only starts the monitoring service.
        // Usage checking will be added in a later stage.

        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ideas I Monitoring",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationManager =
            getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Ideas I")
            .setContentText("Monitoring app usage")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
