package com.famyrex.app

import android.app.usage.UsageStats
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Calendar

/** Resumen visible del estado digital familiar, construido solo con señales locales disponibles. */
@Composable
fun FamilyIntelligenceCard(
    context: Context,
    modifier: Modifier = Modifier,
    onRecommendationAction: (FamilyIntelligenceRecommendationDestination) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var summary by remember { mutableStateOf<FamilyIntelligenceSummary?>(null) }
    var trend by remember { mutableStateOf<FamilyUsageTrend?>(null) }
    var alerts by remember { mutableStateOf<List<SmartAlert>>(emptyList()) }
    var incidents by remember { mutableStateOf<List<CommunicationRiskIncident>>(emptyList()) }

    fun refresh() {
        val usageMonitor = ParentalUsageMonitor(context)
        val usageAccess = usageMonitor.hasUsageAccess()
        val accessibilityEnabled = isFamilyIntelligenceAccessibilityEnabled(context)
        val todayStart = todayStartForFamilyIntelligence()
        val now = System.currentTimeMillis()
        val totalMinutes = if (usageAccess) {
            usageMonitor.queryUsage(todayStart, now).sumOf { it.totalTimeInForeground } / 60_000L
        } else null
        val screenLimit = ParentalControlStore(context).load().screenTimeLimit
        val parentalStatus = ParentalStatusEvaluator.overall(usageAccess, accessibilityEnabled, totalMinutes, screenLimit)
        val currentAlerts = AlertStore(context).load()
        alerts = currentAlerts
        val currentIncidents = CommunicationRiskIncidentStore(context).load()
            .filter { it.status !in setOf(RiskIncidentStatus.DISMISSED, RiskIncidentStatus.AUTO_DISMISSED, RiskIncidentStatus.RESOLVED) }
            .sortedWith(compareByDescending<CommunicationRiskIncident> { it.createdAtMs }.thenBy { it.id })
        incidents = currentIncidents
        val communicationAlertCount = currentIncidents.size
        summary = FamilyIntelligenceAggregator.summarize(
            parentalStatus,
            totalMinutes,
            communicationAlertCount,
            usageAccess,
            accessibilityEnabled
        )
        trend = if (usageAccess) FamilyUsageTrendEvaluator.evaluate(loadRecentDailyMinutes(usageMonitor, todayStart)) else null
    }

    LaunchedEffect(Unit) { refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val current = summary ?: return
    val status = current.parentalStatus
    val recommendation = FamilyIntelligenceRecommendationEngine.recommend(alerts)
    val evidence = FamilyIntelligenceEvidenceBuilder.build(current, trend, incidents)

    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Centro de inteligencia familiar", style = MaterialTheme.typography.titleLarge)
            Text("${status.icon} ${status.label}", style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(current.totalScreenMinutes?.let { "Pantalla hoy: ${formatFamilyMinutes(it)}" } ?: "Pantalla hoy: ⚪ sin datos", Modifier.weight(1f))
                Text(if (current.communicationAlertCount == 0) "Comunicaciones: 0" else "Comunicaciones: ${current.communicationAlertCount}", Modifier.weight(1f))
            }

            trend?.let { usageTrend ->
                val trendText = when (usageTrend.direction) {
                    FamilyUsageTrendDirection.INCREASING -> "↗ Uso en aumento"
                    FamilyUsageTrendDirection.DECREASING -> "↘ Uso a la baja"
                    FamilyUsageTrendDirection.STABLE -> "→ Uso estable"
                    FamilyUsageTrendDirection.INSUFFICIENT_DATA -> "⚪ Tendencia: faltan datos"
                }
                Text("Tendencia · $trendText", style = MaterialTheme.typography.titleSmall)
                FamilyUsageWeekChart(usageTrend.days)
                usageTrend.anomaly?.let { anomaly ->
                    Text(when (anomaly.type) {
                        FamilyUsageAnomalyType.HIGH -> "⚠️ Hoy el uso está ${anomaly.deviationPercent}% por encima de la media anterior."
                        FamilyUsageAnomalyType.LOW -> "ℹ️ Hoy el uso está ${anomaly.deviationPercent}% por debajo de la media anterior."
                    })
                }
                usageTrend.previousAverageMinutes?.let { average ->
                    Text("Referencia: ${formatFamilyMinutes(average.toLong())} diarios de media en los días anteriores.")
                } ?: Text("Necesitamos al menos 2 días de datos para comparar la evolución.")
            }

            Spacer(Modifier.height(2.dp))
            Text(FamilyIntelligenceExplanation.explain(current, trend), style = MaterialTheme.typography.bodyLarge)

            if (evidence.isNotEmpty()) {
                Text("Por qué", style = MaterialTheme.typography.titleSmall)
                evidence.take(5).forEach { item ->
                    Text("• ${item.signal} → ${item.conclusion}", style = MaterialTheme.typography.bodyMedium)
                    item.referenceId?.let { referenceId ->
                        Text("  Incidente: $referenceId${item.incidentStatus?.let { " · ${formatIncidentStatus(it)}" } ?: ""}", style = MaterialTheme.typography.labelMedium)
                    }
                    Text("  Acción: ${item.action}", style = MaterialTheme.typography.bodySmall)
                }
            }

            recommendation?.let { currentRecommendation ->
                Text(currentRecommendation.action, style = MaterialTheme.typography.bodyMedium)
                if (currentRecommendation.destination != FamilyIntelligenceRecommendationDestination.OBSERVE) {
                    Button(
                        onClick = { onRecommendationAction(currentRecommendation.destination) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(currentRecommendation.title) }
                }
            }
            current.reasons.take(3).forEach { reason -> Text("• $reason") }
        }
    }
}

private fun formatIncidentStatus(status: RiskIncidentStatus): String = when (status) {
    RiskIncidentStatus.DETECTED -> "Detectado"
    RiskIncidentStatus.REVIEWED -> "Revisado"
    RiskIncidentStatus.CONFIRMED -> "Confirmado"
    RiskIncidentStatus.DISMISSED -> "Descartado"
    RiskIncidentStatus.AUTO_DISMISSED -> "Descartado automáticamente"
    RiskIncidentStatus.RESOLVED -> "Resuelto"
}

@Composable
private fun FamilyUsageWeekChart(days: List<Long>) {
    if (days.isEmpty()) return
    val barColor = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Uso de los últimos 7 días", style = MaterialTheme.typography.labelLarge)
        Canvas(Modifier.fillMaxWidth().height(100.dp)) { drawFamilyUsageBars(days, barColor) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.indices.forEach { index -> Text(if (index == days.lastIndex) "Hoy" else "-${days.lastIndex - index}", style = MaterialTheme.typography.labelSmall) }
        }
    }
}

private fun DrawScope.drawFamilyUsageBars(days: List<Long>, barColor: Color) {
    val maxMinutes = maxOf(days.maxOrNull() ?: 0L, 1L).toFloat()
    val slotWidth = size.width / days.size
    val barWidth = slotWidth * 0.62f
    days.forEachIndexed { index, minutes ->
        val height = (minutes.toFloat() / maxMinutes) * size.height
        val left = index * slotWidth + (slotWidth - barWidth) / 2f
        drawRoundRect(color = barColor, topLeft = Offset(left, size.height - height), size = Size(barWidth, height.coerceAtLeast(2f)), cornerRadius = CornerRadius(6f, 6f))
    }
}

private fun loadRecentDailyMinutes(usageMonitor: ParentalUsageMonitor, todayStart: Long): List<Long> {
    val calendar = Calendar.getInstance().apply { timeInMillis = todayStart }
    return (6 downTo 0).map { daysAgo ->
        val start = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }.timeInMillis
        val end = if (daysAgo == 0) System.currentTimeMillis() else (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysAgo + 1) }.timeInMillis
        usageMonitor.queryUsage(start, end).sumOf(UsageStats::getTotalTimeInForeground) / 60_000L
    }
}

private fun todayStartForFamilyIntelligence(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun formatFamilyMinutes(minutes: Long): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return if (hours > 0) "${hours} h ${remaining} min" else "$remaining min"
}

private fun isFamilyIntelligenceAccessibilityEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/${FamyrexParentalAccessibilityService::class.java.name}"
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}
