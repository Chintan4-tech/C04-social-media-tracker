package com.ideasi.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.app.usage.UsageStatsManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

class AppAccessibilityService : AccessibilityService() {

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val checkMutex = Mutex()

    private var currentPackage: String? = null
    private var challengeLaunching = false
    private var checking = false

    private var foregroundStartTime = 0L
    private var foregroundStartUsageMillis = 0L

    private val checkRunnable = object : Runnable {
        override fun run() {
            val target = currentPackage

            if (target != null && !challengeLaunching && !checking) {
                checking = true

                serviceScope.launch {
                    try {
                        checkMutex.withLock {
                            checkUsageLimit(target)
                        }
                    } finally {
                        checking = false
                    }
                }
            }

            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        mainHandler.removeCallbacks(checkRunnable)
        mainHandler.post(checkRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val target = event.packageName?.toString() ?: return

        if (target == packageName) {
            currentPackage = null
            return
        }

        if (target != currentPackage) {
            currentPackage = target
            foregroundStartTime = SystemClock.elapsedRealtime()
            foregroundStartUsageMillis = getUsageMillis(target)

            // The user has returned to the selected app.
            // Allow a new challenge if its limit is reached again.
            challengeLaunching = false
        }
    }

    private suspend fun checkUsageLimit(targetPackage: String) {
        val db = AppDatabase.getDatabase(applicationContext)

        val apps = db.selectedAppDao()
            .getAllSelectedApps()
            .first()

        if (currentPackage != targetPackage) return

        val selectedApp = apps.find {
            it.packageName == targetPackage
        } ?: return

        val currentUsageMillis = getUsageMillis(targetPackage)

        // Add time elapsed since this app became foreground.
        // This helps when Android's usage statistics update slowly.
        val elapsedMillis =
            (SystemClock.elapsedRealtime() - foregroundStartTime)
                .coerceAtLeast(0L)

        val estimatedUsageMillis = maxOf(
            currentUsageMillis,
            foregroundStartUsageMillis + elapsedMillis
        )

        val limitMillis = selectedApp.limitMinutes * 60_000L

        if (estimatedUsageMillis < limitMillis) return

        val usedMinutes = (estimatedUsageMillis / 60_000L).toInt()

        val passedUsageMinutes =
            getChallengePassedUsageMinutes(
                applicationContext,
                targetPackage
            )

        if (usedMinutes <= passedUsageMinutes) return

        if (currentPackage != targetPackage) return

        challengeLaunching = true

        setAppOverLimit(
            applicationContext,
            targetPackage,
            true
        )

        launchChallenge(targetPackage)
    }

    private fun getUsageMillis(targetPackage: String): Long {
        val manager = getSystemService(
            Context.USAGE_STATS_SERVICE
        ) as UsageStatsManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val stats = manager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        return stats
            .firstOrNull { it.packageName == targetPackage }
            ?.totalTimeInForeground
            ?: 0L
    }

    private fun launchChallenge(targetPackage: String) {
        mainHandler.post {
            val intent = Intent(
                this,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

                putExtra("show_math_challenge", true)
                putExtra("blocked_package", targetPackage)
            }

            try {
                startActivity(intent)
            } catch (e: Exception) {
                challengeLaunching = false
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        mainHandler.removeCallbacks(checkRunnable)
        serviceScope.cancel()
        super.onDestroy()
    }
}
