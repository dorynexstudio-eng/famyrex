package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEngineLimitTest {
    private fun alert(id: String, severity: AlertSeverity) = SmartAlert(
        id = id,
        type = AlertType.APP_SPIKE,
        severity = severity,
        title = "Aviso",
        message = "Prueba",
        date = "2026-09-05"
    )

    @Test
    fun importantAlertsArePreservedWhenDisplayLimitIsReached() {
        val alerts = buildList {
            repeat(25) { add(alert("attention-$it", AlertSeverity.ATTENTION)) }
            add(alert("important-1", AlertSeverity.IMPORTANT))
            add(alert("important-2", AlertSeverity.IMPORTANT))
        }

        val result = AlertEngine.limitAlerts(alerts, maxSize = 20)

        assertEquals(20, result.size)
        assertTrue(result.any { it.id == "important-1" })
        assertTrue(result.any { it.id == "important-2" })
    }

    @Test
    fun importantAlertsArePreferredOverOlderLowerSeverityAlerts() {
        val alerts = listOf(
            alert("old-attention", AlertSeverity.ATTENTION),
            alert("important", AlertSeverity.IMPORTANT),
            alert("new-attention", AlertSeverity.ATTENTION)
        )

        val result = AlertEngine.limitAlerts(alerts, maxSize = 2)

        assertEquals(listOf("important", "new-attention"), result.map { it.id })
    }
}
