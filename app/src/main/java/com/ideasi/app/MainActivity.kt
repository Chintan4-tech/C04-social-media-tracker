package com.ideasi.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private val SG_Navy = Color(0xFF061426)
private val SG_Green = Color(0xFF19D7A0)
private val SG_Yellow = Color(0xFFFFD43B)
private val SG_Muted = Color(0xFFB8C7D9)
private val SG_Surface = Color(0xFF10243B)
private val SG_Indicator = Color(0xFF253B51)

private const val PREFS_NAME = "ScrollGuardPrefs"
private const val COOLDOWN_KEY_PREFIX = "challenge_cooldown_until_"
private const val CHALLENGE_COOLDOWN_MS = 3 * 60 * 1_000L

// Confirmed Rewarded Ad Unit ID.
// Use Google's test ID while developing.
private const val REWARDED_AD_UNIT_ID =
    "ca-app-pub-8659591552192552/4129491190"

private const val TEST_REWARDED_AD_UNIT_ID =
    "ca-app-pub-3940256099942544/5224354917"

class MainActivity : ComponentActivity() {

    private var challengeRequested by mutableStateOf(false)
    private var blockedPackage: String? = null

    private var currentScreenState by mutableStateOf("splash")

    private var rewardedAd by mutableStateOf<RewardedAd?>(null)
    private var isRewardAdLoading by mutableStateOf(false)
    private var rewardAlreadyUsed by mutableStateOf(false)

    private val preferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        loadRewardedAd()

        handleChallengeIntent(intent)

        currentScreenState = if (challengeRequested) {
            "limitOptions"
        } else {
            "splash"
        }

        setContent {
            IdeasITheme {

                var currentScreen by remember {
                    mutableStateOf(currentScreenState)
                }

                // Keep the Compose screen synchronized with Activity navigation.
                LaunchedEffect(currentScreenState) {
                    currentScreen = currentScreenState
                }

                // Open the limit options when a selected app reaches its limit.
                LaunchedEffect(challengeRequested) {
                    if (challengeRequested) {
                        currentScreen = "limitOptions"
                        currentScreenState = "limitOptions"

                        rewardAlreadyUsed = blockedPackage?.let {
                            hasDailyFiveMinuteReward(
                                this@MainActivity,
                                it
                            )
                        } ?: false
                    }
                }

                // Show splash, then choose the correct setup destination.
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

                        "limitOptions" -> {
                            LimitOptionsScreen(
                                adReady = rewardedAd != null,
                                adLoading = isRewardAdLoading,
                                rewardAlreadyUsed = rewardAlreadyUsed,

                                onWatchAd = {
                                    if (rewardedAd == null) {
                                        loadRewardedAd()
                                    } else {
                                        showRewardedAd()
                                    }
                                },

                                onSolveMath = {
                                    currentScreen = "challenge"
                                    currentScreenState = "challenge"
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

                                        // Start the existing three-minute cooldown.
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

                                    challengeRequested = false
                                    blockedPackage = null

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

    private fun loadRewardedAd() {
        if (isRewardAdLoading || rewardedAd != null) return

        isRewardAdLoading = true

        RewardedAd.load(
            this,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),

            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardAdLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardAdLoading = false
                }
            }
        )
    }

    private fun showRewardedAd() {
        val targetPackage = blockedPackage ?: return

        if (
            hasDailyFiveMinuteReward(
                this,
                targetPackage
            )
        ) {
            rewardAlreadyUsed = true
            return
        }

        val ad = rewardedAd

        if (ad == null) {
            loadRewardedAd()
            return
        }

        rewardedAd = null

        ad.fullScreenContentCallback =
            object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(
                    error: AdError
                ) {
                    loadRewardedAd()
                }
            }

        ad.show(this) { _ ->

            // Grant the extension only after AdMob confirms the reward.
            val granted = grantDailyFiveMinuteReward(
                this,
                targetPackage
            )

            if (!granted) {
                rewardAlreadyUsed = true
                return@show
            }

            rewardAlreadyUsed = true

            setAppOverLimit(
                this,
                targetPackage,
                false
            )

            challengeRequested = false
            blockedPackage = null

            currentScreenState = "dashboard"

            // Reopen the app after granting the reward.
            packageManager
                .getLaunchIntentForPackage(targetPackage)
                ?.let { targetIntent ->
                    targetIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
                    startActivity(targetIntent)
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

        rewardAlreadyUsed = hasDailyFiveMinuteReward(
            this,
            targetPackage
        )

        challengeRequested = true
        currentScreenState = "limitOptions"

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
fun LimitOptionsScreen(
    adReady: Boolean,
    adLoading: Boolean,
    rewardAlreadyUsed: Boolean,
    onWatchAd: () -> Unit,
    onSolveMath: () -> Unit
) {
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(SG_Navy)
            .padding(24.dp),

        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "TIME LIMIT REACHED",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))

        Text(
            text = "Choose how you want to continue.",
            color = SG_Muted
        )

        Spacer(modifier = androidx.compose.ui.Modifier.height(28.dp))

        Button(
            onClick = onWatchAd,
            enabled = !rewardAlreadyUsed,
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    rewardAlreadyUsed ->
                        "Today's reward already used"

                    adLoading ->
                        "Loading advertisement..."

                    adReady ->
                        "Watch Ad - Get 5 Extra Minutes"

                    else ->
                        "Load Advertisement"
                }
            )
        }

        Spacer(modifier = androidx.compose.ui.Modifier.height(14.dp))

        OutlinedButton(
            onClick = onSolveMath,
            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
        ) {
            Text("Solve Maths Question")
        }

        Spacer(modifier = androidx.compose.ui.Modifier.height(20.dp))

        Text(
            text = "One 5-minute ad reward per selected app each day.",
            color = SG_Muted,
            style = MaterialTheme.typography.bodySmall
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
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(SG_Navy)
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.timeshield_splash
            ),
            contentDescription = "TimeShield splash screen",
            contentScale = ContentScale.Fit,
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(8.dp)
        )
    }
}
