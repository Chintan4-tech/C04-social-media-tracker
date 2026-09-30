package com.ideasi.app
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SelectAppsScreen() {
    val context = LocalContext.current

    // Remember the app list so we don't re-scan every time the screen redraws
    val apps = remember { getInstalledApps(context) }

    // Keep track of which package names are checked
    var selectedApps by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Select apps to track",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn {
            items(apps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedApps.contains(app.packageName),
                        onCheckedChange = { checked ->
                            selectedApps = if (checked) {
                                selectedApps + app.packageName
                            } else {
                                selectedApps - app.packageName
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = app.appName)
                }
            }
        }
    }
}
