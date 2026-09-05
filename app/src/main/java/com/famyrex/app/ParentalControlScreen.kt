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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Calendar

/** Pantalla de configuración local del control parental de Famyrex. */
@Composable
fun ParentalControlScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember { ParentalControlStore(context) }
    var config by remember { mutableStateOf(store.load()) }
    var screenLimitEnabled by remember { mutableStateOf(config.screenTimeLimit?.enabled == true) }
    var screenMinutes by remember { mutableStateOf(config.screenTimeLimit?.dailyMinutes ?: 120) }
    var scheduleStart by remember { mutableStateOf("22:00") }
    var scheduleEnd by remember { mutableStateOf("07:00") }
    var scheduleError by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var refreshToken by remember { mutableStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshToken++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val usageMonitor = remember { ParentalUsageMonitor(context) }
    val usageAccess = remember(refreshToken) { usageMonitor.hasUsageAccess() }
    val accessibilityEnabled = remember(refreshToken) { isParentalAccessibilityEnabled(context) }
    val apps = remember {
        context.packageManager.getInstalledApplications(0)
            .filter { it.packageName != context.packageName }
            .sortedBy { context.packageManager.getApplicationLabel(it).toString().lowercase() }
            .take(80)
    }
    val usageByPackage = remember(usageAccess, refreshToken) {
        if (!usageAccess) emptyMap() else {
            usageMonitor.queryUsage(todayStartMs(), System.currentTimeMillis())
                .groupBy { it.packageName }
                .mapValues { (_, stats) -> stats.sumOf { it.totalTimeInForeground } }
        }
    }
    val totalUsageMinutes = if (usageAccess) {
        usageByPackage.values.sum() / 60_000L
    } else null
    val overallStatus = ParentalStatusEvaluator.overall(
        usageAccess = usageAccess,
        accessibilityEnabled = accessibilityEnabled,
        totalUsageMinutes = totalUsageMinutes,
        screenTimeLimit = config.screenTimeLimit
    )

    fun save(next: ParentalControlConfig) {
        store.save(next)
        config = next
        message = "Configuración parental guardada en este dispositivo."
    }

    fun updateAppRestriction(packageName: String, dailyMinutes: Int?, blocked: Boolean) {
        val others = config.appRestrictions.filterNot { it.packageName == packageName }
        val updated = others + AppRestriction(
            packageName = packageName,
            dailyMinutes = dailyMinutes,
            blocked = blocked
        )
        save(config.copy(appRestrictions = updated))
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
                    Text("${overallStatus.icon} ${overallStatus.label}", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        when (overallStatus) {
                            ParentalStatus.WHITE -> "Famyrex todavía no tiene todos los datos o permisos necesarios para valorar la protección."
                            ParentalStatus.RED -> "Se ha alcanzado un límite configurado. Revisa el uso antes de decidir qué hacer."
                            ParentalStatus.ORANGE -> "El uso se acerca a un límite configurado. Conviene revisarlo."
                            ParentalStatus.GREEN -> "No se ha alcanzado ningún límite global configurado con los datos disponibles."
                        }
                    )
                    if (totalUsageMinutes != null) {
                        Text("Uso de pantalla hoy: $totalUsageMinutes min")
                    }
                    Text(if (usageAccess) "🟢 Datos de uso disponibles" else "⚪ Datos de uso no disponibles")
                    Text(if (accessibilityEnabled) "🟢 Guardia parental activa" else "🟠 Guardia parental no activa")
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
        item { Text("Puedes bloquear una aplicación o asignarle un límite diario independiente. El estado se basa en el uso real disponible en Android.") }

        items(apps, key = { it.packageName }) { app ->
            val restriction = config.appRestrictions.firstOrNull { it.packageName == app.packageName }
            val blocked = restriction?.blocked == true
            val currentLimit = restriction?.dailyMinutes
            val label = context.packageManager.getApplicationLabel(app).toString()
            val usedMinutes = usageByPackage[app.packageName]?.div(60_000L)
            val appStatus = ParentalStatusEvaluator.app(usageAccess, usedMinutes, restriction)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = blocked, onCheckedChange = { enabled ->
                            updateAppRestriction(app.packageName, currentLimit, enabled)
                        })
                    }
                    Text("${appStatus.icon} ${appStatus.label}")
                    Text(
                        when {
                            usedMinutes != null && currentLimit != null -> "Uso hoy: $usedMinutes min de $currentLimit min"
                            usedMinutes != null -> "Uso hoy: $usedMinutes min · Sin límite específico"
                            !usageAccess -> "Uso hoy: ⚪ sin datos"
                            else -> "Uso hoy: 0 min"
                        }
                    )
                    if (blocked) Text("🚫 Aplicación bloqueada")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val next = (currentLimit ?: 60).let { (it - 15).coerceAtLeast(15) }
                                updateAppRestriction(app.packageName, next, blocked)
                            },
                            Modifier.weight(1f)
                        ) { Text("−15 min") }
                        OutlinedButton(
                            onClick = {
                                val next = (currentLimit ?: 45).let { (it + 15).coerceAtMost(1440) }
                                updateAppRestriction(app.packageName, next, blocked)
                            },
                            Modifier.weight(1f)
                        ) { Text("+15 min") }
                        OutlinedButton(
                            onClick = { updateAppRestriction(app.packageName, null, blocked) },
                            Modifier.weight(1f)
                        ) { Text("Sin límite") }
                    }
                }
            }
        }

        item {
            Text("Horarios de pausa", style = MaterialTheme.typography.titleLarge)
            Text("Añade varias franjas. También se admiten horarios que atraviesan medianoche, como 22:00–07:00.")
        }
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nueva franja", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = scheduleStart,
                            onValueChange = { scheduleStart = it.take(5) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Inicio HH:mm") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = scheduleEnd,
                            onValueChange = { scheduleEnd = it.take(5) },
                            modifier = Modifier.weight(1f),
                            label = { Text("Fin HH:mm") },
                            singleLine = true
                        )
                    }
                    if (scheduleError.isNotBlank()) Text(scheduleError, color = MaterialTheme.colorScheme.error)
                    Button(onClick = {
                        val start = parseMinuteOfDay(scheduleStart)
                        val end = parseMinuteOfDay(scheduleEnd)
                        if (start == null || end == null) {
                            scheduleError = "Usa el formato HH:mm, por ejemplo 22:00."
                        } else {
                            scheduleError = ""
                            save(config.copy(
                                pauseSchedules = config.pauseSchedules + PauseSchedule(start, end, true)
                            ))
                        }
                    }, Modifier.fillMaxWidth()) { Text("Añadir horario") }
                }
            }
        }

        items(config.pauseSchedules.indices.toList()) { index ->
            val schedule = config.pauseSchedules[index]
            ElevatedCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("🌙 ${formatMinuteOfDay(schedule.startMinuteOfDay)}–${formatMinuteOfDay(schedule.endMinuteOfDay)}")
                        Text(if (schedule.enabled) "Pausa activa" else "Pausa desactivada")
                    }
                    OutlinedButton(onClick = {
                        save(config.copy(pauseSchedules = config.pauseSchedules.filterIndexed { i, _ -> i != index }))
                    }) { Text("Quitar") }
                }
            }
        }

        if (message.isNotBlank()) item { Text(message) }
        item { Text("Famyrex no oculta este control: el adulto debe activar explícitamente los permisos de Android y puede revisar la configuración desde el propio dispositivo.") }
    }
}

private fun todayStartMs(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun parseMinuteOfDay(value: String): Int? {
    val parts = value.trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun formatMinuteOfDay(value: Int): String {
    val hour = (value / 60).coerceIn(0, 23)
    val minute = (value % 60).coerceIn(0, 59)
    return "%02d:%02d".format(hour, minute)
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
