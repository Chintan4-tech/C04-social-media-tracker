package com.ideasi.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val targetPackage = event.packageName?.toString()
            ?: return

        if (targetPackage == packageName) return

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

            // Don't show another challenge at the same recorded usage minute.
            if (usedMinutes <= passedUsageMinutes) {
                return@launch
            }

            setAppOverLimit(
                applicationContext,
                targetPackage,
                true
            )

            launch(Dispatchers.Main) {
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
        serviceScope.cancel()
        super.onDestroy()
    }
}

