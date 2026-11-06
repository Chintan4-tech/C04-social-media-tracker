
package com.ideasi.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first

class UsageLimitWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {

        return try {
            val db = AppDatabase.getDatabase(applicationContext)

            val apps = db.selectedAppDao()
                .getAllSelectedApps()
                .first()

            for (app in apps) {

                val usedMinutes = getTodayUsageMinutes(
                    applicationContext,
                    app.packageName
                )

                // Include the rewarded-ad extension, if earned.
                val effectiveLimit = getEffectiveLimitMinutes(
                    applicationContext,
                    app.packageName,
                    app.limitMinutes.coerceAtLeast(1)
                )

                val overLimit = usedMinutes >= effectiveLimit

                setAppOverLimit(
                    applicationContext,
                    app.packageName,
                    overLimit
                )
            }

            Result.success()

        } catch (exception: Exception) {
            Result.retry()
        }
    }
}
