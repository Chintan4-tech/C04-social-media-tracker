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
