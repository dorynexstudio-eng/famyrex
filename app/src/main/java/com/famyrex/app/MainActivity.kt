package com.famyrex.app

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.Column

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf

import androidx.compose.material3.TextButton

import androidx.compose.material3.Slider

import androidx.compose.material3.Card

import androidx.compose.material3.Button

import android.content.Context
import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private const val PREFS = "famyrex_prefs"
private const val KEY_PARENT = "parent_name"
private const val KEY_CHILD = "child_name"
private const val KEY_CODE = "link_code"

data class Alert(val title: String, val detail: String, val level: String)

data class FamilyState(val parent: String, val child: String, val code: String)

data class GeoZone(val name: String, val latitude: Double, val longitude: Double, val radiusMeters: Float)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FamyrexApp(applicationContext) }
    }
}

@Composable
fun FamyrexApp(context: Context) {
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var tab by remember { mutableIntStateOf(0) }
    var family by remember { mutableStateOf(loadFamily(prefs)) }
    var zones by remember { mutableStateOf(loadZones(prefs)) }

    val alerts = remember {
        listOf(
            Alert("Uso nocturno elevado", "Actividad fuera del horario configurado. Revisa el contexto antes de intervenir.", "Atención"),
            Alert("Señal potencialmente preocupante", "Famyrex recomienda revisar la situación de forma calmada.", "Preocupante")
        )
    }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Famyrex") }) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(tab == 0, { tab = 0 }, icon = {}, label = { Text("Inicio") })
                    NavigationBarItem(tab == 1, { tab = 1 }, icon = {}, label = { Text("Alertas") })
                    NavigationBarItem(tab == 2, { tab = 2 }, icon = {}, label = { Text("Familia") })
                    NavigationBarItem(tab == 3, { tab = 3 }, icon = {}, label = { Text("Ubicación") })
                    NavigationBarItem(tab == 4, { tab = 4 }, icon = {}, label = { Text("Asistente") })
                }
            }
        ) { padding ->
            when (tab) {
                0 -> Dashboard(family, Modifier.padding(padding))
                1 -> AlertsScreen(alerts, Modifier.padding(padding))
                2 -> FamilyScreen(family, { state ->
                    family = state
                    saveFamily(prefs, state)
                }, Modifier.padding(padding))
                3 -> LocationScreen(context, zones, { updated ->
                    zones = updated
                    saveZones(prefs, updated)
                }, Modifier.padding(padding))
                else -> FamilyAssistantScreen(context, Modifier.padding(padding))
            }
        }
    }
}

private fun loadFamily(prefs: android.content.SharedPreferences): FamilyState = FamilyState(
    prefs.getString(KEY_PARENT, "") ?: "",
    prefs.getString(KEY_CHILD, "") ?: "",
    prefs.getString(KEY_CODE, "") ?: ""
)

private fun saveFamily(prefs: android.content.SharedPreferences, state: FamilyState) {
    prefs.edit().putString(KEY_PARENT, state.parent).putString(KEY_CHILD, state.child).putString(KEY_CODE, state.code).apply()
}

@Composable
fun Dashboard(family: FamilyState, modifier: Modifier = Modifier) {
    val configured = family.parent.isNotBlank() && family.child.isNotBlank()
    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Panel de protección familiar", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(if (configured) "Dispositivo preparado para ${family.child}" else "Completa la configuración familiar para empezar.")
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Estado de protección", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(if (configured) "ACTIVO" else "PENDIENTE", style = MaterialTheme.typography.displaySmall)
                    Text(if (configured) "Famyrex está listo para las funciones autorizadas." else "Configura un adulto y un perfil protegido.")
                }
            }
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Radiografía de hoy", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Uso de pantalla: pendiente de permiso")
                    Text("Ubicación: pendiente de permiso")
                    Text("Señales: 0 confirmadas")
                }
            }
        }
        item { Button(onClick = {}, Modifier.fillMaxWidth()) { Text("Ver informe diario") } }
    }
}

@Composable
fun AlertsScreen(alerts: List<Alert>, modifier: Modifier = Modifier) {
    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Alertas", style = MaterialTheme.typography.headlineSmall) }
        items(alerts) { alert ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(alert.title, style = MaterialTheme.typography.titleMedium)
                    Text(alert.level)
                    Spacer(Modifier.height(6.dp))
                    Text(alert.detail)
                }
            }
        }
        item { Text("Las alertas son señales para revisar el contexto, no diagnósticos.") }
    }
}

@Composable
fun FamilyScreen(family: FamilyState, onSave: (FamilyState) -> Unit, modifier: Modifier = Modifier) {
    var parent by remember(family.parent) { mutableStateOf(family.parent) }
    var child by remember(family.child) { mutableStateOf(family.child) }
    var code by remember(family.code) { mutableStateOf(family.code) }
    var message by remember { mutableStateOf("") }

    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Familia", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Configura el vínculo de forma transparente y con los permisos correspondientes.") }
        item { OutlinedTextField(parent, { parent = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del adulto") }) }
        item { OutlinedTextField(child, { child = it }, Modifier.fillMaxWidth(), label = { Text("Nombre del perfil protegido") }) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(code, { code = it.uppercase().take(8) }, Modifier.weight(1f), label = { Text("Código de vinculación") })
                OutlinedButton(onClick = { code = randomCode(); message = "Código generado. Compártelo únicamente con la persona autorizada." }) { Text("Generar") }
            }
        }
        item {
            Button(onClick = {
                if (parent.isBlank() || child.isBlank() || code.length < 6) message = "Completa los campos y usa un código de 6–8 caracteres."
                else { onSave(FamilyState(parent.trim(), child.trim(), code)); message = "Familia guardada correctamente." }
            }, Modifier.fillMaxWidth()) { Text("Guardar configuración") }
        }
        if (message.isNotBlank()) item { Text(message) }
        item { HorizontalDivider() }
        item { Text("Privacidad", style = MaterialTheme.typography.titleMedium) }
        item { Text("Famyrex está diseñado para trabajar con consentimiento, permisos visibles y las APIs oficiales de Android. No incluye lectura secreta de chats ni grabación oculta.") }
    }
}

private fun loadZones(prefs: android.content.SharedPreferences): List<GeoZone> {
    val raw = prefs.getString("geo_zones", "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split(";").mapNotNull { row ->
        val parts = row.split("|")
        if (parts.size == 4) runCatching { GeoZone(parts[0], parts[1].toDouble(), parts[2].toDouble(), parts[3].toFloat()) }.getOrNull() else null
    }
}

private fun saveZones(prefs: android.content.SharedPreferences, zones: List<GeoZone>) {
    val raw = zones.joinToString(";") { "${it.name}|${it.latitude}|${it.longitude}|${it.radiusMeters}" }
    prefs.edit().putString("geo_zones", raw).apply()
}

@Composable
fun LocationScreen(context: Context, zones: List<GeoZone>, onZonesChange: (List<GeoZone>) -> Unit, modifier: Modifier = Modifier) {
    var permissionGranted by remember { mutableStateOf(androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) }
    var status by remember { mutableStateOf("La ubicación aún no está activada.") }
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("150") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        status = if (permissionGranted) "Permiso concedido. Ya puedes crear una geozona." else "Permiso no concedido."
    }
    LazyColumn(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Ubicación y geozonas", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Las geozonas permiten definir lugares seguros, como casa o colegio. La ubicación se usa solo con permiso visible.") }
        item {
            Button(onClick = { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }, Modifier.fillMaxWidth()) {
                Text(if (permissionGranted) "Permiso de ubicación concedido" else "Conceder permiso de ubicación")
            }
        }
        item { Text(status) }
        item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Nombre de la geozona") }, placeholder = { Text("Casa, colegio...") }) }
        item { OutlinedTextField(radius, { radius = it.filter(Char::isDigit).take(5) }, Modifier.fillMaxWidth(), label = { Text("Radio en metros") }) }
        item {
            Button(enabled = permissionGranted && name.isNotBlank(), onClick = {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val provider = when { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER else -> LocationManager.NETWORK_PROVIDER }
                val location = runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
                if (location != null) {
                    onZonesChange(zones + GeoZone(name.trim(), location.latitude, location.longitude, radius.toFloatOrNull() ?: 150f))
                    name = ""
                    status = "Geozona guardada con la última ubicación disponible."
                } else status = "No hay una ubicación disponible todavía. Activa el GPS y vuelve a intentarlo."
            }, Modifier.fillMaxWidth()) { Text("Guardar geozona en mi ubicación actual") }
        }
        item { Text("Geozonas guardadas", style = MaterialTheme.typography.titleMedium) }
        if (zones.isEmpty()) item { Text("Todavía no hay geozonas configuradas.") }
        items(zones) { zone ->
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(zone.name, style = MaterialTheme.typography.titleMedium)
                    Text("Radio: ${zone.radiusMeters.toInt()} m")
                    Text("Coordenadas: %.5f, %.5f".format(zone.latitude, zone.longitude))
                    TextButton(onClick = { onZonesChange(zones - zone) }) { Text("Eliminar") }
                }
            }
        }
    }
}

private fun randomCode(): String = buildString {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    repeat(8) { append(chars[Random.nextInt(chars.length)]) }
}


@Composable
private fun RiskCard(assessment: RiskAssessment) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Nivel de atención")
            Text(
                text = "${assessment.level.name} · ${assessment.score}/100",
                style = MaterialTheme.typography.titleLarge
            )
            assessment.reasons.take(4).forEach { reason ->
                Text("• $reason")
            }
        }
    }
}
