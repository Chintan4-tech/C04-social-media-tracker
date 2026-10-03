package com.ideasi.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.*

class LimitCheckService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification("Monitoring app usage..."))

        serviceScope.launch {
            while (true) {
                val results = checkAllLimits(applicationContext)
                val overLimitApps = results.filter { it.isOverLimit }

                if (overLimitApps.isNotEmpty()) {
                    val names = overLimitApps.joinToString(", ") { it.app.appName }
                    showAlertNotification("Time limit reached: $names")
                }

                delay(30_000) // wait 30 seconds before checking again
            }
        }

        return START_STICKY
    }

    private fun buildNotification(text: String): Notification {
        val channelId = "ideas_i_tracking"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Ideas I Tracking", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        return Notification.Builder(this, channelId)
            .setContentTitle("Ideas I")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
    }

    private fun showAlertNotification(text: String) {
        val channelId = "ideas_i_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Ideas I Alerts", NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("Time limit reached!")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
