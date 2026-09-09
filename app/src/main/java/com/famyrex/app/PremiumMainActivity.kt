package com.famyrex.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

private fun loadPremiumFamily(prefs: android.content.SharedPreferences): PremiumFamilyState = PremiumFamilyState(
    prefs.getString("parent_name", "") ?: "",
    prefs.getString("child_name", "") ?: ""
)

private fun loadPremiumZones(prefs: android.content.SharedPreferences): List<GeoZone> {
    val raw = prefs.getString("geo_zones", "") ?: return emptyList()
    if (raw.isBlank()) return emptyList()
    return raw.split(";").mapNotNull { row ->
        val parts = row.split("|")
        if (parts.size == 4) runCatching { GeoZone(parts[0], parts[1].toDouble(), parts[2].toDouble(), parts[3].toFloat()) }.getOrNull() else null
    }
}

private fun savePremiumZones(prefs: android.content.SharedPreferences, zones: List<GeoZone>) {
    prefs.edit().putString("geo_zones", zones.joinToString(";") { "${it.name}|${it.latitude}|${it.longitude}|${it.radiusMeters}" }).apply()
}

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

    fun refreshFamily() {
        val profiles = familyStore.profiles()
        val owner = profiles.firstOrNull { it.role == FamilyRole.OWNER }
        val child = profiles.firstOrNull { it.role == FamilyRole.CHILD }
        family = if (owner != null || child != null) {
            PremiumFamilyState(owner?.displayName.orEmpty(), child?.displayName.orEmpty())
        } else loadPremiumFamily(prefs)
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
                actions = {
                    IconButton(onClick = { settingsOpen = true }) { Icon(Icons.Default.Settings, "Ajustes") }
                }
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding()) {
                PremiumNavItem(0, tab, "Inicio", Icons.Default.Home) { tab = it; familyManagementOpen = false; parentalControlOpen = false; remoteControlOpen = false }
                PremiumNavItem(1, tab, "Alertas", Icons.Default.Notifications) { tab = it; familyManagementOpen = false }
                PremiumNavItem(2, tab, "Familia", Icons.Default.Person) { tab = it; familyManagementOpen = false; parentalControlOpen = false; remoteControlOpen = false }
                PremiumNavItem(3, tab, "Mapa", Icons.Default.LocationOn) { tab = it; familyManagementOpen = false }
                PremiumNavItem(4, tab, "Asistente", Icons.Default.Info) { tab = it; familyManagementOpen = false }
                PremiumNavItem(5, tab, "Informe", Icons.Default.CheckCircle) { tab = it; familyManagementOpen = false }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> FamilyChildOverviewScreen(
                    context = context,
                    modifier = Modifier.fillMaxSize(),
                    onOpenAlerts = { tab = 1 },
                    onOpenLocation = { tab = 3 },
                    onOpenUsage = { tab = 5 },
                    onOpenFamily = { tab = 2; familyManagementOpen = true },
                    onOpenParentalControl = { tab = 2; parentalControlOpen = true },
                    onOpenSettings = { settingsOpen = true }
                )
                1 -> RealAlertsScreen(context, Modifier.fillMaxSize())
                2 -> when {
                    remoteControlOpen -> FamilyRemoteControlScreen(context, FamyrexCloudFamilyRepository(context).cachedFamilyId(), Modifier.fillMaxSize())
                    parentalControlOpen -> ParentalControlScreen(Modifier.fillMaxSize())
                    familyManagementOpen -> FamilyCoreScreen(
                        context = context,
                        onOpenParentalControl = { parentalControlOpen = true },
                        onOpenRemoteControl = { remoteControlOpen = true },
                        onFamilyChanged = { refreshFamily() },
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> FamilyChildOverviewScreen(
                        context = context,
                        modifier = Modifier.fillMaxSize(),
                        onOpenAlerts = { tab = 1 },
                        onOpenLocation = { tab = 3 },
                        onOpenUsage = { tab = 5 },
                        onOpenFamily = { familyManagementOpen = true },
                        onOpenParentalControl = { parentalControlOpen = true },
                        onOpenSettings = { settingsOpen = true }
                    )
                }
                3 -> LocationScreen(context, zones, { updated -> zones = updated; savePremiumZones(prefs, updated) }, Modifier.fillMaxSize())
                4 -> FamilyAssistantScreen(context, Modifier.fillMaxSize())
                5 -> DailyReportScreen(context, onBack = { tab = 0 }, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PremiumNavItem(index: Int, selected: Int, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onSelect: (Int) -> Unit) {
    NavigationBarItem(
        selected = selected == index,
        onClick = { onSelect(index) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) }
    )
}
