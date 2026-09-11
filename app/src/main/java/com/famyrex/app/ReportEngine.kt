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

        // A trend must compare equivalent completed days. The current day is
        // still accumulating usage, so exclude it from both sides of the
        // comparison. The report totals still include today's available data.
        val trend = if (period == ReportPeriod.DAILY) {
            null
        } else {
            val completedEnd = today.minusDays(1)
            val completedStart = completedEnd.minusDays(days - 1)
            val previousEnd = completedStart.minusDays(1)
            val previousStart = previousEnd.minusDays(days - 1)

            val completed = history.filter { entry ->
                parseDate(entry.date)?.let { date ->
                    !date.isBefore(completedStart) && !date.isAfter(completedEnd)
                } == true
            }
            val previous = history.filter { entry ->
                parseDate(entry.date)?.let { date ->
                    !date.isBefore(previousStart) && !date.isAfter(previousEnd)
                } == true
            }

            // Missing days mean incomplete coverage. Comparing totals would
            // interpret missing data as lower usage, so only publish a trend
            // when both windows contain all expected daily snapshots.
            if (completed.size == days.toInt() && previous.size == days.toInt()) {
                val completedMinutes = completed.sumOf { it.totalTimeMs } / 60_000L
                val previousMinutes = previous.sumOf { it.totalTimeMs } / 60_000L
                if (previousMinutes > 0L) {
                    (((completedMinutes - previousMinutes).toDouble() / previousMinutes) * 100.0)
                        .roundToInt()
                } else null
            } else null
        }

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
