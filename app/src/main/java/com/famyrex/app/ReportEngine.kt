package com.famyrex.app

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

object ReportEngine {

    fun build(
        history: List<DailyUsage>,
        alerts: List<SmartAlert>,
        period: ReportPeriod,
        today: LocalDate = LocalDate.now()
    ): UsageReport {
        val days = when (period) {
            ReportPeriod.DAILY -> 1L
            ReportPeriod.WEEKLY -> 7L
            ReportPeriod.MONTHLY -> 30L
        }

        val end = today
        val start = end.minusDays(days - 1)
        val selected = history.filter {
            val date = runCatching { LocalDate.parse(it.date) }.getOrNull()
            date != null && !date.isBefore(start) && !date.isAfter(end)
        }.sortedBy { it.date }

        val totalMs = selected.sumOf { it.totalTimeMs }
        val totalMinutes = totalMs / 60_000L
        val average = if (selected.isEmpty()) 0L else totalMinutes / selected.size

        val peak = selected.maxByOrNull { it.totalTimeMs }
        val appMap = linkedMapOf<String, ReportAppUsage>()
        selected.flatMap { it.topApps }.forEach { app ->
            val key = app.packageName
            val old = appMap[key]
            appMap[key] = ReportAppUsage(
                label = app.label,
                packageName = key,
                totalMinutes = (old?.totalMinutes ?: 0L) + app.totalTimeMs / 60_000L
            )
        }

        val periodAlerts = alerts.filter { alert ->
            val d = runCatching { LocalDate.parse(alert.date) }.getOrNull()
            d != null && !d.isBefore(start) && !d.isAfter(end)
        }
        val important = periodAlerts.count { it.severity == AlertSeverity.IMPORTANT }

        val previousStart = start.minusDays(days)
        val previousEnd = start.minusDays(1)
        val previous = history.filter {
            val d = runCatching { LocalDate.parse(it.date) }.getOrNull()
            d != null && !d.isBefore(previousStart) && !d.isAfter(previousEnd)
        }
        val previousMinutes = previous.sumOf { it.totalTimeMs } / 60_000L
        val trend = if (previousMinutes > 0L) {
            (((totalMinutes - previousMinutes).toDouble() / previousMinutes) * 100.0)
                .roundToInt()
        } else null

        val narrative = when {
            selected.isEmpty() ->
                "Todavía no hay datos suficientes para generar este informe."
            trend != null && trend >= 15 ->
                "El uso del periodo ha aumentado aproximadamente un $trend% respecto al periodo anterior."
            trend != null && trend <= -15 ->
                "El uso del periodo ha disminuido aproximadamente un ${-trend}% respecto al periodo anterior."
            important > 0 ->
                "El periodo presenta $important alerta(s) importante(s) que conviene revisar junto con el contexto."
            else ->
                "El patrón registrado no muestra un cambio fuerte respecto al periodo anterior."
        }

        return UsageReport(
            period = period,
            startDate = start.toString(),
            endDate = end.toString(),
            totalMinutes = totalMinutes,
            averageDailyMinutes = average,
            peakDate = peak?.date,
            peakMinutes = (peak?.totalTimeMs ?: 0L) / 60_000L,
            topApps = appMap.values.sortedByDescending { it.totalMinutes }.take(10),
            alertCount = periodAlerts.size,
            importantAlertCount = important,
            trendPercent = trend,
            narrative = narrative
        )
    }
}
