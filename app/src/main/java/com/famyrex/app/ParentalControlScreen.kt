package com.famyrex.app

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Pantalla de configuración local del control parental de Famyrex. */
@Composable
fun ParentalControlScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { ParentalControlStore(context) }
    var config by remember { mutableStateOf(store.load()) }
    var screenLimitEnabled by remember { mutableStateOf(config.screenTimeLimit?.enabled == true) }
    var screenMinutes by remember { mutableStateOf(config.screenTimeLimit?.dailyMinutes ?: 120) }
    var message by remember { mutableStateOf("") }

    val usageAccess = remember { ParentalUsageMonitor(context).hasUsageAccess() }
    val accessibilityEnabled = remember { isParentalAccessibilityEnabled(context) }
    val apps = remember {
        context.packageManager.getInstalledApplications(0)
            .filter { it.packageName != context.packageName }
            .sortedBy { context.packageManager.getApplicationLabel(it).toString().lowercase() }
            .take(80)
    }

    fun save(next: ParentalControlConfig) {
        store.save(next)
        config = next
        message = "Configuración parental guardada en este dispositivo."
    }

    LazyColumn(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Control parental", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.padding(2.dp))
            Text("Configura límites y pausas directamente en el dispositivo. No se envían datos a un servidor.")
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estado de protección", style = MaterialTheme.typography.titleMedium)
                    Text(if (usageAccess) "🟢 Acceso al uso: activo" else "🟠 Acceso al uso: falta activarlo")
                    Text(if (accessibilityEnabled) "🟢 Guardia parental: activa" else "🟠 Guardia parental: falta activarla")
                    if (!usageAccess) Button(onClick = { openUsageSettings(context) }, Modifier.fillMaxWidth()) { Text("Activar acceso al uso") }
                    if (!accessibilityEnabled) Button(onClick = { openAccessibilitySettings(context) }, Modifier.fillMaxWidth()) { Text("Activar guardia parental") }
                    OutlinedButton(onClick = { openUsageSettings(context) }, Modifier.fillMaxWidth()) { Text("Revisar permisos de control") }
                }
            }
        }

        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tiempo total de pantalla", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (screenLimitEnabled) "Límite activado" else "Límite desactivado")
                        Switch(checked = screenLimitEnabled, onCheckedChange = { enabled ->
                            screenLimitEnabled = enabled
                            save(config.copy(screenTimeLimit = ScreenTimeLimit(screenMinutes, enabled)))
                        })
                    }
                    Text("Límite diario: $screenMinutes minutos")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            screenMinutes = (screenMinutes - 15).coerceAtLeast(15)
                            if (screenLimitEnabled) save(config.copy(screenTimeLimit = ScreenTimeLimit(screenMinutes, true)))
                        }, Modifier.weight(1f)) { Text("−15") }
                        OutlinedButton(onClick = {
                            screenMinutes = (screenMinutes + 15).coerceAtMost(1440)
                            if (screenLimitEnabled) save(config.copy(screenTimeLimit = ScreenTimeLimit(screenMinutes, true)))
                        }, Modifier.weight(1f)) { Text("+15") }
                    }
                }
            }
        }

        item { HorizontalDivider() }
        item { Text("Aplicaciones", style = MaterialTheme.typography.titleLarge) }
        item { Text("Marca una aplicación como bloqueada. Los límites por aplicación se almacenan junto a la configuración familiar.") }

        items(apps, key = { it.packageName }) { app ->
            val restriction = config.appRestrictions.firstOrNull { it.packageName == app.packageName }
            val blocked = restriction?.blocked == true
            val label = context.packageManager.getApplicationLabel(app).toString()
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = blocked, onCheckedChange = { enabled ->
                        val others = config.appRestrictions.filterNot { it.packageName == app.packageName }
                        val updated = others + AppRestriction(
                            packageName = app.packageName,
                            dailyMinutes = restriction?.dailyMinutes,
                            blocked = enabled
                        )
                        save(config.copy(appRestrictions = updated))
                    })
                }
            }
        }

        item {
            Text("Horarios de pausa", style = MaterialTheme.typography.titleLarge)
            Text("La lógica de pausa ya admite horarios que atraviesan medianoche. La edición avanzada de franjas se añadirá en el siguiente paso.")
        }
        item {
            OutlinedButton(onClick = {
                val example = PauseSchedule(startMinuteOfDay = 22 * 60, endMinuteOfDay = 7 * 60, enabled = true)
                save(config.copy(pauseSchedules = listOf(example)))
            }, Modifier.fillMaxWidth()) { Text("Activar pausa nocturna 22:00–07:00") }
        }
        item {
            OutlinedButton(onClick = {
                save(config.copy(pauseSchedules = emptyList()))
            }, Modifier.fillMaxWidth()) { Text("Quitar horarios de pausa") }
        }
        if (message.isNotBlank()) item { Text(message) }
        item { Text("Famyrex no oculta este control: el adulto debe activar explícitamente los permisos de Android y puede revisar la configuración desde el propio dispositivo.") }
    }
}

private fun openUsageSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun isParentalAccessibilityEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/${FamyrexParentalAccessibilityService::class.java.name}"
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
