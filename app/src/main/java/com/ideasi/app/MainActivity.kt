package com.ideasi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private var challengeRequested by mutableStateOf(false)
    private var blockedPackage: String? = null

    private val preferences by lazy {
        getSharedPreferences("ScrollGuardPrefs", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleChallengeIntent(intent)

        setContent {
            IdeasITheme {

                var currentScreen by remember {
                    mutableStateOf(
                        when {
                            challengeRequested -> "challenge"
                            preferences.getBoolean(
                                "setup_completed",
                                false
                            ) -> "dashboard"
                            else -> "splash"
                        }
                    )
                }

                LaunchedEffect(challengeRequested) {
                    if (challengeRequested) {
                        currentScreen = "challenge"
                    }
                }

                LaunchedEffect(Unit) {
                    if (
                        !challengeRequested &&
                        !preferences.getBoolean(
                            "setup_completed",
                            false
                        )
                    ) {
                        kotlinx.coroutines.delay(1500)

                        if (!challengeRequested) {
                            currentScreen = "selectApps"
                        }
                    }
                }

                when (currentScreen) {

                    "splash" -> {
                        SplashScreen()
                    }

                    "selectApps" -> {
                        SelectAppsScreen(
                            onNext = {
                                currentScreen = "setTime"
                            }
                        )
                    }

                    "setTime" -> {
                        SetTimeScreen(
                            onDone = {
                                val hasAgreed =
                                    preferences.getBoolean(
                                        "permission_disclosure_agreed",
                                        false
                                    )

                                currentScreen =
                                    if (hasAgreed) {
                                        "permission"
                                    } else {
                                        "disclosure"
                                    }
                            }
                        )
                    }

                    "disclosure" -> {
                        PermissionDisclosureScreen(
                            onAgree = {
                                preferences.edit()
                                    .putBoolean(
                                        "permission_disclosure_agreed",
                                        true
                                    )
                                    .apply()

                                currentScreen = "permission"
                            },
                            onDecline = {
                                currentScreen = "setTime"
                            }
                        )
                    }

                    "permission" -> {
                        PermissionScreen(
                            onPermissionGranted = {
                                scheduleUsageMonitoring()

                                preferences.edit()
                                    .putBoolean(
                                        "setup_completed",
                                        true
                                    )
                                    .apply()

                                currentScreen = "dashboard"
                            }
                        )
                    }

                    "dashboard" -> {
                        DashboardScreen(
                            onAddApps = {
                                currentScreen = "selectApps"
                            }
                        )
                    }

                    "challenge" -> {
                        MathChallengeScreen(
                            difficulty = getChallengeDifficulty(
                                this@MainActivity
                            ),
                            onCorrectAnswer = {
                                val targetPackage = blockedPackage

                                if (!targetPackage.isNullOrBlank()) {
                                    val usedMinutes =
                                        getTodayUsageMinutes(
                                            this@MainActivity,
                                            targetPackage
                                        )

                                    setChallengePassedAtUsage(
                                        this@MainActivity,
                                        targetPackage,
                                        usedMinutes
                                    )

                                    setAppOverLimit(
                                        this@MainActivity,
                                        targetPackage,
                                        false
                                    )
                                }

                                challengeRequested = false
                                blockedPackage = null
                                currentScreen = "dashboard"

                                if (!targetPackage.isNullOrBlank()) {
                                    packageManager
                                        .getLaunchIntentForPackage(
                                            targetPackage
                                        )
                                        ?.let { targetIntent ->
                                            startActivity(targetIntent)
                                        }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleChallengeIntent(intent)
    }

    private fun handleChallengeIntent(intent: Intent?) {
        if (
            intent?.getBooleanExtra(
                "show_math_challenge",
                false
            ) == true
        ) {
            blockedPackage =
                intent.getStringExtra("blocked_package")

            challengeRequested = true
        }
    }

    private fun scheduleUsageMonitoring() {
        val request =
            PeriodicWorkRequestBuilder<UsageLimitWorker>(
                15,
                TimeUnit.MINUTES
            ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "IdeasI_UsageMonitoring",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

@Composable
fun IdeasITheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

@Composable
fun SplashScreen() {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = androidx.compose.ui.graphics.Color(0xFF0A1929)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = "ScrollGuard",
                    color = androidx.compose.ui.graphics.Color(0xFF57D9A3),
                    fontSize = 32.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                androidx.compose.foundation.layout.Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(0.05f)
                )

                androidx.compose.material3.Text(
                    text = "Take Control. Reclaim Your Time.",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}
