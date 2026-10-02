package com.ideasi.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun DashboardScreen(onAddApps: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    val savedApps by db.selectedAppDao().getAllSelectedApps()
        .collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (savedApps.isEmpty()) {
            Text("No apps selected yet. Tap below to add some.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(savedApps) { app ->
                    // Placeholder until Stage 5 adds real usage tracking
                    val usedMinutes = 0
                    val remainingMinutes = app.limitMinutes - usedMinutes

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Used: $usedMinutes min")
                            Text("Limit: ${app.limitMinutes} min")
                            Text("Remaining: $remainingMinutes min")
                        }
                    }
                }
            }
        }

        Button(
            onClick = onAddApps,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Add / Change Apps")
        }
    }
}
