package com.ideasi.app

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch

@Composable
fun SelectAppsScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    val db = remember {
        AppDatabase.getDatabase(context)
    }
    val scope = rememberCoroutineScope()

    val apps = remember {
        getInstalledApps(context)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    val savedApps by db.selectedAppDao()
        .getAllSelectedApps()
        .collectAsState(initial = emptyList())

    val selectedPackages = savedApps
        .map { it.packageName }
        .toSet()

    val filteredApps = apps.filter { app ->
        app.appName.contains(
            searchQuery.trim(),
            ignoreCase = true
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Select Your Apps",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Search apps")
            },
            placeholder = {
                Text("Instagram, YouTube...")
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${selectedPackages.size} apps selected",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(
                items = filteredApps,
                key = { it.packageName }
            ) { app ->

                val appIcon = remember(app.packageName) {
                    try {
                        val drawable =
                            context.packageManager.getApplicationIcon(
                                app.packageName
                            )

                        drawable.toBitmap(
                            width = 96,
                            height = 96
                        ).asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedPackages.contains(
                            app.packageName
                        ),
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
                                    db.selectedAppDao()
                                        .deleteApp(app.packageName)
                                }
                            }
                        }
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = "${app.appName} icon",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(
                                    RoundedCornerShape(10.dp)
                                )
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.appName
                                        .take(1)
                                        .uppercase()
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.width(12.dp)
                    )

                    Text(
                        text = app.appName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                HorizontalDivider()
            }

            if (filteredApps.isEmpty()) {
                item {
                    Text(
                        text = "No matching apps found.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("Continue")
        }
    }
}
