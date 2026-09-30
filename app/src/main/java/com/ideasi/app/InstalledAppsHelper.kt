package com.ideasi.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

fun getInstalledApps(context: Context): List<AppInfo> {
    val packageManager = context.packageManager

    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    val resolvedApps = packageManager.queryIntentActivities(intent, 0)

    return resolvedApps
        .map { resolveInfo ->
            AppInfo(
                appName = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.appName.lowercase() }
}
