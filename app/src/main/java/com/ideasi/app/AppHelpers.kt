package com.ideasi.app

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import kotlin.random.Random

data class InstalledApp(
    val appName: String,
    val packageName: String
)

enum class MathDifficulty {
    EASY, MEDIUM, HARD
}

data class MathQuestion(
    val question: String,
    val answer: Int
)

fun getInstalledApps(context: Context): List<InstalledApp> {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    return context.packageManager
        .queryIntentActivities(intent, 0)
        .mapNotNull { info ->
            val packageName = info.activityInfo.packageName

            if (packageName == context.packageName) {
                null
            } else {
                InstalledApp(
                    appName = info.loadLabel(
                        context.packageManager
                    ).toString(),
                    packageName = packageName
                )
            }
        }
        .distinctBy { it.packageName }
        .sortedBy { it.appName.lowercase() }
}

fun hasUsagePermission(context: Context): Boolean {
    val appOps = context.getSystemService(
        Context.APP_OPS_SERVICE
    ) as AppOpsManager

    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )

    return mode == AppOpsManager.MODE_ALLOWED
}

fun openUsagePermissionSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

private fun preferences(context: Context) =
    context.getSharedPreferences(
        "ideas_i_settings",
        Context.MODE_PRIVATE
    )

fun setAppOverLimit(
    context: Context,
    packageName: String,
    overLimit: Boolean
) {
    preferences(context).edit()
        .putBoolean("over_$packageName", overLimit)
        .apply()
}

fun isAppOverLimit(
    context: Context,
    packageName: String
): Boolean {
    return preferences(context).getBoolean(
        "over_$packageName",
        false
    )
}

fun saveChallengeDifficulty(
    context: Context,
    difficulty: MathDifficulty
) {
    preferences(context).edit()
        .putString("math_difficulty", difficulty.name)
        .apply()
}

fun getChallengeDifficulty(
    context: Context
): MathDifficulty {
    val saved = preferences(context).getString(
        "math_difficulty",
        MathDifficulty.EASY.name
    )

    return runCatching {
        MathDifficulty.valueOf(saved ?: "EASY")
    }.getOrDefault(MathDifficulty.EASY)
}

fun generateMathQuestion(
    difficulty: MathDifficulty
): MathQuestion {
    return when (difficulty) {
        MathDifficulty.EASY -> {
            val a = Random.nextInt(10, 100)
            val b = Random.nextInt(10, 100)

            MathQuestion("$a + $b", a + b)
        }

        MathDifficulty.MEDIUM -> {
            val a = Random.nextInt(10, 100)
            val b = Random.nextInt(2, 10)

            MathQuestion("$a × $b", a * b)
        }

        MathDifficulty.HARD -> {
            val a = Random.nextInt(100, 1000)
            val b = Random.nextInt(10, 100)

            MathQuestion("$a + $b", a + b)
        }
    }
}
