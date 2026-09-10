package com.famyrex.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class PremiumMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val familyStore = FamilyStore(applicationContext)
        SupervisedStateRestorer.restore(familyStore)
        FamyrexNotificationManager.ensureChannels(this)
        FamyrexWorkScheduler.scheduleProtectionHealth(applicationContext)
        setContent { FamyrexPremiumApp(applicationContext) }
    }
}

private data class PremiumFamilyState(val parent: String, val child: String)

private fun loadPremiumFamily(prefs: SharedPreferences): PremiumFamilyState = PremiumFamilyState(
    prefs.getString("parent_name", "") ?: "",
    prefs.getString("child_name", "") ?: ""
)

private fun loadPremiumZones(prefs: SharedPreferences): List<GeoZone> {
    val raw = prefs.getString("geo_zones", "") ?: return emptyList()
    if (raw.isBlank()) return emptyList()
    return raw.split(";").mapNotNull { row ->
        val parts = row.split("|")
        if (parts.size == 4) runCatching { GeoZone(parts[0], parts[1].toDouble(), parts[2].toDouble(), parts[3].toFloat()) }.getOrNull() else null
    }
}

private fun savePremiumZones(prefs: SharedPreferences, zones: List<GeoZone>) {
    prefs.edit().putString("geo_zones", zones.joinToString(";") { "${it.name}|${it.latitude}|${it.longitude}|${it.radiusMeters}" }).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamyrexPremiumApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("famyrex_prefs", Context.MODE_PRIVATE) }
    val familyStore = remember { FamilyStore(context) }
    var tab by remember { mutableIntStateOf(0) }
    var family by remember { mutableStateOf(loadPremiumFamily(prefs)) }
    var zones by remember { mutableStateOf(loadPremiumZones(prefs)) }
    var parentalControlOpen by remember { mutableStateOf(false) }
    var remoteControlOpen by remember { mutableStateOf(false) }
    var familyManagementOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var usageReportOpen by remember { mutableStateOf(false) }

    fun refreshFamily() {
        val profiles = familyStore.profiles()
        val owner = profiles.firstOrNull { it.role == FamilyRole.OWNER }
        val child = profiles.firstOrNull { it.role == FamilyRole.CHILD }
        family = if (owner != null || child != null) PremiumFamilyState(owner?.displayName.orEmpty(), child?.displayName.orEmpty()) else loadPremiumFamily(prefs)
    }

    LaunchedEffect(Unit) { refreshFamily() }

    if (settingsOpen) {
        FamyrexSettingsScreen(context, onBack = { settingsOpen = false }, modifier = Modifier.fillMaxSize())
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Famyrex", style = MaterialTheme.typography.titleLarge)
                        Text("Protección familiar inteligente", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = { IconButton(onClick = { settingsOpen = true }) { Icon(Icons.Default.Settings, "Ajustes") } }
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                PremiumNavItem(0, tab, "Inicio", Icons.Default.Home) { tab = it; familyManagementOpen = false; parentalControlOpen = false; remoteControlOpen = false; usageReportOpen = false }
                PremiumNavItem(1, tab, "Alertas", Icons.Default.Notifications) { tab = it; familyManagementOpen = false; parentalControlOpen = false; remoteControlOpen = false; usageReportOpen = false }
                PremiumNavItem(2, tab, "Familia", Icons.Default.Person) { tab = it; familyManagementOpen = false; parentalControlOpen = false; remoteControlOpen = false; usageReportOpen = false }
                PremiumNavItem(3, tab, "Mapa", Icons.Default.LocationOn) { tab = it; familyManagementOpen = false; parentalControlOpen = false; remoteControlOpen = false; usageReportOpen = false }
                PremiumNavItem(4, tab, "Asistente", Icons.Default.Info) { tab = it; familyManagementOpen = false; parentalControlOpen = false; remoteControlOpen = false; usageReportOpen = false }
                PremiumNavItem(5, tab, "Actividad", Icons.Default.CheckCircle) { tab = it; familyManagementOpen = false; parentalControlOpen = false; remoteControlOpen = false; usageReportOpen = false }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> FamilyChildOverviewScreen(context, Modifier.fillMaxSize(), onOpenAlerts = { tab = 1 }, onOpenLocation = { tab = 3 }, onOpenUsage = { tab = 5 }, onOpenFamily = { tab = 2; familyManagementOpen = true }, onOpenParentalControl = { tab = 2; parentalControlOpen = true }, onOpenSettings = { settingsOpen = true })
                1 -> RealAlertsScreen(context, Modifier.fillMaxSize())
                2 -> when {
                    remoteControlOpen -> FamilyRemoteControlScreen(context, FamyrexCloudFamilyRepository(context).cachedFamilyId(), Modifier.fillMaxSize())
                    parentalControlOpen -> ParentalControlScreen(Modifier.fillMaxSize())
                    familyManagementOpen -> FamilyCoreScreen(context = context, onOpenParentalControl = { parentalControlOpen = true }, onOpenRemoteControl = { remoteControlOpen = true }, onFamilyChanged = { refreshFamily() }, modifier = Modifier.fillMaxSize())
                    else -> FamilyChildOverviewScreen(context, Modifier.fillMaxSize(), onOpenAlerts = { tab = 1 }, onOpenLocation = { tab = 3 }, onOpenUsage = { tab = 5 }, onOpenFamily = { familyManagementOpen = true }, onOpenParentalControl = { parentalControlOpen = true }, onOpenSettings = { settingsOpen = true })
                }
                3 -> LocationScreen(context, zones, { updated -> zones = updated; savePremiumZones(prefs, updated) }, Modifier.fillMaxSize())
                4 -> FamilyAssistantScreen(context, Modifier.fillMaxSize())
                5 -> if (usageReportOpen) DailyReportScreen(context, onBack = { usageReportOpen = false }, modifier = Modifier.fillMaxSize()) else UsageInsightsScreen(context, Modifier.fillMaxSize(), onOpenSettings = { settingsOpen = true }, onOpenReport = { usageReportOpen = true })
            }
        }
    }
}

@Composable
private fun PremiumNavItem(index: Int, selected: Int, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onSelect: (Int) -> Unit) {
    androidx.compose.material3.NavigationBarItem(
        selected = selected == index,
        onClick = { onSelect(index) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) }
    )
}
