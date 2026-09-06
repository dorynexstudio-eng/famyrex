package com.famyrex.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import android.content.Context
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Calendar
import kotlin.random.Random

private const val PREFS = "famyrex_prefs"
private const val KEY_PARENT = "parent_name"
private const val KEY_CHILD = "child_name"
private const val KEY_CODE = "link_code"

data class FamilyState(val parent: String, val child: String, val code: String)
data class GeoZone(val name: String, val latitude: Double, val longitude: Double, val radiusMeters: Float)

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val familyStore = FamilyStore(applicationContext)
        SupervisedStateRestorer.restore(familyStore)
        FamyrexNotificationManager.ensureChannels(this)
        FamyrexWorkScheduler.scheduleProtectionHealth(applicationContext)
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { FamyrexApp(applicationContext) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamyrexApp(context: Context) {
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    val familyStore = remember { FamilyStore(context) }
    var tab by remember { mutableIntStateOf(0) }
    var family by remember { mutableStateOf(loadFamily(prefs)) }
    var zones by remember { mutableStateOf(loadZones(prefs)) }
    var parentalControlOpen by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    fun refreshFamily() {
        val profiles = familyStore.profiles()
        val owner = profiles.firstOrNull { it.role == FamilyRole.OWNER }
        val child = profiles.firstOrNull { it.role == FamilyRole.CHILD }
        if (owner != null || child != null) {
            family = FamilyState(
                parent = owner?.displayName.orEmpty(),
                child = child?.displayName.orEmpty(),
                code = prefs.getString(KEY_CODE, "") ?: ""
            )
        } else {
            family = loadFamily(prefs)
        }
    }

    LaunchedEffect(Unit) { refreshFamily() }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refreshFamily()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Famyrex") }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == 0, { tab = 0 }, icon = {}, label = { Text("Inicio") })
                    NavigationBarItem(tab == 1, { tab = 1 }, icon = {}, label = { Text("Alertas") })
                    NavigationBarItem(tab == 2, { tab = 2; parentalControlOpen = false }, icon = {}, label = { Text("Familia") })
                    NavigationBarItem(tab == 3, { tab = 3 }, icon = {}, label = { Text("Ubicación") })
                    NavigationBarItem(tab == 4, { tab = 4 }, icon = {}, label = { Text("Asistente") })
                    NavigationBarItem(tab == 5, { tab = 5 }, icon = {}, label = { Text("Informe") })
                }
            }
        ) { padding ->
            when (tab) {
                0 -> Dashboard(
                    context = context,
                    family = family,
                    modifier = Modifier.padding(padding),
                    onRecommendationAction = { destination ->
                        when (destination) {
                            FamilyIntelligenceRecommendationDestination.ALERTS -> {
                                parentalControlOpen = false
                                tab = 1
                            }
                            FamilyIntelligenceRecommendationDestination.PARENTAL_CONTROL -> {
                                tab = 2
                                parentalControlOpen = true
                            }
                            FamilyIntelligenceRecommendationDestination.OBSERVE -> Unit
                        }
                    },
                    onOpenReport = { tab = 5 }
                )
                1 -> RealAlertsScreen(context, Modifier.padding(padding))
                2 -> if (parentalControlOpen) {
                    ParentalControlScreen(modifier = Modifier.padding(padding))
                } else {
                    FamilyCoreScreen(
                        context = context,
                        onOpenParentalControl = { parentalControlOpen = true },
                        onFamilyChanged = { refreshFamily() },
                        modifier = Modifier.padding(padding)
                    )
                }
                3 -> LocationScreen(context, zones, { updated -> zones = updated; saveZones(prefs, updated) }, Modifier.padding(padding))
                4 -> FamilyAssistantScreen(context, Modifier.padding(padding))
                5 -> DailyReportScreen(context, onBack = { tab = 0 }, modifier = Modifier.padding(padding))
            }
        }
    }
}

private fun loadFamily(prefs: android.content.SharedPreferences): FamilyState = FamilyState(
    prefs.getString(KEY_PARENT, "") ?: "", prefs.getString(KEY_CHILD, "") ?: "", prefs.getString(KEY_CODE, "") ?: ""
)

private fun saveFamily(prefs: android.content.SharedPreferences, state: FamilyState) {
    prefs.edit().putString(KEY_PARENT, state.parent).putString(KEY_CHILD, state.child).putString(KEY_CODE, state.code).apply()
}

@Composable
fun Dashboard(
    context: Context,
    family: FamilyState,
    modifier: Modifier = Modifier,
    onRecommendationAction: (FamilyIntelligenceRecommendationDestination) -> Unit = {},
    onOpenReport: () -> Unit = {}
) {
    val configured = family.parent.isNotBlank() && family.child.isNotBlank()
    var components by remember { mutableStateOf(ProtectionComponentChecker.check(context)) }
    var intelligenceStatus by remember { mutableStateOf<ParentalStatus?>(null) }

    fun refreshDashboard() {
        components = ProtectionComponentChecker.check(context)
        val usageMonitor = ParentalUsageMonitor(context)
        val usageAccess = usageMonitor.hasUsageAccess()
        val accessibilityEnabled = isDashboardAccessibilityEnabled(context)
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val totalMinutes = if (usageAccess) {
            usageMonitor.queryUsage(todayStart, System.currentTimeMillis()).sumOf { it.totalTimeInForeground } / 60_000L
        } else null
        val screenLimit = ParentalControlStore(context).load().screenTimeLimit
        intelligenceStatus = ParentalStatusEvaluator.overall(usageAccess, accessibilityEnabled, totalMinutes, screenLimit)
    }

    LaunchedEffect(Unit) { refreshDashboard() }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refreshDashboard()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val degraded = components.count { it.status == ProtectionComponentStatus.DEGRADED }
    val active = components.count { it.status == ProtectionComponentStatus.ACTIVE }
    val overall = FamilyDashboardStatusEvaluator.label(
        configured = configured,
        intelligenceStatus = intelligenceStatus,
        degradedComponents = degraded,
        notConfiguredComponents = components.count { it.status == ProtectionComponentStatus.NOT_CONFIGURED }
    )
    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Panel de protección familiar", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(if (configured) "Dispositivo preparado para ${family.child}" else "Completa la configuración familiar para empezar.")
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Estado general", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(overall, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("$active capacidades activas · $degraded con atención")
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { refreshDashboard() }, Modifier.fillMaxWidth()) { Text("Comprobar todas ahora") }
                }
            }
        }
        item { RiskCard(components) }
        item {
            FamilyIntelligenceCard(
                context = context,
                modifier = Modifier.fillMaxWidth(),
                onRecommendationAction = onRecommendationAction
            )
        }
        item { Text("Estado de cada protección", style = MaterialTheme.typography.titleLarge) }
        items(components, key = { it.key }) { component ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val symbol = when (component.status) {
                        ProtectionComponentStatus.ACTIVE -> "🟢"
                        ProtectionComponentStatus.DEGRADED -> "🟠"
                        ProtectionComponentStatus.NOT_CONFIGURED -> "⚪"
                    }
                    val label = when (component.status) {
                        ProtectionComponentStatus.ACTIVE -> "ACTIVA"
                        ProtectionComponentStatus.DEGRADED -> "ATENCIÓN"
                        ProtectionComponentStatus.NOT_CONFIGURED -> "NO CONFIGURADA"
                    }
                    Text("$symbol ${component.name}", style = MaterialTheme.typography.titleMedium)
                    Text(label)
                    Spacer(Modifier.height(4.dp))
                    Text(component.detail)
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Radiografía de hoy", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Las señales se muestran solo cuando existe evidencia suficiente.")
                }
            }
        }
        item { Button(onClick = onOpenReport, Modifier.fillMaxWidth()) { Text("Ver informe diario") } }
    }
}

private fun isDashboardAccessibilityEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/${FamyrexParentalAccessibilityService::class.java.name}"
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

@Composable
fun RealAlertsScreen(context: Context, modifier: Modifier = Modifier) {
    val viewModel: AlertsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = AlertsViewModelFactory(context))
    val alerts by viewModel.alerts.collectAsState()
    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Alertas", style = MaterialTheme.typography.headlineSmall); TextButton(onClick = viewModel::refresh) { Text("Actualizar") } } }
        if (alerts.isEmpty()) item { ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp)) { Text("Sin alertas", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(6.dp)); Text("No hay señales que requieran atención ahora. Esto no significa que se haya demostrado que no exista ningún riesgo.") } } }
        items(alerts, key = { it.id }) { alert ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(alert.title, style = MaterialTheme.typography.titleMedium)
                    Text("${alert.severity.name} · ${alert.lifecycleStatus.name}")
                    Spacer(Modifier.height(6.dp))
                    Text(alert.message)
                    Spacer(Modifier.height(8.dp))
                    Text(alert.date)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { viewModel.updateStatus(alert, AlertLifecycleStatus.REVIEWED) }) { Text("Revisada") }
                        TextButton(onClick = { viewModel.updateStatus(alert, AlertLifecycleStatus.CONFIRMED) }) { Text("Confirmada") }
                        TextButton(onClick = { viewModel.updateStatus(alert, AlertLifecycleStatus.DISMISSED) }) { Text("Falso positivo") }
                        if (alert.lifecycleStatus == AlertLifecycleStatus.CONFIRMED) TextButton(onClick = { viewModel.updateStatus(alert, AlertLifecycleStatus.RESOLVED) }) { Text("Resolver") }
                    }
                }
            }
        }
        item { Text("Las alertas son señales para revisar el contexto, no diagnósticos ni acusaciones.") }
    }
}

private fun loadZones(prefs: android.content.SharedPreferences): List<GeoZone> {
    val raw = prefs.getString("geo_zones", "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split(";").mapNotNull { row -> val parts = row.split("|"); if (parts.size == 4) runCatching { GeoZone(parts[0], parts[1].toDouble(), parts[2].toDouble(), parts[3].toFloat()) }.getOrNull() else null }
}

private fun saveZones(prefs: android.content.SharedPreferences, zones: List<GeoZone>) {
    prefs.edit().putString("geo_zones", zones.joinToString(";") { "${it.name}|${it.latitude}|${it.longitude}|${it.radiusMeters}" }).apply()
}

private fun lastKnownLocation(context: Context): android.location.Location? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = when {
        fine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> return null
    }
    return runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
}

@Composable
fun LocationScreen(context: Context, zones: List<GeoZone>, onZonesChange: (List<GeoZone>) -> Unit, modifier: Modifier = Modifier) {
    var permissionGranted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    var status by remember { mutableStateOf("La ubicación aún no está activada.") }
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("150") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result -> permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true; status = if (permissionGranted) "Permiso concedido. Ya puedes crear una geozona." else "Permiso no concedido." }
    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Ubicación y geozonas", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Las geozonas permiten definir lugares seguros, como casa o colegio. La ubicación se usa solo con permiso visible.") }
        item { Button(onClick = { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, modifier = Modifier.fillMaxWidth()) { Text(if (permissionGranted) "Permiso de ubicación concedido" else "Conceder permiso de ubicación") } }
        item { Text(status) }
        item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre de la geozona") }, placeholder = { Text("Casa, colegio...") }) }
        item { OutlinedTextField(radius, { radius = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), label = { Text("Radio en metros") }) }
        item {
            Button(
                enabled = permissionGranted && name.isNotBlank(),
                onClick = {
                    val location = lastKnownLocation(context)
                    if (location != null) {
                        onZonesChange(zones + GeoZone(name.trim(), location.latitude, location.longitude, radius.toFloatOrNull() ?: 150f))
                        name = ""
                        status = "Geozona guardada con la última ubicación disponible."
                    } else {
                        status = "No hay una ubicación reciente disponible."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar geozona") }
        }
        items(zones) { zone ->
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(zone.name, style = MaterialTheme.typography.titleMedium); Text("Radio: ${zone.radiusMeters.toInt()} m"); Text("${zone.latitude}, ${zone.longitude}") } }
        }
    }
}
