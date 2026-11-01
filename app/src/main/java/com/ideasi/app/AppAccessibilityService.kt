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
        private const val REFRESH_APPS_INTERVAL_MS = 10_000L
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
    private var selectedPackages: Set<String> = emptySet()

    @Volatile
    private var challengeLaunching = false

    @Volatile
    private var checking = false

    @Volatile
    private var lastAppsRefreshTime = 0L

    private var foregroundStartTime = 0L
    private var foregroundStartUsageMillis = 0L

    private val checkRunnable = object : Runnable {
        override fun run() {
            refreshSelectedAppsIfNeeded()

            val target = currentPackage

            if (
                target != null &&
                target in selectedPackages &&
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
                        // Keep the monitoring loop alive.
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

    private val refreshAppsRunnable = object : Runnable {
        override fun run() {
            refreshSelectedAppsIfNeeded(force = true)
            mainHandler.postDelayed(
                this,
                REFRESH_APPS_INTERVAL_MS
            )
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        mainHandler.removeCallbacks(checkRunnable)
        mainHandler.removeCallbacks(refreshAppsRunnable)

        refreshSelectedAppsIfNeeded(force = true)
        mainHandler.post(checkRunnable)
        mainHandler.post(refreshAppsRunnable)
    }

    private fun refreshSelectedAppsIfNeeded(
        force: Boolean = false
    ) {
        val now = SystemClock.elapsedRealtime()

        if (
            !force &&
            now - lastAppsRefreshTime < REFRESH_APPS_INTERVAL_MS
        ) {
            return
        }

        lastAppsRefreshTime = now

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(
                    applicationContext
                )

                val apps = db.selectedAppDao()
                    .getAllSelectedApps()
                    .first()

                selectedPackages = apps
                    .map { it.packageName }
                    .toSet()
            } catch (_: Exception) {
                // Keep the last successfully loaded list.
            }
        }
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {
        if (event == null) return

        if (
            event.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType !=
            AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            return
        }

        val target = event.packageName?.toString() ?: return

        // TimeShield opened: stop checking the blocked app while
        // the challenge screen is being displayed.
        if (target == packageName) {
            currentPackage = null
            challengeLaunching = false
            return
        }

        // Track only apps the user has selected.
        if (target in selectedPackages) {
            if (target != currentPackage) {
                currentPackage = target

                foregroundStartTime =
                    SystemClock.elapsedRealtime()

                foregroundStartUsageMillis =
                    getUsageMillis(target)
            }

            return
        }

        // Ignore common temporary system surfaces, keyboards and
        // permission overlays. They should not replace the app
        // currently being monitored.
        if (isTransientSystemPackage(target)) {
            return
        }

        // A different regular app or the launcher became active.
        // Do not keep monitoring the previous selected app.
        currentPackage = null
        challengeLaunching = false
    }

    private fun isTransientSystemPackage(
        target: String
    ): Boolean {
        return target == "com.android.systemui" ||
            target == "com.android.permissioncontroller" ||
            target == "com.google.android.permissioncontroller" ||
            target == "com.google.android.inputmethod.latin" ||
            target == "com.samsung.android.honeyboard" ||
            target.startsWith("com.samsung.android.app.")
    }

    private suspend fun checkUsageLimit(
        targetPackage: String
    ) {
        if (currentPackage != targetPackage) return
        if (targetPackage !in selectedPackages) return

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

        val limitMillis =
            selectedApp.limitMinutes
                .coerceAtLeast(1) * 60_000L

        val currentUsageMillis =
            getUsageMillis(targetPackage)

        val elapsedMillis = (
            SystemClock.elapsedRealtime() -
                foregroundStartTime
            ).coerceAtLeast(0L)

        // Android's usage statistics can update with a delay.
        // Use the foreground timer as a fallback estimate.
        val estimatedUsageMillis = maxOf(
            currentUsageMillis,
            foregroundStartUsageMillis + elapsedMillis
        )

        if (estimatedUsageMillis < limitMillis) return
        if (challengeLaunching) return
        if (isInChallengeCooldown(targetPackage)) return
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
        return try {
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

            stats
                .filter { it.packageName == targetPackage }
                .sumOf { it.totalTimeInForeground }
        } catch (_: Exception) {
            0L
        }
    }

    private fun launchChallenge(
        targetPackage: String
    ) {
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
            } catch (_: Exception) {
                challengeLaunching = false
            }
        }
    }

    override fun onInterrupt() {
        // No action required.
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(checkRunnable)
        mainHandler.removeCallbacks(refreshAppsRunnable)
        serviceScope.cancel()
        super.onDestroy()
    }
}
