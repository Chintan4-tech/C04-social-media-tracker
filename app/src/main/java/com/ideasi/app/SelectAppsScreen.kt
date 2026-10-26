package com.ideasi.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch

private val AppsNavy = Color(0xFF061426)
private val AppsSurface = Color(0xFF10243B)
private val AppsGreen = Color(0xFF19D7A0)
private val AppsYellow = Color(0xFFFFD43B)
private val AppsWhite = Color(0xFFFFFFFF)
private val AppsMuted = Color(0xFFB8C7D9)
private val AppsBorder = Color(0xFF7890AA)

@Composable
fun SelectAppsScreen(onNext: () -> Unit) {
val context = LocalContext.current
val db = remember { AppDatabase.getDatabase(context) }
val scope = rememberCoroutineScope()

val apps = remember { getInstalledApps(context) }

var searchQuery by remember { mutableStateOf("") }

val savedApps by db.selectedAppDao()
    .getAllSelectedApps()
    .collectAsState(initial = emptyList())

val selectedPackages = savedApps
    .map { it.packageName }
    .toSet()

val filteredApps = apps.filter { app ->
    app.appName.contains(searchQuery.trim(), ignoreCase = true)
}

val textFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = AppsWhite,
    unfocusedTextColor = AppsWhite,
    focusedContainerColor = AppsSurface,
    unfocusedContainerColor = AppsSurface,
    focusedBorderColor = AppsGreen,
    unfocusedBorderColor = AppsBorder,
    focusedLabelColor = AppsGreen,
    unfocusedLabelColor = AppsMuted,
    focusedPlaceholderColor = AppsMuted,
    unfocusedPlaceholderColor = AppsMuted,
    cursorColor = AppsGreen
)

Column(
    modifier = Modifier
        .fillMaxSize()
        .background(AppsNavy)
        .padding(16.dp)
) {
    Text(
        text = "Choose Your Enemies",
        style = MaterialTheme.typography.headlineSmall,
        color = AppsWhite,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Search apps") },
        placeholder = { Text("Instagram, YouTube...") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = textFieldColors
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "${selectedPackages.size} apps selected",
        style = MaterialTheme.typography.bodyMedium,
        color = AppsMuted
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
                    context.packageManager
                        .getApplicationIcon(app.packageName)
                        .toBitmap(width = 96, height = 96)
                        .asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = selectedPackages.contains(app.packageName),
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
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AppsGreen,
                        uncheckedColor = AppsMuted,
                        checkmarkColor = AppsNavy
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = "${app.appName} icon",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = AppsSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = app.appName.take(1).uppercase(),
                                color = AppsWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = app.appName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppsWhite
                )
            }

            HorizontalDivider(color = AppsBorder.copy(alpha = 0.55f))
        }

        if (filteredApps.isEmpty()) {
            item {
                Text(
                    text = "No matching apps found.",
                    modifier = Modifier.padding(16.dp),
                    color = AppsMuted
                )
            }
        }
    }

    Button(
        onClick = onNext,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppsGreen,
            contentColor = AppsNavy
        )
    ) {
        Text(
            text = "Continue Battle ⚔️",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

}
