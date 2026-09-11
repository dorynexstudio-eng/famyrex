package com.famyrex.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
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

        val cloudRepository = FamyrexCloudFamilyRepository(applicationContext)
        val cloudFamilyId = cloudRepository.cachedFamilyId()
        if (cloudFamilyId.isNullOrBlank()) {
            setContent { FamyrexPremiumApp(applicationContext) }
        } else {
            setContent { FamilyRecoveryLoadingScreen() }
            cloudRepository.syncFamilySnapshot(
                familyId = cloudFamilyId,
                onSuccess = { snapshot ->
                    runCatching { familyStore.applyCloudFamilySnapshot(snapshot) }
                    runOnUiThread { setContent { FamyrexPremiumApp(applicationContext) } }
                },
                onError = {
                    // Never erase the local mirror on a failed refresh. The user can
                    // continue with the last known family state and retry next launch.
                    runOnUiThread { setContent { FamyrexPremiumApp(applicationContext) } }
                }
            )
        }
    }
}

@Composable
private fun FamilyRecoveryLoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Text("Recuperando la familia Famyrex…", style = MaterialTheme.typography.bodyLarge)
    }
}

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
    var tab by remember { mutableIntStateOf(0) }
    var zones by remember { mutableStateOf(loadPremiumZones(prefs)) }
    var parentalControlOpen by remember { mutableStateOf(false) }
    var remoteControlOpen by remember { mutableStateOf(false) }
    var familyManagementOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var usageReportOpen by remember { mutableStateOf(false) }
    var assistantOpen by remember { mutableStateOf(false) }

    if (settingsOpen) {
        BackHandler { settingsOpen = false }
        FamyrexSettingsScreen(context, onBack = { settingsOpen = false }, modifier = Modifier.fillMaxSize())
        return
    }

    fun resetSubscreens() {
        familyManagementOpen = false
        parentalControlOpen = false
        remoteControlOpen = false
        usageReportOpen = false
        assistantOpen = false
    }

    BackHandler(enabled = familyManagementOpen || parentalControlOpen || remoteControlOpen || usageReportOpen || assistantOpen) {
        when {
            remoteControlOpen -> remoteControlOpen = false
            parentalControlOpen -> parentalControlOpen = false
            familyManagementOpen -> familyManagementOpen = false
            usageReportOpen -> usageReportOpen = false
            assistantOpen -> assistantOpen = false
        }
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
                PremiumNavItem(0, tab, "Inicio", Icons.Default.Home) { tab = it; resetSubscreens() }
                PremiumNavItem(1, tab, "Alertas", Icons.Default.Notifications) { tab = it; resetSubscreens() }
                PremiumNavItem(2, tab, "Familia", Icons.Default.Person) { tab = it; resetSubscreens() }
                PremiumNavItem(3, tab, "Mapa", Icons.Default.LocationOn) { tab = it; resetSubscreens() }
                PremiumNavItem(4, tab, "Actividad", Icons.Default.CheckCircle) { tab = it; resetSubscreens() }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> FamilyChildOverviewScreen(context, Modifier.fillMaxSize(), onOpenAlerts = { tab = 1 }, onOpenLocation = { tab = 3 }, onOpenUsage = { tab = 4 }, onOpenFamily = { tab = 2; familyManagementOpen = true }, onOpenParentalControl = { tab = 2; parentalControlOpen = true }, onOpenSettings = { settingsOpen = true })
                1 -> PremiumAlertsScreen(context, Modifier.fillMaxSize())
                2 -> if (remoteControlOpen) {
                    FamilyRemoteControlScreen(context, FamyrexCloudFamilyRepository(context).cachedFamilyId(), Modifier.fillMaxSize())
                } else if (parentalControlOpen) {
                    ParentalControlScreen(Modifier.fillMaxSize())
                } else {
                    PremiumFamilyScreen(context, onOpenParentalControl = { parentalControlOpen = true }, onOpenRemoteControl = { remoteControlOpen = true }, onFamilyChanged = { familyManagementOpen = false }, openManagement = familyManagementOpen, modifier = Modifier.fillMaxSize())
                }
                3 -> PremiumLocationScreen(context, zones, { updated -> zones = updated; savePremiumZones(prefs, updated) }, Modifier.fillMaxSize())
                4 -> when {
                    assistantOpen -> PremiumAssistantScreen(context, Modifier.fillMaxSize())
                    usageReportOpen -> DailyReportScreen(context, onBack = { usageReportOpen = false }, modifier = Modifier.fillMaxSize())
                    else -> PremiumActivityScreen(context, onOpenReport = { usageReportOpen = true }, onOpenAssistant = { assistantOpen = true }, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun PremiumNavItem(index: Int, selected: Int, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onSelect: (Int) -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        IconButton(onClick = { onSelect(index) }) {
            Icon(icon, contentDescription = label, tint = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
