package com.yourlooma.vault

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri

/** Load an app's icon and label safely */
fun getAppLabelAndIcon(ctx: Context, info: ApplicationInfo): Pair<String, Any?> {
    val pm = ctx.packageManager
    return try {
        val label = info.loadLabel(pm).toString()
        val icon = info.loadIcon(pm)
        label to icon
    } catch (_: Exception) {
        info.packageName to null
    }
}

/** Open system settings for the selected app */
fun openAppSettings(ctx: Context, packageName: String) {
    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    ctx.startActivity(intent)
}
