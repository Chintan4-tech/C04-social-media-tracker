package com.ideasi.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

private fun isAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(
        context,
        AppAccessibilityService::class.java
    ).flattenToString()

    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.split(':').any {
        it.equals(expected, ignoreCase = true)
    }
}

@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner

    var usageGranted by remember {
        mutableStateOf(hasUsagePermission(context))
    }

    var accessibilityGranted by remember {
        mutableStateOf(isAccessibilityEnabled(context))
    }

    var continued by remember {
        mutableStateOf(false)
    }

    fun checkPermissions() {
        usageGranted = hasUsagePermission(context)
        accessibilityGranted = isAccessibilityEnabled(context)

        if (usageGranted && accessibilityGranted && !continued) {
            continued = true
            onPermissionGranted()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }

        lifecycleOwner?.lifecycle?.addObserver(observer)

        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Set Up Ideas I",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "Enable both permissions to track app usage and trigger the Math Challenge when a limit is reached."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            if (usageGranted) "✓ Usage Access enabled"
            else "1. Usage Access required"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                openUsagePermissionSettings(context)
            },
            enabled = !usageGranted
        ) {
            Text("Enable Usage Access")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            if (accessibilityGranted) "✓ Accessibility enabled"
            else "2. Accessibility permission required"
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                )
            },
            enabled = !accessibilityGranted
        ) {
            Text("Enable Accessibility")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                checkPermissions()
            }
        ) {
            Text("Check Permissions")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "After enabling each permission in Android Settings, return here. Ideas I will check automatically.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
