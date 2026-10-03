package com.ideasi.app

import android.content.Context
import kotlinx.coroutines.flow.first

data class LimitStatus(
    val app: SelectedApp,
    val usedMinutes: Int,
    val isOverLimit: Boolean
)

suspend fun checkAllLimits(context: Context): List<LimitStatus> {
    val db = AppDatabase.getDatabase(context)
    val apps = db.selectedAppDao().getAllSelectedApps().first()

    return apps.map { app ->
        val used = getTodayUsageMinutes(context, app.packageName)
        LimitStatus(
            app = app,
            usedMinutes = used,
            isOverLimit = used >= app.limitMinutes
        )
    }
}
