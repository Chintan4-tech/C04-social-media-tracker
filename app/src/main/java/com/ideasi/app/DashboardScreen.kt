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

// ============================================================
// TIMESHIELD COLOUR PALETTE
// ============================================================

private val NavyBackground = Color(0xFF061426)
private val CardBackground = Color(0xFF10243B)
private val GreenAccent = Color(0xFF19D7A0)
private val YellowAccent = Color(0xFFFFD43B)
private val MutedText = Color(0xFFB0C1D5)
private val RedAccent = Color(0xFFFF5277)
private val BlueAccent = Color(0xFF3C91E6)

// ============================================================
// DASHBOARD SCREEN
// ============================================================

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

    // Refresh usage data every 30 seconds.
    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            refreshKey++
        }
    }

    // Recalculate usage whenever saved apps or refreshKey changes.
    val usageData = remember(savedApps, refreshKey) {
        savedApps.map { app ->
            val used = getTodayUsageMinutes(
                context,
                app.packageName
            )

            Triple(
                app,
                used,
                app.limitMinutes
            )
        }
    }

    val totalUsage = usageData.sumOf {
        it.second
    }

    val totalGoal = usageData.sumOf {
        it.third.coerceAtLeast(0)
    }

    val safeTotalGoal = totalGoal.coerceAtLeast(1)

    val goalProgress = (
        totalUsage.toFloat() / safeTotalGoal.toFloat()
    ).coerceIn(0f, 1f)

    // Red if any individual app has reached its limit.
    val anyAppLimitReached = usageData.any { entry ->
        entry.third <= 0 || entry.second >= entry.third
    }

    // Yellow when combined usage reaches 75% of combined limits.
    val warningThresholdReached =
        goalProgress >= 0.75f

    val overviewColor = when {
        anyAppLimitReached -> RedAccent
        warningThresholdReached -> YellowAccent
        else -> GreenAccent
    }

    val overviewMessage = when {
        anyAppLimitReached -> "Limit reached"
        warningThresholdReached -> "Getting close!"
        else -> "Keep going!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyBackground)
    ) {

        // ====================================================
        // HEADER
        // ====================================================

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

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF164A78),
                                Color(0xFF10243B)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⏱",
                    fontSize = 26.sp
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "TimeShield",
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
                color = overviewColor,
                fontSize = 18.sp
            )
        }

        // ====================================================
        // SCROLLABLE DASHBOARD CONTENT
        // ====================================================

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // =================================================
            // MOTIVATIONAL CARD
            // =================================================

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
                                        Color(0xFF202B4A)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {

                            Text(
                                text = "YOUR FUTURE IS BUILT TODAY",
                                color = GreenAccent,
                                fontSize = 11.sp,
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

            // =================================================
            // TODAY'S OVERVIEW
            // =================================================

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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Overview ring changes colour with usage.
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            colors = listOf(
                                                overviewColor,
                                                overviewColor.copy(alpha = 0.45f),
                                                overviewColor
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
                                    text = overviewMessage,
                                    color = overviewColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
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
                                    color = overviewColor,
                                    trackColor = Color(0xFF253B51)
                                )

                                Spacer(
                                    modifier = Modifier.height(6.dp)
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

            // =================================================
            // APP USAGE SECTION HEADER
            // =================================================

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
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(3.dp)
                        )

                        Text(
                            text = "Your selected apps and their limits",
                            color = MutedText,
                            fontSize = 12.sp
                        )
                    }

                    TextButton(
                        onClick = onAddApps
                    ) {
                        Text(
                            text = "+ Apps",
                            color = YellowAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // =================================================
            // EMPTY STATE
            // =================================================

            if (usageData.isEmpty()) {

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CardBackground
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "📱",
                                fontSize = 38.sp
                            )

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = "No apps selected yet",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )

                            Text(
                                text = "Choose apps to start tracking your usage.",
                                color = MutedText,
                                fontSize = 13.sp
                            )

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            Button(
                                onClick = onAddApps,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GreenAccent,
                                    contentColor = NavyBackground
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = "Select Apps",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

            } else {

                // =============================================
                // SELECTED APP USAGE CARDS
                // =============================================

                items(
                    items = usageData,
                    key = {
                        it.first.packageName
                    }
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
                        limitMinutes <= 0 ||
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

            // Existing dashboard sections remain as in the supplied
            // original. Math Challenge and Daily Streak cards were
            // not present in the code you supplied.
        }
    }
}

// ============================================================
// INDIVIDUAL APP USAGE CARD
// ============================================================

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

    // Load the installed application's icon.
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // Application icon.
            if (iconBitmap != null) {

                Image(
                    bitmap = iconBitmap,
                    contentDescription = "$appName icon",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

            } else {

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF263C53)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appName
                            .take(1)
                            .uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            // Usage and progress.
            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = appName,
                    color = Color.White,
                    fontSize = 15.sp,
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
                    fontSize = 12.sp
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
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

            // Limit status.
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
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
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

// ============================================================
// FORMAT USAGE MINUTES
// ============================================================

private fun formatMinutes(
    minutes: Int
): String {
    val safeMinutes = minutes.coerceAtLeast(0)

    val hours = safeMinutes / 60
    val remainingMinutes = safeMinutes % 60

    return if (hours > 0) {
        "${hours}h ${remainingMinutes}m"
    } else {
        "${remainingMinutes}m"
    }
}
