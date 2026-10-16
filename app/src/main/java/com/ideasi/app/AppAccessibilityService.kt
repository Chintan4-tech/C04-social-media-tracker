package com.ideasi.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AppAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType !=
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val targetPackage = event.packageName?.toString()
            ?: return

        if (targetPackage == packageName) return

        if (!isAppOverLimit(this, targetPackage)) return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("show_math_challenge", true)
            putExtra("blocked_package", targetPackage)
        }

        startActivity(intent)
    }

    override fun onInterrupt() = Unit
}
