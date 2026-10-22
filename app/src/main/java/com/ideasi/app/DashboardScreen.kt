package com.ideasi.app

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.util.Calendar

private val NavyBackground = Color(0xFF061426)
private val CardBackground = Color(0xFF10243B)
private val GreenAccent = Color(0xFF19D7A0)
private val YellowAccent = Color(0xFFFFD43B)
private val MutedText = Color(0xFF9DB0C8)
private val RedAccent = Color(0xFFFF5277)

@Composable
fun DashboardScreen(
    onAddApps: () -> Unit
) {
    val context = LocalContext.current

    val db = remember {
        AppDatabase.getDatabase(context)
    }

    val savedApps by db.selectedAppDao()
        .getAllSelectedApps()
        .collectAsState(initial = emptyList())

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            refreshKey++
        }
    }

    val usageData = remember(savedApps, refreshKey) {
        savedApps.map { app ->
            val used = getTodayUsageMinutes(
                context,
                app.packageName
            )

            Triple(app, used, app.limitMinutes)
        }
    }

    val totalUsage = usageData.sumOf {
        it.second
    }

    val totalGoal = usageData.sumOf {
        it.third
    }.coerceAtLeast(1)

    val goalProgress = (
        totalUsage.toFloat() / totalGoal
    ).coerceIn(0f, 1f)

    val calendar = remember {
        Calendar.getInstance()
    }

    val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {

        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 20.dp,
                    top = 20.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "✦",
                color = YellowAccent,
                fontSize = 34.sp
            )

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "ScrollGuard",
                    color = Color.White,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Track • Focus • Grow",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }

            Text(
                text = "●",
                color = GreenAccent,
                fontSize = 18.sp
            )
        }

        // MAIN SCROLLABLE CONTENT
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // MOTIVATIONAL HEADER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF173C5A),
                                        Color(0xFF172443),
                                        Color(0xFF302447)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = "YOUR FUTURE IS BUILT TODAY",
                                color = GreenAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = "Discipline today builds the freedom you want tomorrow.",
                                color = Color.White,
                                fontSize = 23.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = "Small steps. Strong habits. Better focus.",
                                color = MutedText,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // DAILY SUMMARY
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardBackground
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "TODAY'S OVERVIEW",
                            color = MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            colors = listOf(
                                                GreenAccent,
                                                Color(0xFF087E79),
                                                GreenAccent
                                            )
                                        )
                                    )
                                    .padding(5.dp)
                                    .clip(CircleShape)
                                    .background(CardBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment =
                                        Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = formatMinutes(totalUsage),
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = "Used today",
                                        color = MutedText,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.width(16.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (
                                        totalUsage <= totalGoal
                                    ) {
                                        "Keep going!"
                                    } else {
                                        "Goal exceeded"
                                    },
                                    color = GreenAccent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(
                                    modifier = Modifier.height(5.dp)
                                )

                                Text(
                                    text = "${usageData.size} apps selected",
                                    color = MutedText,
                                    fontSize = 12.sp
                                )

                                Spacer(
                                    modifier = Modifier.height(10.dp)
                                )

                                LinearProgressIndicator(
                                    progress = {
                                        goalProgress
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(CircleShape),
                                    color = GreenAccent,
                                    trackColor = Color(0xFF253B51)
                                )

                                Spacer(
                                    modifier = Modifier.height(5.dp)
                                )

                                Text(
                                    text = "Combined selected-app limits",
                                    color = MutedText,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            // APP USAGE HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 4.dp,
                            bottom = 2.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "App Usage",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Your selected apps and their limits",
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }

                    TextButton(
                        onClick = onAddApps
                    ) {
                        Text(
                            text = "+ Apps",
                            color = YellowAccent
                        )
                    }
                }
            }

            // SELECTED APP ROWS
            if (usageData.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CardBackground
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No apps selected yet",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Choose apps to start tracking usage.",
                                color = MutedText,
                                fontSize = 12.sp
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Button(
                                onClick = onAddApps
                            ) {
                                Text("Select Apps")
                            }
                        }
                    }
                }
            } else {
                items(
                    items = usageData,
                    key = { it.first.packageName }
                ) { entry ->

                    val app = entry.first
                    val usedMinutes = entry.second
                    val limitMinutes = entry.third

                    val progress = if (limitMinutes > 0) {
                        (
                            usedMinutes.toFloat() /
                                limitMinutes.toFloat()
                        ).coerceIn(0f, 1f)
                    } else {
                        1f
                    }

                    val limitReached =
                        usedMinutes >= limitMinutes

                    AppUsageCard(
                        appName = app.appName,
                        packageName = app.packageName,
                        usedMinutes = usedMinutes,
                        limitMinutes = limitMinutes,
                        progress = progress,
                        limitReached = limitReached
                    )
                }
            }

            // MATH CHALLENGE CARD
            item {
                val reachedApp = usageData.firstOrNull {
                    it.second >= it.third
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF392344)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "🧮  Math Challenge",
                            color = YellowAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = if (reachedApp != null) {
                                "You've reached the limit for ${reachedApp.first.appName}. Open that app to trigger your configured challenge."
                            } else {
                                "Stay focused! When you reach a selected app's limit, ScrollGuard can request a math challenge."
                            },
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Practice your focus, one problem at a time.",
                            color = MutedText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // DAILY STREAK CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF25243B)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔥",
                            fontSize = 30.sp
                        )

                        Spacer(
                            modifier = Modifier.width(12.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Build Your Streak",
                                color = YellowAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Text(
                                text = "Make focused choices every day.",
                                color = MutedText,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = "Day $dayOfYear",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageCard(
    appName: String,
    packageName: String,
    usedMinutes: Int,
    limitMinutes: Int,
    progress: Float,
    limitReached: Boolean
) {
    val context = LocalContext.current

    val iconBitmap = remember(packageName) {
        try {
            val drawable: Drawable =
                context.packageManager.getApplicationIcon(
                    packageName
                )

            drawable.toBitmap(
                width = 96,
                height = 96
            ).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    val progressColor = if (limitReached) {
        RedAccent
    } else {
        GreenAccent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = "$appName icon",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF263C53)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = appName,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "${formatMinutes(usedMinutes)} / ${
                        formatMinutes(limitMinutes)
                    }",
                    color = MutedText,
                    fontSize = 11.sp
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                LinearProgressIndicator(
                    progress = {
                        progress
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = progressColor,
                    trackColor = Color(0xFF263B51)
                )
            }

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (limitReached) {
                        "LIMIT"
                    } else {
                        "ACTIVE"
                    },
                    color = progressColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = if (limitReached) {
                        "Reached"
                    } else {
                        "${(limitMinutes - usedMinutes).coerceAtLeast(0)}m left"
                    },
                    color = MutedText,
                    fontSize = 10.sp
                )
            }
        }
    }
}

private fun formatMinutes(minutes: Int): String {
    val safeMinutes = minutes.coerceAtLeast(0)
    val hours = safeMinutes / 60
    val remainingMinutes = safeMinutes % 60

    return if (hours > 0) {
        "${hours}h ${remainingMinutes}m"
    } else {
        "${remainingMinutes}m"
    }
}
