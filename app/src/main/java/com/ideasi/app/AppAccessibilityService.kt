
package com.ideasi.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class AppAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val targetPackage = event.packageName?.toString()
            ?: return

        if (targetPackage == packageName) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val apps = db.selectedAppDao().getAllSelectedApps().first()

            val selectedApp = apps.find {
                it.packageName == targetPackage
            } ?: return@launch

            val usedMinutes = getTodayUsageMinutes(
                applicationContext,
                targetPackage
            )

            if (usedMinutes < selectedApp.limitMinutes) return@launch

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
}

