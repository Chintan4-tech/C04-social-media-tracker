

package com.ideasi.app

import android.content.Context
import java.util.Calendar

private const val REWARD_PREFS = "ScrollGuardPrefs"

private const val REWARD_DATE_PREFIX = "daily_ad_reward_date_"
private const val REWARD_COUNT_PREFIX = "daily_ad_reward_count_"

private const val MAX_DAILY_AD_REWARDS = 2

private fun todayRewardKey(): String {
    val calendar = Calendar.getInstance()

    return "${calendar.get(Calendar.YEAR)}-" +
            "${calendar.get(Calendar.DAY_OF_YEAR)}"
}

fun getDailyAdRewardsUsed(
    context: Context,
    packageName: String
): Int {

    val prefs = context.getSharedPreferences(
        REWARD_PREFS,
        Context.MODE_PRIVATE
    )

    val savedDate = prefs.getString(
        REWARD_DATE_PREFIX + packageName,
        ""
    )

    if (savedDate != todayRewardKey()) {
        return 0
    }

    return prefs.getInt(
        REWARD_COUNT_PREFIX + packageName,
        0
    ).coerceIn(0, MAX_DAILY_AD_REWARDS)
}

fun getRemainingDailyAdRewards(
    context: Context,
    packageName: String
): Int {

    return (
        MAX_DAILY_AD_REWARDS -
                getDailyAdRewardsUsed(context, packageName)
        ).coerceAtLeast(0)
}

fun canUseDailyAdReward(
    context: Context,
    packageName: String
): Boolean {

    return getDailyAdRewardsUsed(
        context,
        packageName
    ) < MAX_DAILY_AD_REWARDS
}

/**
 * Records one reward only after AdMob confirms completion.
 * Returns false if both daily rewards have already been used.
 */
fun recordDailyAdReward(
    context: Context,
    packageName: String
): Boolean {

    val prefs = context.getSharedPreferences(
        REWARD_PREFS,
        Context.MODE_PRIVATE
    )

    synchronized(REWARD_PREFS.intern()) {

        val today = todayRewardKey()

        val savedDate = prefs.getString(
            REWARD_DATE_PREFIX + packageName,
            ""
        )

        val currentCount =
            if (savedDate == today) {
                prefs.getInt(
                    REWARD_COUNT_PREFIX + packageName,
                    0
                )
            } else {
                0
            }

        if (currentCount >= MAX_DAILY_AD_REWARDS) {
            return false
        }

        return prefs.edit()
            .putString(
                REWARD_DATE_PREFIX + packageName,
                today
            )
            .putInt(
                REWARD_COUNT_PREFIX + packageName,
                currentCount + 1
            )
            .commit()
    }
}

/*
 * Compatibility functions:
 * These keep older project files compiling while using
 * the new two-rewards-per-app system.
 */

// True means the daily allowance is exhausted.
fun hasDailyFiveMinuteReward(
    context: Context,
    packageName: String
): Boolean {

    return !canUseDailyAdReward(
        context,
        packageName
    )
}

// Old function name retained for compatibility.
// It now records one ad reward, NOT five extra minutes.
fun grantDailyFiveMinuteReward(
    context: Context,
    packageName: String
): Boolean {

    return recordDailyAdReward(
        context,
        packageName
    )
}

// Preserve the original time limit.
// Rewarded ads no longer add five minutes.
fun getEffectiveLimitMinutes(
    context: Context,
    packageName: String,
    originalLimitMinutes: Int
): Int {

    return originalLimitMinutes
}

