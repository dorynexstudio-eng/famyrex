package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertLifecyclePolicyTest {
    @Test
    fun refreshedAlertKeepsPersistedLifecycleStatus() {
        val existing = SmartAlert(
            id = "daily_limit_2026-09-05",
            type = AlertType.DAILY_LIMIT,
            severity = AlertSeverity.ATTENTION,
            title = "Límite diario superado",
            message = "Uso elevado",
            date = "2026-09-05",
            lifecycleStatus = AlertLifecycleStatus.DISMISSED
        )
        val regenerated = existing.copy(
            severity = AlertSeverity.IMPORTANT,
            message = "Uso muy elevado",
            lifecycleStatus = AlertLifecycleStatus.DETECTED
        )

        val refreshed = regenerated.copy(lifecycleStatus = existing.lifecycleStatus)

        assertEquals(AlertLifecycleStatus.DISMISSED, refreshed.lifecycleStatus)
        assertEquals(AlertSeverity.IMPORTANT, refreshed.severity)
        assertEquals("Uso muy elevado", refreshed.message)
    }

    @Test
    fun resolvedAlertRemainsResolvedWhenDetectorRegeneratesIt() {
        val existing = SmartAlert(
            id = "night_2026-09-05",
            type = AlertType.NIGHT_USE,
            severity = AlertSeverity.ATTENTION,
            title = "Uso nocturno",
            message = "30 min",
            date = "2026-09-05",
            lifecycleStatus = AlertLifecycleStatus.RESOLVED
        )
        val regenerated = existing.copy(message = "45 min", lifecycleStatus = AlertLifecycleStatus.DETECTED)

        val refreshed = regenerated.copy(lifecycleStatus = existing.lifecycleStatus)

        assertEquals(AlertLifecycleStatus.RESOLVED, refreshed.lifecycleStatus)
        assertEquals("45 min", refreshed.message)
    }
}
