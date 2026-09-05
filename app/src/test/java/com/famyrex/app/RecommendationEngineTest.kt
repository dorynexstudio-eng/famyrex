package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {

    @Test
    fun communicationRisk_isNeutralAndSupportive() {
        val alert = SmartAlert(
            id = "communication_risk_1",
            type = AlertType.COMMUNICATION_RISK,
            severity = AlertSeverity.IMPORTANT,
            title = "Riesgo de comunicación",
            message = "Señal detectada",
            date = "2026-09-05"
        )

        val result = RecommendationEngine.evaluate(listOf(alert))

        assertEquals(1, result.size)
        assertEquals(RecommendationPriority.HIGH, result.first().priority)
        assertEquals(RecommendationAction.SUPPORT, result.first().action)
        assertTrue(result.first().message.contains("sin asumir"))
    }

    @Test
    fun dismissedAlerts_doNotProduceRecommendations() {
        val alert = SmartAlert(
            id = "daily_1",
            type = AlertType.DAILY_LIMIT,
            severity = AlertSeverity.ATTENTION,
            title = "Límite",
            message = "Uso elevado",
            date = "2026-09-05",
            lifecycleStatus = AlertLifecycleStatus.DISMISSED
        )

        assertTrue(RecommendationEngine.evaluate(listOf(alert)).isEmpty())
    }

    @Test
    fun resolvedAlerts_doNotProduceRecommendations() {
        val alert = SmartAlert(
            id = "resolved_1",
            type = AlertType.COMMUNICATION_RISK,
            severity = AlertSeverity.IMPORTANT,
            title = "Comunicación",
            message = "Revisar",
            date = "2026-09-05",
            lifecycleStatus = AlertLifecycleStatus.RESOLVED
        )

        assertTrue(RecommendationEngine.evaluate(listOf(alert)).isEmpty())
    }

    @Test
    fun importantAlerts_arePrioritizedFirst() {
        val low = SmartAlert(
            id = "restored",
            type = AlertType.PROTECTION_RESTORED,
            severity = AlertSeverity.INFO,
            title = "Protección restaurada",
            message = "OK",
            date = "2026-09-05"
        )
        val high = SmartAlert(
            id = "evasion",
            type = AlertType.EVASION_SIGNAL,
            severity = AlertSeverity.IMPORTANT,
            title = "Señal de evasión",
            message = "Señal",
            date = "2026-09-05"
        )

        val result = RecommendationEngine.evaluate(listOf(low, high))

        assertEquals("recommendation_evasion", result.first().id)
        assertEquals(RecommendationPriority.HIGH, result.first().priority)
    }
}
