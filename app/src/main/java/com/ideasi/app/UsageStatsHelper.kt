package com.ideasi.app

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

fun getTodayUsageMinutes(context: Context, packageName: String): Int {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()

    val statsList = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )

    val matchingStats = statsList.find { it.packageName == packageName }
    val totalMillis = matchingStats?.totalTimeInForeground ?: 0L

    return (totalMillis / 1000 / 60).toInt()
}
