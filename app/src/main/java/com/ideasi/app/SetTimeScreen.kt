package com.ideasi.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment

@Composable
fun SetTimeScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val scope = rememberCoroutineScope()

    val savedApps by db.selectedAppDao().getAllSelectedApps()
        .collectAsState(initial = emptyList())

    // Local text for each app's input box, keyed by package name
    val textValues = remember { mutableStateMapOf<String, String>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Set time limits",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(savedApps) { app ->
                val currentText = textValues[app.packageName]
                    ?: app.limitMinutes.toString()

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = app.appName,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                    )
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { newValue ->
                            textValues[app.packageName] = newValue
                        },
                        label = { Text("minutes") },
                        modifier = Modifier.width(100.dp)
                    )
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    savedApps.forEach { app ->
                        val minutes = textValues[app.packageName]
                            ?.toIntOrNull() ?: app.limitMinutes
                        db.selectedAppDao().insertApp(
                            app.copy(limitMinutes = minutes)
                        )
                    }
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Save")
        }
    }
}
