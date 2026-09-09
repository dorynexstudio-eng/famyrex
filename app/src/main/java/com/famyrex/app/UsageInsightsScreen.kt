package com.famyrex.app

import android.app.usage.UsageStats
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

private data class UsageDay(val label: String, val dateKey: String, val minutes: Long)
private data class UsageApp(val label: String, val minutes: Long)

@Composable
fun UsageInsightsScreen(
    context: Context,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenReport: () -> Unit = {}
) {
    var days by remember { mutableStateOf<List<UsageDay>>(emptyList()) }
    var apps by remember { mutableStateOf<List<UsageApp>>(emptyList()) }
    var hasAccess by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }

    fun refresh() {
        val monitor = ParentalUsageMonitor(context)
        hasAccess = monitor.hasUsageAccess()
        if (!hasAccess) {
            days = emptyList(); apps = emptyList(); loading = false; return
        }
        val today = Calendar.getInstance()
        val todayStart = today.startOfDay()
        val live = monitor.queryUsage(todayStart, System.currentTimeMillis())
        apps = live.mapNotNull { stat ->
            val label = runCatching {
                context.packageManager.getApplicationLabel(
                    context.packageManager.getApplicationInfo(stat.packageName, 0)
                ).toString()
            }.getOrNull()
            label?.let { UsageApp(it, stat.totalTimeInForeground / 60_000L) }
        }.filter { it.minutes > 0 }.sortedByDescending { it.minutes }.take(8)

        val history = UsageSnapshotStore(context).loadHistory().associateBy { it.date }
        val result = (6 downTo 0).map { offset ->
            val day = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = day.dateKey()
            val minutes = if (offset == 0) live.sumOf { it.totalTimeInForeground } / 60_000L
            else (history[key]?.totalTimeMs ?: 0L) / 60_000L
            UsageDay(day.shortDayLabel(), key, minutes)
        }
        days = result
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    val totalWeek = days.sumOf { it.minutes }
    val average = if (days.isEmpty()) 0L else totalWeek / days.size
    val todayMinutes = days.lastOrNull()?.minutes ?: 0L
    val previousWeek = UsageSnapshotStore(context).loadHistory()
        .filter { isPreviousWeek(it.date) }
        .sumOf { it.totalTimeMs } / 60_000L
    val trend = if (previousWeek > 0) ((totalWeek - previousWeek) * 100 / previousWeek).toInt() else null
    val peak = days.maxByOrNull { it.minutes }
    val limit = ParentalControlStore(context).load().screenTimeLimit?.dailyMinutes?.toLong()

    Column(modifier = modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Tiempo de uso", style = MaterialTheme.typography.headlineMedium)
                Text("Actividad y tendencias de los últimos 7 días", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, null); Spacer(Modifier.size(4.dp)); Text("Ajustes") }
        }

        if (!hasAccess && !loading) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Necesitamos acceso al uso de aplicaciones", style = MaterialTheme.typography.titleLarge)
                    Text("Famyrex usa la API oficial de Android para calcular el tiempo de uso. Los datos se procesan localmente.")
                    Button(onClick = { ParentalUsageMonitor(context).openUsageAccessSettings() }) { Text("Activar acceso") }
                }
            }
        } else if (!loading) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Hoy", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatUsageMinutes(todayMinutes), style = MaterialTheme.typography.displaySmall)
                    Text(
                        when {
                            limit == null -> "Sin límite diario configurado"
                            todayMinutes <= limit -> "Dentro del límite de ${formatUsageMinutes(limit)}"
                            else -> "${formatUsageMinutes(todayMinutes - limit)} por encima del límite"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Últimos 7 días", style = MaterialTheme.typography.titleMedium)
                            Text("Media ${formatUsageMinutes(average)} al día", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    UsageWeekChart(days)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total: ${formatUsageMinutes(totalWeek)}", style = MaterialTheme.typography.labelLarge)
                        peak?.let { Text("Pico: ${it.label} · ${formatUsageMinutes(it.minutes)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tendencia", style = MaterialTheme.typography.titleMedium)
                    Text(
                        trend?.let {
                            when {
                                it > 5 -> "El uso ha aumentado un ${it}% frente a los 7 días anteriores."
                                it < -5 -> "El uso ha disminuido un ${-it}% frente a los 7 días anteriores."
                                else -> "El uso se mantiene estable frente a los 7 días anteriores."
                            }
                        } ?: "Aún no hay suficientes datos históricos para comparar semanas."
                    )
                    Text("La tendencia describe cambios de actividad; no indica por sí sola si el uso es bueno o malo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (apps.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Más utilizadas hoy", style = MaterialTheme.typography.titleMedium)
                        apps.forEachIndexed { index, app ->
                            UsageAppRow(index + 1, app, apps.first().minutes)
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Histórico", style = MaterialTheme.typography.titleMedium)
                    Text("Famyrex conserva el historial local disponible para detectar cambios de uso y preparar informes diarios y semanales.")
                    OutlinedButton(onClick = onOpenReport, modifier = Modifier.fillMaxWidth()) { Text("Abrir informe familiar") }
                }
            }
        }
    }
}

@Composable
private fun UsageWeekChart(days: List<UsageDay>) {
    val maxMinutes = max(1L, days.maxOfOrNull { it.minutes } ?: 1L)
    Canvas(Modifier.fillMaxWidth().height(170.dp).padding(top = 8.dp)) {
        val gap = 10f
        val barWidth = (size.width - gap * (days.size - 1)) / days.size
        days.forEachIndexed { index, day ->
            val height = (day.minutes.toFloat() / maxMinutes.toFloat()) * (size.height - 32f)
            val left = index * (barWidth + gap)
            val top = size.height - 26f - height
            drawRoundRect(
                color = Color.Gray,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth, height.coerceAtLeast(6f)),
                cornerRadius = CornerRadius(10f, 10f)
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { Text(it.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun UsageAppRow(position: Int, app: UsageApp, maxMinutes: Long) {
    val fraction = if (maxMinutes > 0) (app.minutes.toFloat() / maxMinutes).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$position. ${app.label}")
            Text(formatUsageMinutes(app.minutes), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(Modifier.fillMaxWidth().height(7.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Surface(Modifier.fillMaxWidth(fraction).height(7.dp), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {}
        }
    }
}

private fun Calendar.startOfDay(): Long = apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun Calendar.dateKey(): String = String.format(Locale.US, "%04d-%02d-%02d", get(Calendar.YEAR), get(Calendar.MONTH) + 1, get(Calendar.DAY_OF_MONTH))
private fun Calendar.shortDayLabel(): String = arrayOf("D", "L", "M", "X", "J", "V", "S")[get(Calendar.DAY_OF_WEEK) - 1]
private fun isPreviousWeek(date: String): Boolean {
    val today = Calendar.getInstance()
    val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -13); startOfDay() }
    val end = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7); startOfDay() }
    val key = date.take(10)
    return key >= start.dateKey() && key <= end.dateKey()
}
private fun formatUsageMinutes(minutes: Long): String {
    val hours = minutes / 60; val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}min" else "$mins min"
}
