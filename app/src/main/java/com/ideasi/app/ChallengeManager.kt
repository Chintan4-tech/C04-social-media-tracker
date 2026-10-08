package com.ideasi.app

import android.content.Context

private const val PREFS_NAME = "ideas_i_challenge"
private const val KEY_DIFFICULTY = "difficulty"

fun saveChallengeDifficulty(
    context: Context,
    difficulty: MathDifficulty
) {
    context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
        .edit()
        .putString(KEY_DIFFICULTY, difficulty.name)
        .apply()
}

fun getChallengeDifficulty(context: Context): MathDifficulty {
    val saved = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
        .getString(
            KEY_DIFFICULTY,
            MathDifficulty.EASY.name
        )

    return try {
        MathDifficulty.valueOf(saved ?: MathDifficulty.EASY.name)
    } catch (e: IllegalArgumentException) {
        MathDifficulty.EASY
    }
}
