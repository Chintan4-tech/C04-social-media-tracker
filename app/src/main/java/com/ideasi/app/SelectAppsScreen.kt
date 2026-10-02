package com.ideasi.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SelectAppsScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val apps = remember { getInstalledApps(context) }

    // Load currently saved selections from the database
    val savedApps by db.selectedAppDao().getAllSelectedApps()
        .collectAsState(initial = emptyList())

    val selectedPackageNames = savedApps.map { it.packageName }.toSet()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Select apps to track",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(apps) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedPackageNames.contains(app.packageName),
                        onCheckedChange = { checked ->
                            scope.launch {
                                if (checked) {
                                    db.selectedAppDao().insertApp(
                                        SelectedApp(
                                            packageName = app.packageName,
                                            appName = app.appName,
                                            limitMinutes = 30
                                        )
                                    )
                                } else {
                                    db.selectedAppDao().deleteApp(app.packageName)
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = app.appName)
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Next")
        }
    }
}
