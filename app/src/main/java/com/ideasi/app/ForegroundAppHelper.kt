package com.ideasi.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

fun getCurrentForegroundApp(context: Context): String? {

    val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE)
            as UsageStatsManager

    val endTime = System.currentTimeMillis()
    val startTime = endTime - 10_000

    val events = usageStatsManager.queryEvents(
        startTime,
        endTime
    )

    val event = UsageEvents.Event()
    var foregroundPackage: String? = null
    var latestTimestamp = 0L

    while (events.hasNextEvent()) {
        events.getNextEvent(event)

        val isForegroundEvent =
            event.eventType == UsageEvents.Event.ACTIVITY_RESUMED

        if (isForegroundEvent &&
            event.timeStamp >= latestTimestamp
        ) {
            latestTimestamp = event.timeStamp
            foregroundPackage = event.packageName
        }
    }

    return foregroundPackage
}
