package com.ideasi.app

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
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

companion object {
    private const val CHECK_INTERVAL_MS = 1_000L
    private const val CHALLENGE_COOLDOWN_MS = 3 * 60 * 1_000L
    private const val PREFS_NAME = "ScrollGuardPrefs"
    private const val COOLDOWN_KEY_PREFIX = "challenge_cooldown_until_"
}

private val serviceScope =
    CoroutineScope(SupervisorJob() + Dispatchers.IO)

private val mainHandler = Handler(Looper.getMainLooper())
private val checkMutex = Mutex()

@Volatile
private var currentPackage: String? = null

@Volatile
private var challengeLaunching = false

@Volatile
private var checking = false

private var foregroundStartTime = 0L
private var foregroundStartUsageMillis = 0L

private val checkRunnable = object : Runnable {
    override fun run() {
        val target = currentPackage

        if (
            target != null &&
            target != packageName &&
            !challengeLaunching &&
            !checking
        ) {
            checking = true

            serviceScope.launch {
                try {
                    checkMutex.withLock {
                        checkUsageLimit(target)
                    }
                } catch (_: Exception) {
                    // Continue monitoring if a usage check fails.
                } finally {
                    checking = false
                }
            }
        }

        mainHandler.postDelayed(this, CHECK_INTERVAL_MS)
    }
}

override fun onServiceConnected() {
    super.onServiceConnected()

    mainHandler.removeCallbacks(checkRunnable)
    mainHandler.post(checkRunnable)
}

override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return

    if (
        event.eventType !=
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    ) return

    val target = event.packageName?.toString() ?: return

    // TimeShield has opened. Stop monitoring the previous app
    // while the math challenge is displayed, and release the guard.
    if (target == packageName) {
        currentPackage = null
        challengeLaunching = false
        return
    }

    if (target != currentPackage) {
        currentPackage = target
        foregroundStartTime = SystemClock.elapsedRealtime()
        foregroundStartUsageMillis = getUsageMillis(target)
    }
}

private suspend fun checkUsageLimit(targetPackage: String) {
    if (currentPackage != targetPackage) return

    val db = AppDatabase.getDatabase(applicationContext)

    val apps = db.selectedAppDao()
        .getAllSelectedApps()
        .first()

    if (currentPackage != targetPackage) return

    val selectedApp = apps.find {
        it.packageName == targetPackage
    } ?: return

    val limitMillis =
        selectedApp.limitMinutes.coerceAtLeast(1) * 60_000L

    val currentUsageMillis = getUsageMillis(targetPackage)

    val elapsedMillis =
        (SystemClock.elapsedRealtime() - foregroundStartTime)
            .coerceAtLeast(0L)

    val estimatedUsageMillis = maxOf(
        currentUsageMillis,
        foregroundStartUsageMillis + elapsedMillis
    )

    if (estimatedUsageMillis < limitMillis) return

    val usedMinutes =
        (estimatedUsageMillis / 60_000L).toInt()

    val passedUsageMinutes = getChallengePassedUsageMinutes(
        applicationContext,
        targetPackage
    )

    // Do not show the challenge again at the same recorded
    // usage level at which the user last passed it.
    if (usedMinutes <= passedUsageMinutes) return

    // Enforce the cooldown after a challenge has been passed.
    if (isInChallengeCooldown(targetPackage)) return

    if (currentPackage != targetPackage) return
    if (challengeLaunching) return

    // Set the guard before launching to prevent duplicate screens.
    challengeLaunching = true

    setAppOverLimit(
        applicationContext,
        targetPackage,
        true
    )

    launchChallenge(targetPackage)
}

private fun isInChallengeCooldown(
    targetPackage: String
): Boolean {
    val prefs = getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    val cooldownUntil = prefs.getLong(
        COOLDOWN_KEY_PREFIX + targetPackage,
        0L
    )

    return System.currentTimeMillis() < cooldownUntil
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

    // Sum all matching usage entries.
    return stats
        .filter { it.packageName == targetPackage }
        .sumOf { it.totalTimeInForeground }
}

private fun launchChallenge(targetPackage: String) {
    mainHandler.post {
        if (currentPackage != targetPackage) {
            challengeLaunching = false
            return@post
        }

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP

            putExtra("show_math_challenge", true)
            putExtra("blocked_package", targetPackage)
        }

        try {
            startActivity(intent)

            // Fallback: release the guard if the challenge
            // did not successfully take over the foreground.
            mainHandler.postDelayed({
                if (
                    currentPackage == targetPackage &&
                    !isMainActivityInForeground()
                ) {
                    challengeLaunching = false
                }
            }, 2_500L)

        } catch (_: Exception) {
            challengeLaunching = false
        }
    }
}

private fun isMainActivityInForeground(): Boolean {
    return currentPackage == packageName
}

override fun onInterrupt() = Unit

override fun onDestroy() {
    mainHandler.removeCallbacks(checkRunnable)
    serviceScope.cancel()
    super.onDestroy()
}

}
