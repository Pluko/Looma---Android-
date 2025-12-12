package com.yourlooma.vault

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class VaultInfo(val count: Int, val lastSaved: Long?)

private fun snapshotsDir(ctx: Context): File =
    File(ctx.filesDir, "looma/vault/snapshots").apply { mkdirs() }

fun getVaultInfo(ctx: Context): VaultInfo {
    val dir = snapshotsDir(ctx)
    val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    val last = files.firstOrNull()?.lastModified()
    return VaultInfo(files.size, last)
}

/** Save a lightweight snapshot (permissions + top usage) as JSON (internal storage). */
fun saveSnapshot(
    ctx: Context,
    perms: List<AppPermSummary>,
    topUsage: List<Pair<String, Long>> // packageName to minutes
): File {
    val now = System.currentTimeMillis()
    val dir = snapshotsDir(ctx)
    val tsName = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))
    val out = File(dir, "snapshot_$tsName.json")

    val root = JSONObject().apply {
        put("savedAt", now)
        put("permissions", JSONArray().apply {
            perms.forEach { p ->
                put(JSONObject().apply {
                    put("package", p.packageName)
                    put("label", p.appLabel)
                    put("granted", JSONArray(p.granted))
                })
            }
        })
        put("topUsage", JSONArray().apply {
            topUsage.forEach { (pkg, mins) ->
                put(JSONObject().apply {
                    put("package", pkg)
                    put("minutes", mins)
                })
            }
        })
    }

    out.writeText(root.toString(2))
    return out
}
