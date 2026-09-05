package com.famyrex.app

import java.util.Calendar
import kotlin.math.roundToInt

object AlertEngine {

    fun evaluate(
        history: List<DailyUsage>,
        intervals: List<UsageInterval>,
        settings: ProtectionSettings = ProtectionSettings()
    ): List<SmartAlert> {
        if (history.isEmpty()) return emptyList()

        val today = history.maxByOrNull { it.date } ?: return emptyList()
        val alerts = mutableListOf<SmartAlert>()
        val todayMinutes = (today.totalTimeMs / 60_000L)

        if (todayMinutes >= settings.dailyMinutesThreshold) {
            val severity = if (todayMinutes >= settings.dailyMinutesThreshold * 1.5) {
                AlertSeverity.IMPORTANT
            } else AlertSeverity.ATTENTION
            alerts += SmartAlert(
                id = "daily_limit_${today.date}",
                type = AlertType.DAILY_LIMIT,
                severity = severity,
                title = "Límite diario superado",
                message = "El uso de hoy es de ${todayMinutes} min, por encima del umbral configurado de ${settings.dailyMinutesThreshold} min.",
                date = today.date
            )
        }

        val recentByApp = history.dropLast(1).takeLast(6)
        if (recentByApp.size >= 3) {
            val currentApps = today.topApps.associate { it.packageName to it.totalTimeMs }
            currentApps.forEach { (pkg, currentMs) ->
                val previous = recentByApp.mapNotNull { day ->
                    day.topApps.firstOrNull { it.packageName == pkg }?.totalTimeMs
                }
                if (previous.size >= 3) {
                    val baseline = previous.average()
                    if (baseline > 0) {
                        val percent = ((currentMs / baseline) - 1.0) * 100.0
                        if (percent >= settings.appSpikePercent) {
                            val name = today.topApps.firstOrNull { it.packageName == pkg }?.label ?: pkg
                            alerts += SmartAlert(
                                id = "spike_${today.date}_$pkg",
                                type = AlertType.APP_SPIKE,
                                severity = if (percent >= settings.appSpikePercent * 1.75) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION,
                                title = "Aumento de uso en $name",
                                message = "El uso está aproximadamente un ${percent.roundToInt()}% por encima de la referencia reciente.",
                                date = today.date,
                                packageName = pkg
                            )
                        }
                    }
                }
            }
        }

        if (history.size >= 7) {
            val recent = history.takeLast(4).map { it.totalTimeMs }.average()
            val older = history.dropLast(4).takeLast(6).map { it.totalTimeMs }.average()
            if (older > 0 && recent >= older * 1.35) {
                alerts += SmartAlert(
                    id = "pattern_${today.date}",
                    type = AlertType.PATTERN_CHANGE,
                    severity = AlertSeverity.ATTENTION,
                    title = "Cambio sostenido de patrón",
                    message = "El promedio de uso de los últimos días es significativamente superior a la referencia anterior.",
                    date = today.date
                )
            }
        }

        val nightMinutes = intervals
            .filter { isInNightWindow(it.timestampMs, settings.nightStartMinutes, settings.nightEndMinutes) }
            .sumOf { it.totalTimeMs } / 60_000L

        if (nightMinutes >= settings.nightMinutesThreshold) {
            alerts += SmartAlert(
                id = "night_${today.date}",
                type = AlertType.NIGHT_USE,
                severity = if (nightMinutes >= settings.nightMinutesThreshold * 2) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION,
                title = "Uso durante horario nocturno",
                message = "Se han detectado aproximadamente $nightMinutes min de uso durante el horario nocturno configurado.",
                date = today.date
            )
        }

        return limitAlerts(alerts.distinctBy { it.id })
    }

    /** Keeps the display cap while never evicting IMPORTANT alerts in favor of lower-severity ones. */
    internal fun limitAlerts(alerts: List<SmartAlert>, maxSize: Int = 20): List<SmartAlert> {
        if (alerts.size <= maxSize) return alerts
        val important = alerts.filter { it.severity == AlertSeverity.IMPORTANT }
        val remaining = alerts.filter { it.severity != AlertSeverity.IMPORTANT }
        if (important.size >= maxSize) return important.takeLast(maxSize)
        return important + remaining.takeLast(maxSize - important.size)
    }

    private fun isInNightWindow(timestampMs: Long, start: Int, end: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (start <= end) {
            minutes in start until end
        } else {
            minutes >= start || minutes < end
        }
    }
}
