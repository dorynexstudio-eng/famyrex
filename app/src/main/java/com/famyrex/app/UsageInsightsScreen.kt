package com.famyrex.app

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun UsageInsightsScreen(
    context: Context,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenReport: () -> Unit = {}
) {
    var hasAccess by remember { mutableStateOf(false) }
    var totalMinutes by remember { mutableStateOf(0L) }
    var topApps by remember { mutableStateOf<List<Pair<String, Long>>>(emptyList()) }

    fun refresh() {
        val monitor = ParentalUsageMonitor(context)
        hasAccess = monitor.hasUsageAccess()
        if (!hasAccess) return
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val stats = monitor.queryUsage(start, System.currentTimeMillis())
        totalMinutes = stats.sumOf { it.totalTimeInForeground } / 60_000L
        topApps = stats.take(6).mapNotNull { stat ->
            runCatching {
                context.packageManager.getApplicationLabel(
                    context.packageManager.getApplicationInfo(stat.packageName, 0)
                ).toString() to (stat.totalTimeInForeground / 60_000L)
            }.getOrNull()
        }
    }

    LaunchedEffect(Unit) { refresh() }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Tiempo de uso", style = MaterialTheme.typography.headlineMedium)
                Text("Actividad y tendencias de forma clara y familiar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Hoy", style = MaterialTheme.typography.labelLarge)
                    Text(formatUsageDuration(totalMinutes), style = MaterialTheme.typography.displaySmall)
                    Text(if (hasAccess) "Tiempo total registrado en el dispositivo" else "Necesitamos permiso de acceso a datos de uso")
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Más utilizadas", style = MaterialTheme.typography.titleMedium)
                    if (topApps.isEmpty()) {
                        Text("Todavía no hay actividad suficiente para mostrar aplicaciones.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        val max = topApps.maxOf { it.second }.coerceAtLeast(1L)
                        topApps.forEach { (name, minutes) ->
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(name, maxLines = 1, modifier = Modifier.weight(1f))
                                    Text(formatUsageDuration(minutes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { minutes.toFloat() / max.toFloat() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Histórico", style = MaterialTheme.typography.titleMedium)
                    Text("El histórico diario y semanal se construye con los registros locales de Famyrex.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onOpenReport, modifier = Modifier.fillMaxWidth()) { Text("Ver informe completo") }
                }
            }
        }
        item {
            if (!hasAccess) {
                OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Dar acceso a datos de uso")
                }
            } else {
                OutlinedButton(onClick = { refresh() }, modifier = Modifier.fillMaxWidth()) { Text("Actualizar") }
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

private fun formatUsageDuration(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return if (hours > 0) "${hours}h ${mins}min" else "$mins min"
}
