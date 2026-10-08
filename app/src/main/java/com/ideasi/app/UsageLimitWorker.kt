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

        val db = AppDatabase.getDatabase(applicationContext)

        val apps = db.selectedAppDao()
            .getAllSelectedApps()
            .first()

        for (app in apps) {

            val usedMinutes = getTodayUsageMinutes(
                applicationContext,
                app.packageName
            )

            val overLimit = usedMinutes >= app.limitMinutes

            setAppOverLimit(
                applicationContext,
                app.packageName,
                overLimit
            )
        }

        return Result.success()
    }
}
