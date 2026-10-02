package com.ideasi.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(hasUsagePermission(context)) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Usage Access Needed",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Ideas I needs permission to see how long you use each app, " +
                "so it can enforce your time limits. This data stays on your device.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { openUsagePermissionSettings(context) }) {
            Text("Open Settings")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            hasPermission = hasUsagePermission(context)
            if (hasPermission) onPermissionGranted()
        }) {
            Text("I've granted it — Continue")
        }
    }
}
