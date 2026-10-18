
package com.ideasi.app

import android.content.Context

private const val PREFS_NAME = "ideas_i_limits"

fun setAppOverLimit(
    context: Context,
    packageName: String,
    overLimit: Boolean
) {
    context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
        .edit()
        .putBoolean(packageName, overLimit)
        .apply()
}

fun isAppOverLimit(
    context: Context,
    packageName: String
): Boolean {
    return context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
        .getBoolean(packageName, false)
}
fun setChallengePassedAtUsage(
    context: Context,
    packageName: String,
    usedMinutes: Int
) {
    context.getSharedPreferences(
        "ideas_i_limits",
        Context.MODE_PRIVATE
    ).edit()
        .putInt("passed_usage_$packageName", usedMinutes)
        .apply()
}

fun getChallengePassedUsageMinutes(
    context: Context,
    packageName: String
): Int {
    return context.getSharedPreferences(
        "ideas_i_limits",
        Context.MODE_PRIVATE
    ).getInt("passed_usage_$packageName", -1)
}
