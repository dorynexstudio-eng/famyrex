package com.famyrex.app

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportEngineTest {
    @Test
    fun reportCountsAlertsStoredWithTimestamp() {
        val today = LocalDate.of(2026, 9, 6)
        val history = listOf(
            DailyUsage("2026-09-06", 60 * 60_000L, emptyList())
        )
        val alerts = listOf(
            SmartAlert(
                id = "a1",
                type = AlertType.DAILY_LIMIT,
                severity = AlertSeverity.IMPORTANT,
                title = "Límite",
                message = "Revisar",
                date = "2026-09-06 18:42:11"
            ),
            SmartAlert(
                id = "a2",
                type = AlertType.APP_SPIKE,
                severity = AlertSeverity.ATTENTION,
                title = "Variación",
                message = "Revisar",
                date = "2026-09-06"
            ),
            SmartAlert(
                id = "old",
                type = AlertType.NIGHT_USE,
                severity = AlertSeverity.IMPORTANT,
                title = "Anterior",
                message = "Fuera del periodo",
                date = "2026-09-05 23:59:59"
            )
        )

        val report = ReportEngine.build(history, alerts, ReportPeriod.DAILY, today)

        assertEquals(2, report.alertCount)
        assertEquals(1, report.importantAlertCount)
    }

    @Test
    fun malformedAlertDateDoesNotBreakReport() {
        val report = ReportEngine.build(
            history = listOf(DailyUsage("2026-09-06", 30 * 60_000L, emptyList())),
            alerts = listOf(
                SmartAlert(
                    id = "bad",
                    type = AlertType.PATTERN_CHANGE,
                    severity = AlertSeverity.ATTENTION,
                    title = "Dato inválido",
                    message = "Ignorar fecha no válida",
                    date = "sin-fecha"
                )
            ),
            period = ReportPeriod.DAILY,
            today = LocalDate.of(2026, 9, 6)
        )

        assertEquals(0, report.alertCount)
        assertTrue(report.totalMinutes > 0)
    }
}
