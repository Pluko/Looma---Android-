package com.yourlooma.vault


import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.*
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import com.yourlooma.vault.ui.theme.LoomaTheme
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


enum class LoomaTab { Home, Usage, Permissions }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoomaTheme { LoomaApp() }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoomaApp() {
        var tab by remember { mutableStateOf(LoomaTab.Home) }
        var triggerRefresh by remember { mutableStateOf<(() -> Unit)?>(null) }
        var showAboutDialog by remember { mutableStateOf(false) }

        MaterialTheme {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Looma",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Your data. Your choice.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme
                                        .onSurface
                                        .copy(alpha = 0.7f)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showAboutDialog = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "About Looma"
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Column(Modifier.padding(innerPadding)) {

                    // Tabs
                    TabRow(
                        selectedTabIndex = when (tab) {
                            LoomaTab.Home -> 0
                            LoomaTab.Usage -> 1
                            LoomaTab.Permissions -> 2
                        }
                    ) {
                        Tab(
                            selected = tab == LoomaTab.Home,
                            onClick = { tab = LoomaTab.Home },
                            text = { Text("Home") }
                        )
                        Tab(
                            selected = tab == LoomaTab.Usage,
                            onClick = { tab = LoomaTab.Usage },
                            text = { Text("Usage") }
                        )
                        Tab(
                            selected = tab == LoomaTab.Permissions,
                            onClick = { tab = LoomaTab.Permissions },
                            text = { Text("Permissions") }
                        )
                    }

                    // Content under tabs
                    when (tab) {
                        LoomaTab.Home -> HomeScreen(
                            onSeeUsage = { tab = LoomaTab.Usage },
                            onReviewRisks = { tab = LoomaTab.Permissions },
                            onOpenPermissionFilter = { tab = LoomaTab.Permissions },
                            registerRefresh = { triggerRefresh = it }
                        )
                        LoomaTab.Usage -> UsageAccessScreen()
                        LoomaTab.Permissions -> PermissionsScreen()
                    }
                }
                }
            }
            // --- About Looma dialog (from 3-dot menu) ---
            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showAboutDialog = false }) {
                            Text("Close")
                        }
                    },
                    title = { Text("About Looma") },
                    text = {
                        Text(
                            "Looma helps you understand how apps use your data, focusing on sensitive permissions like camera, microphone, location, and contacts.\n\n" +
                                    "All scanning and analysis happens locally on your device. Looma does not create an account, upload your data, or share it with anyone. " +
                                    "You stay in control."
                        )
                    }
                )
            }

        }
        }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun HomeScreen(
        onSeeUsage: () -> Unit,
        onReviewRisks: () -> Unit,
        onOpenPermissionFilter: () -> Unit,
        registerRefresh: ((() -> Unit) -> Unit)
    ) {

        // Derived on demand, lives only in memory
        var lastScan by remember { mutableStateOf<Long?>(null) }
        var permSummaries by remember { mutableStateOf<List<AppPermSummary>>(emptyList()) }
        var topUsage by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }

        // Small info popups
        var showPrivacyInfo by remember { mutableStateOf(false) }
        var showChipsInfo by remember { mutableStateOf(false) }
        var showAboutDialog by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Quick permissions",
                style = MaterialTheme.typography.labelMedium
            )
            IconButton(
                onClick = { showChipsInfo = true },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "What do these numbers mean?",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Aggregates for the chips
        val camCount = permSummaries.count { "Camera" in it.granted }
        val micCount = permSummaries.count { "Microphone" in it.granted }
        val locCount = permSummaries.count { "Location" in it.granted }
        val conCount = permSummaries.count { "Contacts" in it.granted}

        // Very simple “score” for now
        val privacyScore = remember(permSummaries) {
            // Example: fewer total sensitive permissions → higher score
            val total = camCount + micCount + locCount + conCount
            // Clamp to 0–100
            (100 - total * 2).coerceIn(0, 100)
        }

        // Centralised refresh logic used by:
        //  - home card refresh icon
        //  - (optionally) toolbar refresh action
        val ctx = LocalContext.current

        val doRefresh: () -> Unit = refresh@ {
            if (loading) return@refresh

                loading = true
                Thread {
                    val perms = loadAppPermissionSummaries(ctx)
                    val usage = fetchTopUsage(context = ctx)

                    (ctx as ComponentActivity).runOnUiThread {
                        permSummaries = perms
                        topUsage = usage
                        lastScan = System.currentTimeMillis()
                        loading = false

                        Toast.makeText(
                             ctx,
                             "Dashboard refreshed",
                             Toast.LENGTH_SHORT
                        ).show()
                    }
                }.start()
            }
        // Allow the Activity/top bar to hook into the same refresh logic
        registerRefresh(doRefresh)
        // First run: auto-populate once
        LaunchedEffect(Unit) {
            if (permSummaries.isEmpty()) {
                doRefresh()
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ───────────────── Privacy card ─────────────────
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                // Header row: title + refresh icon
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ){
                        Text(
                            text = "Privacy score",
                            style = MaterialTheme.typography.labelMedium
                        )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { showPrivacyInfo = true },
                                modifier = Modifier.size(20.dp) // keeps it tiny
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = "What is the privacy score?",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                        Text(
                            text = "$privacyScore",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        // Last scanned timestamp
                        if (lastScan != null) {
                            val timeAgo = remember(lastScan) {
                                val diff = System.currentTimeMillis() - lastScan!!
                                val minutes = diff / 60000
                                when {
                                    minutes < 1 -> "Just now"
                                    minutes < 60 -> "${minutes}m ago"
                                    else -> "${minutes / 60}h ago"
                                }
                            }
                            Text(
                                text = "Last scanned: $timeAgo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Text(
                        text = "$camCount camera • $micCount mic • $locCount location • $conCount contacts",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                    IconButton(
                        onClick = { doRefresh() },
                        enabled = !loading
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh dashboard",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ───────────────── Usage snapshot card ─────────────────
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 14.dp)
                ) {
                    Text(
                        text = "Usage snapshot (24h)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Most-used apps in the last 24 hours (from Android’s Usage Access).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    )
                    val usageText = if (topUsage.isEmpty()) {
                        "No recent usage yet."
                    } else {
                        topUsage.take(3).joinToString(" • ") { (pkg, minutes) ->
                            val label = try {
                                val pm = ctx.packageManager
                                val info = pm.getApplicationInfo(pkg, 0)
                                pm.getApplicationLabel(info).toString()
                            } catch (_: Exception) {
                                pkg // fallback to package name
                            }
                            "$label ${minutes}m"
                        }
                    }

                    Text(
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        text = usageText,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedButton(onClick = onSeeUsage) {
                        Text(text = "See all usage")
                    }
                }
            }

            // ───────────────── Quick chips (auto-wrap) ─────────────────
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onOpenPermissionFilter,
                    label = { Text("Camera: $camCount") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                AssistChip(
                    onClick = onOpenPermissionFilter,
                    label = { Text("Location: $locCount") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                AssistChip(
                    onClick = onOpenPermissionFilter,
                    label = { Text("Mic: $micCount") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                AssistChip(
                    onClick = onOpenPermissionFilter,
                    label = { Text("Contacts: $conCount") },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // ───────────────── Vault preview placeholder ─────────────────
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 14.dp)
                ) {
                    Text(
                        text = "Looma vault (coming soon)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = "This will show saved snapshots and future insight cards.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Looma analyses your data locally on this device. Nothing is uploaded or shared.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                        // --- Info dialog: Privacy score ---
                        if (showPrivacyInfo) {
                            AlertDialog(
                                onDismissRequest = { showPrivacyInfo = false },
                                confirmButton = {
                                    TextButton(onClick = { showPrivacyInfo = false }) {
                                        Text("Got it")
                                    }
                                },
                                title = { Text("What is the privacy score?") },
                                text = {
                                    Text(
                                        "Your privacy score is based on how many apps on your device can access sensitive data like your camera, microphone, location, and contacts. " +
                                                "Looma calculates this score entirely on your device. Nothing leaves your phone."
                                    )
                                }
                            )
                        }
                }
            }
// --- Info dialog: Quick permissions chips ---
        if (showChipsInfo) {
            AlertDialog(
                onDismissRequest = { showChipsInfo = false },
                confirmButton = {
                    TextButton(onClick = { showChipsInfo = false }) {
                        Text("Got it")
                    }
                },
                title = { Text("What do these numbers mean?") },
                text = {
                    Text(
                        "These numbers show how many installed apps have been granted each permission (camera, microphone, location, or contacts). " +
                                "You can tap a chip to jump into your system settings and review those permissions."
                    )
                }
            )
        }
        }

private fun fetchTopUsage(context: Context): List<Pair<String, Long>> {
    val usm = context.getSystemService<UsageStatsManager>() ?: return emptyList()
    val end = System.currentTimeMillis()
    val start = end - TimeUnit.DAYS.toMillis(1) // last 24h

    // Get stats and convert to a list of (packageName, minutes)
    val stats = usm.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        start,
        end
    ) ?: return emptyList()

    return stats
        .asSequence()
        .filter { it.totalTimeInForeground > 0 } // keep only apps with usage
        .map { it.packageName to TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground) }
        .groupBy({ it.first }, { it.second }) // just in case of duplicates
        .mapValues { (_, minutesList) -> minutesList.sum() } // combine minutes for same package
        .toList()
        .sortedByDescending { it.second } // high -> low
}


/* -------------------------- Usage tab -------------------------- */
@Composable
fun UsageAccessScreen() {
    val ctx = LocalContext.current
    var usageList by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }
    var hasAccess by remember { mutableStateOf(hasUsageAccess(ctx)) } // your existing helper
    var loading by remember { mutableStateOf(false) }

    // Load usage once when screen appears (background thread -> UI update)
    LaunchedEffect(key1 = hasAccess) {
        if (hasAccess) {
            loading = true
            // keep same style as other code: run blocking work off main thread
            withContext(Dispatchers.IO) {
                val fetched = fetchTopUsage(ctx)
                // Post result on UI thread
                (ctx as? ComponentActivity)?.runOnUiThread {
                    usageList = fetched
                    loading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Looma Prototype - Usage Access",
            style = MaterialTheme.typography.headlineSmall
        )

        if (!hasAccess) {
            Text(
                text = "Grant Usage Access so Looma can show recently used apps.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(all = 16.dp)
            )
            Button(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }) {
                Text("Open Usage Access settings")
            }
            return@Column
        }

        // If loading, show a spinner
        if (loading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(12.dp))
                Text("Loading usage...", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            if (usageList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "No app usage yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Use your phone normally and check back in a few hours to see your app usage stats.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Show only apps that have usage (fetchTopUsage already filters zeros)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(usageList) { (pkg, mins) ->
                        // You probably have a helper to resolve package name -> app label/icon.
                        // Use packageManager if you want label/icon here. Minimal example:
                        val label = remember(pkg) {
                            try {
                                val pm = ctx.packageManager
                                val info = pm.getApplicationInfo(pkg, 0)
                                pm.getApplicationLabel(info).toString()
                            } catch (_: Exception) {
                                pkg // fallback to package name
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                            Text(text = "${mins}m", style = MaterialTheme.typography.bodyMedium)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}



private fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService<AppOpsManager>() ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Binder.getCallingUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                "android:get_usage_stats",
                Binder.getCallingUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun scanRecentApps(context: Context, onDone: (List<UsageStats>) -> Unit) {
        Thread {
            val usm = context.getSystemService<UsageStatsManager>()!!
            val end = System.currentTimeMillis()
            val start = end - TimeUnit.DAYS.toMillis(1)
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
                .sortedByDescending { it.lastTimeUsed }
            (context as ComponentActivity).runOnUiThread { onDone(stats) }
        }.start()
    }

    /* ----------------------- Permissions tab ----------------------- */

@Composable
fun PermissionsScreen() {
    val ctx = LocalContext.current
    var loading by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<AppPermSummary>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "These apps have been granted permissions to access sensitive features. Tap an app to open its system settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp)
        )
            Text("App permissions (granted)", style = MaterialTheme.typography.headlineSmall)
            Button(enabled = !loading, onClick = {
                loading = true
                Thread {
                    val data = loadAppPermissionSummaries(ctx)
                    (ctx as ComponentActivity).runOnUiThread {
                        apps = data
                        loading = false
                    }
                }.start()
            }) {
                if (loading) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("Scanning...")
                    }
                } else {
                    Text("Scan permissions")
                }
            }

            if (apps.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(apps) { a ->
                        ElevatedCard {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // App icon
                                val pm = ctx.packageManager
                                val drawable = try {
                                    pm.getApplicationIcon(a.packageName)
                                } catch (_: Exception) {
                                    null
                                }
                                drawable?.let {
                                    val bmp = (it as? BitmapDrawable)?.bitmap ?: it.toBitmap()
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(a.appLabel, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${a.granted.size} permissions granted",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    a.granted.take(3).forEach { p -> Text("• ${friendlyName(p)}") }
                                    if (a.granted.size > 3) Text("…")
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = { openAppSettings(ctx, a.packageName) }) {
                                    Text("Manage")
                                }
                            }
                        }
                    }
                }
            } else if (!loading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Ready to scan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Tap 'Scan permissions' above to see which apps have access to your camera, microphone, location, and other sensitive data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
