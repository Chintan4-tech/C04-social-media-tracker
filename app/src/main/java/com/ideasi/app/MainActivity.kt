
package com.ideasi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp


class MainActivity : ComponentActivity() {

    private var challengeRequested by mutableStateOf(false)
    private var blockedPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleChallengeIntent(intent)

        setContent {
            IdeasITheme {
                var currentScreen by remember {
                    mutableStateOf(
                        if (challengeRequested) "challenge"
                        else "splash"
                    )
                }

                LaunchedEffect(challengeRequested) {
                    if (challengeRequested) {
                        currentScreen = "challenge"
                    }
                }

                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1000)
                    if (!challengeRequested) {
                        currentScreen = "selectApps"
                    }
                }

                when (currentScreen) {
                    "splash" -> SplashScreen()

                    "selectApps" -> SelectAppsScreen(
                        onNext = {
                            currentScreen = "setTime"
                        }
                    )

                    "setTime" -> SetTimeScreen(
                        onDone = {
                            currentScreen = "permission"
                        }
                    )

                    "permission" -> PermissionScreen(
                        onPermissionGranted = {
                            scheduleUsageMonitoring()
                            currentScreen = "dashboard"
                        }
                    )

                    "dashboard" -> DashboardScreen(
                        onAddApps = {
                            currentScreen = "selectApps"
                        }
                    )

                    "challenge" -> MathChallengeScreen(
                        difficulty = getChallengeDifficulty(
                            this@MainActivity
                        ),
                        onCorrectAnswer = {
                            val targetPackage = blockedPackage

                            if (!targetPackage.isNullOrBlank()) {
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
                                    .getLaunchIntentForPackage(targetPackage)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleChallengeIntent(intent)
    }

    private fun handleChallengeIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(
                "show_math_challenge",
                false
            ) == true
        ) {
            blockedPackage = intent.getStringExtra("blocked_package")
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
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = "IDEAS I",
                fontSize = androidx.compose.ui.unit.sp(32)
            )
        }
    }
}

