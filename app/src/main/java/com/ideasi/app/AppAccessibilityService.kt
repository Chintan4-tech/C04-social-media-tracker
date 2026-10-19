package com.ideasi.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppAccessibilityService : AccessibilityService() {

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentPackage: String? = null
    private var challengeLaunching = false

    private val checkRunnable = object : Runnable {
        override fun run() {
            val targetPackage = currentPackage

            if (targetPackage != null && !challengeLaunching) {
                checkUsageLimit(targetPackage)
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

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                val targetPackage = event.packageName?.toString()
                    ?: return

                if (targetPackage == packageName) {
                    currentPackage = null
                    return
                }

                currentPackage = targetPackage
            }
        }
    }

    private fun checkUsageLimit(targetPackage: String) {
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)

            val apps = db.selectedAppDao()
                .getAllSelectedApps()
                .first()

            val selectedApp = apps.find {
                it.packageName == targetPackage
            } ?: return@launch

            val usedMinutes = getTodayUsageMinutes(
                applicationContext,
                targetPackage
            )

            if (usedMinutes < selectedApp.limitMinutes) {
                return@launch
            }

            val passedUsageMinutes =
                getChallengePassedUsageMinutes(
                    applicationContext,
                    targetPackage
                )

            if (usedMinutes <= passedUsageMinutes) {
                return@launch
            }

            setAppOverLimit(
                applicationContext,
                targetPackage,
                true
            )

            launch(Dispatchers.Main) {
                if (challengeLaunching) return@launch

                challengeLaunching = true

                val intent = Intent(
                    this@AppAccessibilityService,
                    MainActivity::class.java
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                    putExtra("show_math_challenge", true)
                    putExtra("blocked_package", targetPackage)
                }

                startActivity(intent)
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
