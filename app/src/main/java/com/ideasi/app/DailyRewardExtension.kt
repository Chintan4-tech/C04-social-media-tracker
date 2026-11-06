package com.ideasi.app

import android.content.Context
import java.util.Calendar

private const val REWARD_PREFS = "ScrollGuardPrefs"
private const val REWARD_DATE_PREFIX = "daily_reward_date_"

private const val EXTRA_MINUTES = 5

private fun todayKey(): String {
    val calendar = Calendar.getInstance()

    return "${calendar.get(Calendar.YEAR)}-" +
            "${calendar.get(Calendar.DAY_OF_YEAR)}"
}

fun hasDailyFiveMinuteReward(
    context: Context,
    packageName: String
): Boolean {
    val prefs = context.getSharedPreferences(
        REWARD_PREFS,
        Context.MODE_PRIVATE
    )

    return prefs.getString(
        REWARD_DATE_PREFIX + packageName,
        ""
    ) == todayKey()
}

fun grantDailyFiveMinuteReward(
    context: Context,
    packageName: String
): Boolean {
    if (hasDailyFiveMinuteReward(context, packageName)) {
        return false
    }

    context.getSharedPreferences(
        REWARD_PREFS,
        Context.MODE_PRIVATE
    ).edit()
        .putString(
            REWARD_DATE_PREFIX + packageName,
            todayKey()
        )
        .apply()

    return true
}

fun getEffectiveLimitMinutes(
    context: Context,
    packageName: String,
    originalLimitMinutes: Int
): Int {
    return originalLimitMinutes +
            if (hasDailyFiveMinuteReward(context, packageName)) {
                EXTRA_MINUTES
            } else {
                0
            }
}
