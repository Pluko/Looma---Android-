package com.yourlooma.vault

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

data class AppPermSummary(
    val packageName: String,
    val appLabel: String,
    val granted: List<String>
)

private val friendlyMap = mapOf(
    "android.permission.CAMERA" to "Camera",
    "android.permission.RECORD_AUDIO" to "Microphone",
    "android.permission.ACCESS_FINE_LOCATION" to "Location",
    "android.permission.READ_CONTACTS" to "Contacts",
    "android.permission.READ_CALENDAR" to "Calendar",
    "android.permission.READ_SMS" to "Messages",
    "android.permission.BLUETOOTH_CONNECT" to "Bluetooth",
    "android.permission.POST_NOTIFICATIONS" to "Notifications",
)

/** Simplify raw permission name */
fun friendlyName(raw: String) = friendlyMap[raw] ?: raw.substringAfterLast('.')

fun loadAppPermissionSummaries(ctx: Context): List<AppPermSummary> {
    val pm = ctx.packageManager
    val pkgs = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
    val out = ArrayList<AppPermSummary>(pkgs.size)

    for (pi in pkgs) {
        val req = pi.requestedPermissions ?: emptyArray()
        val flags = pi.requestedPermissionsFlags ?: IntArray(0)
        val granted = mutableListOf<String>()
        for (i in req.indices) {
            val isGranted = ((flags.getOrNull(i) ?: 0) and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            if (isGranted) granted += req[i]
        }
        if (granted.isNotEmpty()) {
            val label = try { pi.applicationInfo.loadLabel(pm).toString() } catch (_: Exception) { pi.packageName }
            out += AppPermSummary(pi.packageName, label, granted)
        }
    }
    return out.sortedByDescending { it.granted.size }
}
