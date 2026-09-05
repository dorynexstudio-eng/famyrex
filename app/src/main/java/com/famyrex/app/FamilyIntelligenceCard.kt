package com.famyrex.app

import android.app.usage.UsageStats
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Calendar

/** Resumen visible del estado digital familiar, construido solo con señales locales disponibles. */
@Composable
fun FamilyIntelligenceCard(context: Context, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var summary by remember { mutableStateOf<FamilyIntelligenceSummary?>(null) }
    var trend by remember { mutableStateOf<FamilyUsageTrend?>(null) }

    fun refresh() {
        val usageMonitor = ParentalUsageMonitor(context)
        val usageAccess = usageMonitor.hasUsageAccess()
        val accessibilityEnabled = isFamilyIntelligenceAccessibilityEnabled(context)
        val todayStart = todayStartForFamilyIntelligence()
        val now = System.currentTimeMillis()
        val totalMinutes = if (usageAccess) {
            usageMonitor.queryUsage(todayStart, now)
                .sumOf { it.totalTimeInForeground } / 60_000L
        } else null
        val screenLimit = ParentalControlStore(context).load().screenTimeLimit
        val parentalStatus = ParentalStatusEvaluator.overall(
            usageAccess = usageAccess,
            accessibilityEnabled = accessibilityEnabled,
            totalUsageMinutes = totalMinutes,
            screenTimeLimit = screenLimit
        )
        val communicationAlertCount = AlertStore(context).load().count { alert ->
            alert.type == AlertType.COMMUNICATION_RISK &&
                alert.lifecycleStatus !in setOf(
                    AlertLifecycleStatus.DISMISSED,
                    AlertLifecycleStatus.AUTO_DISMISSED,
                    AlertLifecycleStatus.RESOLVED
                )
        }
        summary = FamilyIntelligenceAggregator.summarize(
            parentalStatus = parentalStatus,
            totalScreenMinutes = totalMinutes,
            communicationAlertCount = communicationAlertCount,
            usageAccess = usageAccess,
            accessibilityEnabled = accessibilityEnabled
        )
        trend = if (usageAccess) {
            FamilyUsageTrendEvaluator.evaluate(loadRecentDailyMinutes(usageMonitor, todayStart))
        } else null
    }

    LaunchedEffect(Unit) { refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val current = summary ?: return
    val status = current.parentalStatus
    val statusText = "${status.icon} ${status.label}"

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Centro de inteligencia familiar", style = MaterialTheme.typography.titleLarge)
            Text(statusText, style = MaterialTheme.typography.headlineSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    current.totalScreenMinutes?.let { "Pantalla hoy: ${formatFamilyMinutes(it)}" } ?: "Pantalla hoy: ⚪ sin datos",
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (current.communicationAlertCount == 0) "Comunicaciones: 0" else "Comunicaciones: ${current.communicationAlertCount}",
                    modifier = Modifier.weight(1f)
                )
            }

            trend?.let { usageTrend ->
                val trendText = when (usageTrend.direction) {
                    FamilyUsageTrendDirection.INCREASING -> "↗ Uso en aumento"
                    FamilyUsageTrendDirection.DECREASING -> "↘ Uso a la baja"
                    FamilyUsageTrendDirection.STABLE -> "→ Uso estable"
                    FamilyUsageTrendDirection.INSUFFICIENT_DATA -> "⚪ Tendencia: faltan datos"
                }
                Text("Tendencia · $trendText", style = MaterialTheme.typography.titleSmall)
                usageTrend.previousAverageMinutes?.let { average ->
                    Text("Referencia: ${formatFamilyMinutes(average.toLong())} diarios de media en los días anteriores.")
                } ?: Text("Necesitamos al menos 2 días de datos para comparar la evolución.")
            }

            Spacer(Modifier.height(2.dp))
            Text(
                when {
                    current.actionRequired -> "Hay señales que conviene revisar hoy."
                    status == ParentalStatus.WHITE -> "Famyrex aún no tiene evidencia suficiente para valorar todo el estado."
                    else -> "No hay señales que requieran acción inmediata."
                }
            )

            current.reasons.take(3).forEach { reason ->
                Text("• $reason")
            }
        }
    }
}

private fun loadRecentDailyMinutes(usageMonitor: ParentalUsageMonitor, todayStart: Long): List<Long> {
    val calendar = Calendar.getInstance().apply { timeInMillis = todayStart }
    return (6 downTo 0).map { daysAgo ->
        val start = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }.timeInMillis
        val end = if (daysAgo == 0) System.currentTimeMillis() else {
            (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo + 1) }.timeInMillis
        }
        usageMonitor.queryUsage(start, end).sumOf(UsageStats::getTotalTimeInForeground) / 60_000L
    }
}

private fun todayStartForFamilyIntelligence(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatFamilyMinutes(minutes: Long): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return if (hours > 0) "${hours} h ${remaining} min" else "$remaining min"
}

private fun isFamilyIntelligenceAccessibilityEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/${FamyrexParentalAccessibilityService::class.java.name}"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
