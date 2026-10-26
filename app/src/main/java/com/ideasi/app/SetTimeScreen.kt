package com.ideasi.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

private val BoundaryNavy = Color(0xFF061426)
private val BoundarySurface = Color(0xFF10243B)
private val BoundaryGreen = Color(0xFF19D7A0)
private val BoundaryYellow = Color(0xFFFFD43B)
private val BoundaryWhite = Color(0xFFFFFFFF)
private val BoundaryMuted = Color(0xFFB8C7D9)
private val BoundaryBorder = Color(0xFF7890AA)

@Composable
fun SetTimeScreen(onDone: () -> Unit) {
val context = androidx.compose.ui.platform.LocalContext.current
val db = remember { AppDatabase.getDatabase(context) }
val scope = rememberCoroutineScope()

val savedApps by db.selectedAppDao()
    .getAllSelectedApps()
    .collectAsState(initial = emptyList())

val textValues = remember { mutableStateMapOf<String, String>() }

var difficulty by remember {
    mutableStateOf(getChallengeDifficulty(context))
}

val inputColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = BoundaryWhite,
    unfocusedTextColor = BoundaryWhite,
    focusedContainerColor = BoundarySurface,
    unfocusedContainerColor = BoundarySurface,
    focusedBorderColor = BoundaryGreen,
    unfocusedBorderColor = BoundaryBorder,
    focusedLabelColor = BoundaryGreen,
    unfocusedLabelColor = BoundaryMuted,
    cursorColor = BoundaryGreen
)

Box(
    modifier = Modifier
        .fillMaxSize()
        .background(BoundaryNavy)
) {
    // Large mathematical symbol used as a subtle background watermark.
    Text(
        text = "π",
        modifier = Modifier
            .align(Alignment.Center)
            .zIndex(0f),
        fontSize = 280.sp,
        fontWeight = FontWeight.Black,
        color = BoundaryMuted.copy(alpha = 0.07f)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .zIndex(1f)
    ) {
        Text(
            text = "Set Your Boundaries",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = BoundaryWhite,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (savedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No enemies selected yet.\nGo back and choose your apps.",
                    color = BoundaryMuted,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(
                    items = savedApps,
                    key = { it.packageName }
                ) { app ->
                    val currentText =
                        textValues[app.packageName]
                            ?: app.limitMinutes.toString()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.appName,
                            modifier = Modifier.weight(1f),
                            color = BoundaryWhite,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        OutlinedTextField(
                            value = currentText,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() }) {
                                    textValues[app.packageName] = input
                                }
                            },
                            label = {
                                Text("Minutes")
                            },
                            singleLine = true,
                            modifier = Modifier.width(112.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = inputColors
                        )
                    }

                    HorizontalDivider(
                        color = BoundaryBorder.copy(alpha = 0.4f)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Math Difficulty",
                        style = MaterialTheme.typography.titleMedium,
                        color = BoundaryWhite,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Choose the challenge level for your battle.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BoundaryMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MathDifficulty.entries.forEach { level ->
                            val selected = difficulty == level

                            FilterChip(
                                selected = selected,
                                onClick = { difficulty = level },
                                label = {
                                    Text(
                                        text = level.name
                                            .lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BoundaryYellow,
                                    selectedLabelColor = BoundaryNavy,
                                    containerColor = BoundarySurface,
                                    labelColor = BoundaryMuted,
                                    selectedLeadingIconColor = BoundaryNavy
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = BoundaryBorder,
                                    selectedBorderColor = BoundaryYellow
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    savedApps.forEach { app ->
                        val enteredText = textValues[app.packageName]
                        val minutes = if (enteredText == null) {
                            app.limitMinutes
                        } else {
                            enteredText.toIntOrNull()
                                ?: app.limitMinutes
                        }

                        db.selectedAppDao().insertApp(
                            app.copy(
                                limitMinutes = minutes.coerceAtLeast(1)
                            )
                        )
                    }

                    saveChallengeDifficulty(context, difficulty)
                    onDone()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BoundaryGreen,
                contentColor = BoundaryNavy
            )
        ) {
            Text(
                text = "Save Boundaries",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

}
