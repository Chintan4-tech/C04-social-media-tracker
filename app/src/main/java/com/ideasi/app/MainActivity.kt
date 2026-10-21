package com.ideasi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

private val SG_Navy = Color(0xFF061426)
private val SG_Green = Color(0xFF19D7A0)
private val SG_Yellow = Color(0xFFFFD43B)
private val SG_Muted = Color(0xFF9DB0C8)

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

                // A Math Challenge must take priority over navigation.
                LaunchedEffect(challengeRequested) {
                    if (challengeRequested) {
                        currentScreen = "challenge"
                    }
                }

                // First-launch flow only.
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

                val showBottomNavigation =
                    currentScreen == "dashboard" ||
                    currentScreen == "apps" ||
                    currentScreen == "settings"

                if (showBottomNavigation) {

                    Scaffold(
                        containerColor = SG_Navy,

                        bottomBar = {
                            NavigationBar(
                                containerColor = Color(0xFF10243B)
                            ) {

                                NavigationBarItem(
                                    selected = currentScreen == "apps",

                                    onClick = {
                                        currentScreen = "apps"
                                    },

                                    icon = {
                                        Text("📱")
                                    },

                                    label = {
                                        Text("Apps")
                                    },

                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SG_Yellow,
                                        selectedTextColor = SG_Yellow,
                                        unselectedIconColor = SG_Muted,
                                        unselectedTextColor = SG_Muted,
                                        indicatorColor = Color(0xFF253B51)
                                    }
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "settings",

                                    onClick = {
                                        currentScreen = "settings"
                                    },

                                    icon = {
                                        Text("⚙️")
                                    },

                                    label = {
                                        Text("Settings")
                                    },

                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SG_Yellow,
                                        selectedTextColor = SG_Yellow,
                                        unselectedIconColor = SG_Muted,
                                        unselectedTextColor = SG_Muted,
                                        indicatorColor = Color(0xFF253B51)
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "dashboard",

                                    onClick = {
                                        currentScreen = "dashboard"
                                    },

                                    icon = {
                                        Text("🏠")
                                    },

                                    label = {
                                        Text("Home")
                                    },

                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SG_Yellow,
                                        selectedTextColor = SG_Yellow,
                                        unselectedIconColor = SG_Muted,
                                        unselectedTextColor = SG_Muted,
                                        indicatorColor = Color(0xFF253B51)
                                    )
                                )
                            }
                        }
                    ) { innerPadding ->

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SG_Navy)
                                .padding(innerPadding)
                        ) {

                            when (currentScreen) {

                                "apps" -> {
                                    SelectAppsScreen(
                                        onNext = {
                                            currentScreen = "settings"
                                        }
                                    )
                                }

                                "settings" -> {
                                    SetTimeScreen(
                                        onDone = {
                                            currentScreen = "settings"
                                        }
                                    )
                                }

                                "dashboard" -> {
                                    DashboardScreen(
                                        onAddApps = {
                                            currentScreen = "apps"
                                        }
                                    )
                                }
                            }
                        }
                    }

                } else {

                    // Setup and challenge screens do not show bottom navigation.
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
fun IdeasITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme {
        content()
    }
}

@Composable
fun SplashScreen() {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxSize(),
        color = SG_Navy
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement =
                androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment =
                androidx.compose.ui.Alignment.CenterHorizontally
        ) {

            Text(
                text = "ScrollGuard",
                color = SG_Green,
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "Take Control. Reclaim Your Time.",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = "Every focused minute is progress.",
                color = SG_Muted,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
