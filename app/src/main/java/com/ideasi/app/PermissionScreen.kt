package com.ideasi.app

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

@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner

    var hasPermission by remember {
        mutableStateOf(hasUsagePermission(context))
    }

    var continued by remember {
        mutableStateOf(false)
    }

    fun checkPermission() {
        val granted = hasUsagePermission(context)
        hasPermission = granted

        if (granted && !continued) {
            continued = true
            onPermissionGranted()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermission()
            }
        }

        lifecycleOwner?.lifecycle?.addObserver(observer)

        onDispose {
            lifecycleOwner?.lifecycle?.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkPermission()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Usage Access Needed",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Ideas I needs permission to monitor app usage " +
                "and enforce your selected time limits.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                openUsagePermissionSettings(context)
            }
        ) {
            Text("Enable Usage Access")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                checkPermission()
            }
        ) {
            Text("Check Permission")
        }

        if (!hasPermission) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Enable Usage Access in Android Settings, " +
                    "then return here. Ideas I will check automatically.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
