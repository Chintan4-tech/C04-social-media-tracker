package com.ideasi.app

import android.content.Context
import android.content.Intent
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

private fun preferences(context: Context) =
    context.getSharedPreferences(
        "ideas_i_settings",
        Context.MODE_PRIVATE
    )

fun isAppOverLimit(
    context: Context,
    packageName: String
): Boolean {
    return preferences(context).getBoolean(
        "over_$packageName",
        false
    )
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
