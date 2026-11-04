package com.ideasi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import com.google.android.gms.ads.MobileAds

private val SG_Navy = Color(0xFF061426)
private val SG_Green = Color(0xFF19D7A0)
private val SG_Yellow = Color(0xFFFFD43B)
private val SG_Muted = Color(0xFFB8C7D9)
private val SG_Surface = Color(0xFF10243B)
private val SG_Indicator = Color(0xFF253B51)

private const val PREFS_NAME = "ScrollGuardPrefs"
private const val COOLDOWN_KEY_PREFIX = "challenge_cooldown_until_"
private const val CHALLENGE_COOLDOWN_MS = 3 * 60 * 1_000L

class MainActivity : ComponentActivity() {

    private var challengeRequested by mutableStateOf(false)
    private var blockedPackage: String? = null

    private var currentScreenState by mutableStateOf("splash")

    private val preferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}

        handleChallengeIntent(intent)

        currentScreenState = if (challengeRequested) {
            "challenge"
        } else {
            "splash"
        }

        setContent {
            IdeasITheme {

                var currentScreen by remember {
                    mutableStateOf(currentScreenState)
                }

                // Switch immediately to the challenge when a new
                // challenge intent arrives.
                LaunchedEffect(challengeRequested) {
                    if (challengeRequested) {
                        currentScreen = "challenge"
                        currentScreenState = "challenge"
                    }
                }

                // Show the splash screen, then choose the correct
                // destination according to setup completion.
                LaunchedEffect(Unit) {
                    if (!challengeRequested) {
                        delay(1800L)

                        if (!challengeRequested) {
                            val nextScreen =
                                if (
                                    preferences.getBoolean(
                                        "setup_completed",
                                        false
                                    )
                                ) {
                                    "dashboard"
                                } else {
                                    "selectApps"
                                }

                            currentScreen = nextScreen
                            currentScreenState = nextScreen
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
                                containerColor = SG_Surface
                            ) {

                                NavigationBarItem(
                                    selected = currentScreen == "dashboard",
                                    onClick = {
                                        currentScreen = "dashboard"
                                        currentScreenState = "dashboard"
                                    },
                                    icon = {
                                        Text(text = "🏠")
                                    },
                                    label = {
                                        Text(text = "Home")
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SG_Yellow,
                                        selectedTextColor = SG_Yellow,
                                        unselectedIconColor = SG_Muted,
                                        unselectedTextColor = SG_Muted,
                                        indicatorColor = SG_Indicator
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "apps",
                                    onClick = {
                                        currentScreen = "apps"
                                        currentScreenState = "apps"
                                    },
                                    icon = {
                                        Text(text = "📱")
                                    },
                                    label = {
                                        Text(text = "Select Apps")
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SG_Yellow,
                                        selectedTextColor = SG_Yellow,
                                        unselectedIconColor = SG_Muted,
                                        unselectedTextColor = SG_Muted,
                                        indicatorColor = SG_Indicator
                                    )
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "settings",
                                    onClick = {
                                        currentScreen = "settings"
                                        currentScreenState = "settings"
                                    },
                                    icon = {
                                        Text(text = "⚙️")
                                    },
                                    label = {
                                        Text(text = "Settings")
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = SG_Yellow,
                                        selectedTextColor = SG_Yellow,
                                        unselectedIconColor = SG_Muted,
                                        unselectedTextColor = SG_Muted,
                                        indicatorColor = SG_Indicator
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
                                            currentScreenState = "settings"
                                        }
                                    )
                                }

                                "settings" -> {
                                    SetTimeScreen(
                                        onDone = {
                                            currentScreen = "dashboard"
                                            currentScreenState = "dashboard"
                                        }
                                    )
                                }

                                "dashboard" -> {
                                    DashboardScreen(
                                        onAddApps = {
                                            currentScreen = "apps"
                                            currentScreenState = "apps"
                                        }
                                    )
                                }
                            }
                        }
                    }

                } else {

                    when (currentScreen) {

                        "splash" -> {
                            SplashScreen()
                        }

                        "selectApps" -> {
                            SelectAppsScreen(
                                onNext = {
                                    currentScreen = "setTime"
                                    currentScreenState = "setTime"
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

                                    val nextScreen =
                                        if (hasAgreed) {
                                            "permission"
                                        } else {
                                            "disclosure"
                                        }

                                    currentScreen = nextScreen
                                    currentScreenState = nextScreen
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
                                    currentScreenState = "permission"
                                },
                                onDecline = {
                                    currentScreen = "setTime"
                                    currentScreenState = "setTime"
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
                                    currentScreenState = "dashboard"
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

                                        // Start the three-minute cooldown.
                                        preferences.edit()
                                            .putLong(
                                                COOLDOWN_KEY_PREFIX +
                                                    targetPackage,
                                                System.currentTimeMillis() +
                                                    CHALLENGE_COOLDOWN_MS
                                            )
                                            .apply()

                                        setAppOverLimit(
                                            this@MainActivity,
                                            targetPackage,
                                            false
                                        )
                                    }

                                    // Clear the current challenge.
                                    challengeRequested = false
                                    blockedPackage = null

                                    // Return to the dashboard.
                                    currentScreen = "dashboard"
                                    currentScreenState = "dashboard"

                                    // Reopen the app the user was using.
                                    if (!targetPackage.isNullOrBlank()) {
                                        packageManager
                                            .getLaunchIntentForPackage(
                                                targetPackage
                                            )
                                            ?.let { targetIntent ->
                                                targetIntent.addFlags(
                                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                                )
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

    override fun onResume() {
        super.onResume()

        // Process a challenge only if its intent extra is present.
        handleChallengeIntent(intent)
    }

    private fun handleChallengeIntent(intent: Intent?) {
        if (
            intent?.getBooleanExtra(
                "show_math_challenge",
                false
            ) != true
        ) {
            return
        }

        val targetPackage =
            intent.getStringExtra("blocked_package")

        if (targetPackage.isNullOrBlank()) {
            intent.removeExtra("show_math_challenge")
            intent.removeExtra("blocked_package")
            return
        }

        blockedPackage = targetPackage
        challengeRequested = true
        currentScreenState = "challenge"

        // Consume the extras so the same intent is not processed again.
        intent.removeExtra("show_math_challenge")
        intent.removeExtra("blocked_package")
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
    val darkColors = androidx.compose.material3.darkColorScheme(
        primary = SG_Green,
        onPrimary = SG_Navy,

        secondary = SG_Yellow,
        onSecondary = SG_Navy,

        background = SG_Navy,
        onBackground = Color.White,

        surface = SG_Surface,
        onSurface = Color.White,

        surfaceVariant = SG_Indicator,
        onSurfaceVariant = SG_Muted,

        outline = Color(0xFF7890AA)
    )

    MaterialTheme(
        colorScheme = darkColors,
        content = content
    )
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SG_Navy)
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.timeshield_splash
            ),
            contentDescription = "TimeShield splash screen",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        )
    }
}
