package com.ideasi.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

private const val REWARDED_AD_UNIT_ID =
    "ca-app-pub-8659591552192552/4129491190"

private const val TEST_REWARDED_AD_UNIT_ID =
    "ca-app-pub-3940256099942544/5224354917"

// Keep true while testing.
private const val USE_TEST_ADS = true

private const val MAX_DAILY_AD_REWARDS = 2

class MainActivity : ComponentActivity() {

    private var challengeRequested by mutableStateOf(false)
    private var blockedPackage: String? = null

    private var currentScreenState by mutableStateOf("splash")

    private var rewardedAd by mutableStateOf<RewardedAd?>(null)
    private var isRewardAdLoading by mutableStateOf(false)

    private var dailyRewardsUsed by mutableIntStateOf(0)

    private var shouldShowAdWhenLoaded = false

    private val preferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        loadRewardedAd()

        handleChallengeIntent(intent)

        if (!challengeRequested) {
            currentScreenState = "splash"
        }

        setContent {
            IdeasITheme {

                var currentScreen by remember {
                    mutableStateOf(currentScreenState)
                }

                // Keep the visible Compose screen synchronized
                // with navigation requests from the Activity.
                LaunchedEffect(currentScreenState) {
                    currentScreen = currentScreenState
                }

                // Normal first-launch splash navigation.
                // A challenge request must never be overwritten.
                LaunchedEffect(Unit) {
                    if (!challengeRequested &&
                        currentScreenState == "splash"
                    ) {
                        delay(1800L)

                        if (!challengeRequested &&
                            currentScreenState == "splash"
                        ) {
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
                                        currentScreenState = "dashboard"
                                    },
                                    icon = { Text("🏠") },
                                    label = { Text("Home") },
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
                                        currentScreenState = "apps"
                                    },
                                    icon = { Text("📱") },
                                    label = { Text("Select Apps") },
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
                                        currentScreenState = "settings"
                                    },
                                    icon = { Text("⚙️") },
                                    label = { Text("Settings") },
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
                                            currentScreenState = "settings"
                                        }
                                    )
                                }

                                "settings" -> {
                                    SetTimeScreen(
                                        onDone = {
                                            currentScreenState = "dashboard"
                                        }
                                    )
                                }

                                "dashboard" -> {
                                    DashboardScreen(
                                        onAddApps = {
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

                                    currentScreenState =
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

                                    currentScreenState = "permission"
                                },
                                onDecline = {
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

                                    currentScreenState = "dashboard"
                                }
                            )
                        }

                        "limitOptions" -> {
                            LimitOptionsScreen(
                                adReady = rewardedAd != null,
                                adLoading = isRewardAdLoading,
                                dailyRewardsUsed = dailyRewardsUsed,

                                onWatchAd = {
                                    val targetPackage = blockedPackage

                                    if (targetPackage.isNullOrBlank()) {
                                        Log.e(
                                            "TimeShieldAds",
                                            "No blocked package available"
                                        )
                                    } else if (
                                        canUseDailyAdReward(
                                            this@MainActivity,
                                            targetPackage
                                        )
                                    ) {
                                        if (rewardedAd == null) {
                                            shouldShowAdWhenLoaded = true
                                            loadRewardedAd()
                                        } else {
                                            showRewardedAd()
                                        }
                                    } else {
                                        dailyRewardsUsed =
                                            getDailyAdRewardsUsed(
                                                this@MainActivity,
                                                targetPackage
                                            )

                                        // Safety fallback:
                                        // Never send the third attempt
                                        // to the dashboard.
                                        currentScreenState = "challenge"
                                    }
                                },

                                onSolveMath = {
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
                                        grantChallengeBypass(targetPackage)
                                    }

                                    challengeRequested = false
                                    blockedPackage = null

                                    currentScreenState = "dashboard"

                                    reopenTargetApp(targetPackage)
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

        val adUnitId =
            if (USE_TEST_ADS) {
                TEST_REWARDED_AD_UNIT_ID
            } else {
                REWARDED_AD_UNIT_ID
            }

        RewardedAd.load(
            this,
            adUnitId,
            AdRequest.Builder().build(),

            object : RewardedAdLoadCallback() {

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardAdLoading = false

                    Log.d(
                        "TimeShieldAds",
                        "Rewarded ad loaded successfully"
                    )

                    if (shouldShowAdWhenLoaded) {
                        shouldShowAdWhenLoaded = false
                        showRewardedAd()
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardAdLoading = false
                    shouldShowAdWhenLoaded = false

                    Log.e(
                        "TimeShieldAds",
                        "Ad failed: ${error.code} - ${error.message}"
                    )
                }
            }
        )
    }

    private fun showRewardedAd() {

        val targetPackage = blockedPackage ?: return

        if (!canUseDailyAdReward(this, targetPackage)) {
            dailyRewardsUsed = getDailyAdRewardsUsed(
                this,
                targetPackage
            )

            currentScreenState = "challenge"
            return
        }

        val ad = rewardedAd

        if (ad == null) {
            shouldShowAdWhenLoaded = true
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
                    Log.e(
                        "TimeShieldAds",
                        "Ad failed to show: ${error.message}"
                    )

                    loadRewardedAd()
                }
            }

        ad.show(this) { _ ->

            // Count only after AdMob confirms the reward.
            val rewardRecorded = recordDailyAdReward(
                this,
                targetPackage
            )

            dailyRewardsUsed = getDailyAdRewardsUsed(
                this,
                targetPackage
            )

            if (!rewardRecorded) {
                Log.w(
                    "TimeShieldAds",
                    "Daily reward limit reached for $targetPackage"
                )

                currentScreenState = "challenge"
                return@show
            }

            // Successful ad skips maths.
            // It does not add extra usage minutes.
            grantChallengeBypass(targetPackage)

            challengeRequested = false
            blockedPackage = null

            currentScreenState = "dashboard"

            reopenTargetApp(targetPackage)
        }
    }

    private fun grantChallengeBypass(targetPackage: String) {

        val usedMinutes = getTodayUsageMinutes(
            this,
            targetPackage
        )

        setChallengePassedAtUsage(
            this,
            targetPackage,
            usedMinutes
        )

        preferences.edit()
            .putLong(
                COOLDOWN_KEY_PREFIX + targetPackage,
                System.currentTimeMillis() +
                    CHALLENGE_COOLDOWN_MS
            )
            .apply()

        setAppOverLimit(
            this,
            targetPackage,
            false
        )
    }

    private fun reopenTargetApp(targetPackage: String?) {

        if (targetPackage.isNullOrBlank()) return

        packageManager
            .getLaunchIntentForPackage(targetPackage)
            ?.let { targetIntent ->
                targetIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
                startActivity(targetIntent)
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

    /**
     * Receives the blocked app from AppAccessibilityService.
     *
     * First and second successful-ad attempts:
     *     Show Limit Options.
     *
     * After two successful ads:
     *     Open Maths Challenge directly.
     */
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

        // Consume the request so it is not processed again.
        intent.removeExtra("show_math_challenge")
        intent.removeExtra("blocked_package")

        if (targetPackage.isNullOrBlank()) {
            Log.e(
                "TimeShieldAds",
                "Challenge request received without blocked package"
            )
            return
        }

        blockedPackage = targetPackage

        dailyRewardsUsed = getDailyAdRewardsUsed(
            this,
            targetPackage
        )

        challengeRequested = true

        currentScreenState =
            if (dailyRewardsUsed >= MAX_DAILY_AD_REWARDS) {
                Log.d(
                    "TimeShieldAds",
                    "Two daily ads used. Opening maths directly for $targetPackage"
                )
                "challenge"
            } else {
                Log.d(
                    "TimeShieldAds",
                    "$dailyRewardsUsed daily ads used. Opening limit options for $targetPackage"
                )
                "limitOptions"
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
fun LimitOptionsScreen(
    adReady: Boolean,
    adLoading: Boolean,
    dailyRewardsUsed: Int,
    onWatchAd: () -> Unit,
    onSolveMath: () -> Unit
) {

    val remainingRewards =
        (MAX_DAILY_AD_REWARDS - dailyRewardsUsed).coerceAtLeast(0)

    Column(
        modifier = Modifier
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Choose how you want to continue.",
            color = SG_Muted
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onWatchAd,
            enabled = remainingRewards > 0,
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                when {
                    remainingRewards == 0 ->
                        "Daily ad rewards used"

                    adLoading ->
                        "Loading advertisement..."

                    !adReady ->
                        "Load Advertisement"

                    else ->
                        "Watch Ad - Skip Maths ($remainingRewards left)"
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            onClick = onSolveMath,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Solve Maths Question")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "You can earn up to 2 ad rewards per selected app each day.",
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

