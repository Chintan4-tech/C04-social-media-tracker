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
        private const val COOLDOWN_KEY_PREFIX =
            "challenge_cooldown_until_"
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
                        // Keep monitoring if a usage check fails.
                    } finally {
                        checking = false
                    }
                }
            }

            mainHandler.postDelayed(
                this,
                CHECK_INTERVAL_MS
            )
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        mainHandler.removeCallbacks(checkRunnable)
        mainHandler.post(checkRunnable)
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        if (event == null) return

        if (
            event.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            return
        }

        val target =
            event.packageName?.toString() ?: return

        // TimeShield has opened.
        // Stop checking the previous app while the challenge
        // screen is displayed.
        if (target == packageName) {
            currentPackage = null
            challengeLaunching = false
            return
        }

        // Start a new foreground tracking period when the
        // foreground app changes.
        if (target != currentPackage) {
            currentPackage = target

            foregroundStartTime =
                SystemClock.elapsedRealtime()

            foregroundStartUsageMillis =
                getUsageMillis(target)
        }
    }

    private suspend fun checkUsageLimit(
        targetPackage: String
    ) {
        if (currentPackage != targetPackage) return

        val db = AppDatabase.getDatabase(
            applicationContext
        )

        val apps = db.selectedAppDao()
            .getAllSelectedApps()
            .first()

        if (currentPackage != targetPackage) return

        val selectedApp = apps.find {
            it.packageName == targetPackage
        } ?: return

        // Do not allow a selected app to have a zero-minute limit.
        val limitMillis =
            selectedApp.limitMinutes
                .coerceAtLeast(1) * 60_000L

        // Android's usage statistics may update with a delay.
        val currentUsageMillis =
            getUsageMillis(targetPackage)

        // Estimate the usage accrued since this app became
        // the foreground app.
        val elapsedMillis = (
            SystemClock.elapsedRealtime() -
                foregroundStartTime
            ).coerceAtLeast(0L)

        val estimatedUsageMillis = maxOf(
            currentUsageMillis,
            foregroundStartUsageMillis + elapsedMillis
        )

        // Continue checking until the usage limit is reached.
        if (estimatedUsageMillis < limitMillis) return

        // A challenge has already been launched for this app.
        if (challengeLaunching) return

        // Respect the cooldown after the user solves a challenge.
        if (isInChallengeCooldown(targetPackage)) return

        // Make sure the user is still in the selected app.
        if (currentPackage != targetPackage) return

        // Set this before launching to prevent duplicate challenges.
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

    private fun getUsageMillis(
        targetPackage: String
    ): Long {
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
        ) ?: return 0L

        return stats
            .filter {
                it.packageName == targetPackage
            }
            .sumOf {
                it.totalTimeInForeground
            }
    }

    private fun launchChallenge(
        targetPackage: String
    ) {
        mainHandler.post {
            // Cancel if the user has already left the target app.
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

                putExtra(
                    "show_math_challenge",
                    true
                )

                putExtra(
                    "blocked_package",
                    targetPackage
                )
            }

            try {
                startActivity(intent)

                // Fallback in case the activity fails to open.
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

    override fun onInterrupt() {
        // No action required.
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(checkRunnable)
        serviceScope.cancel()
        super.onDestroy()
    }
}
