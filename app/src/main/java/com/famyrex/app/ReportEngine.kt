package com.famyrex.app

import java.time.LocalDate
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
            parseDate(it.date)?.let { date -> !date.isBefore(start) && !date.isAfter(end) } == true
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

        // SmartAlert dates are normally "yyyy-MM-dd HH:mm:ss". Accept the
        // date-only form too so locally persisted alerts are not silently lost.
        val periodAlerts = alerts.filter { alert ->
            parseDate(alert.date)?.let { date -> !date.isBefore(start) && !date.isAfter(end) } == true
        }
        val important = periodAlerts.count { it.severity == AlertSeverity.IMPORTANT }

        val previousStart = start.minusDays(days)
        val previousEnd = start.minusDays(1)
        val previous = history.filter {
            parseDate(it.date)?.let { date -> !date.isBefore(previousStart) && !date.isAfter(previousEnd) } == true
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

    private fun parseDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value.take(10)) }.getOrNull()
}
