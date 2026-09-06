package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyIntelligenceRecommendationIncidentRegressionTest {
    private fun incident(
        id: String,
        createdAtMs: Long,
        status: RiskIncidentStatus = RiskIncidentStatus.DETECTED,
        direction: CommunicationDirection = CommunicationDirection.UNKNOWN
    ) = CommunicationRiskIncident(
        id = id,
        createdAtMs = createdAtMs,
        type = CommunicationRiskType.SOCIAL_CONFLICT,
        confidence = RiskConfidence.MEDIUM,
        score = 72,
        reasons = listOf(
            RiskReason(
                "SOCIAL_CONFLICT",
                "Posible conflicto entre iguales",
                "Señal de prueba"
            )
        ),
        sourcePackage = "test",
        direction = direction,
        status = status
    )

    private fun alert() = SmartAlert(
        id = "limit-1",
        type = AlertType.DAILY_LIMIT,
        severity = AlertSeverity.IMPORTANT,
        title = "Límite diario",
        message = "Límite alcanzado",
        date = "2026-09-06"
    )

    @Test
    fun activeIncidentTakesPriorityOverOlderAlertRecommendation() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            alerts = listOf(alert()),
            incidents = listOf(incident("incident-1", 200L))
        )

        assertEquals(FamilyIntelligenceRecommendationDestination.ALERTS, result?.destination)
        assertEquals("Revisad la situación con apoyo", result?.title)
        assertTrue(result?.action?.contains("incidente más reciente") == true)
    }

    @Test
    fun resolvedIncidentCannotKeepCommunicationRecommendationAlive() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            alerts = emptyList(),
            incidents = listOf(incident("incident-1", 200L, RiskIncidentStatus.RESOLVED))
        )

        assertEquals(null, result)
    }

    @Test
    fun dismissedAndAutoDismissedIncidentsCannotKeepRecommendationAlive() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            alerts = emptyList(),
            incidents = listOf(
                incident("dismissed", 300L, RiskIncidentStatus.DISMISSED),
                incident("auto", 200L, RiskIncidentStatus.AUTO_DISMISSED)
            )
        )

        assertEquals(null, result)
    }

    @Test
    fun activeIncidentCountIgnoresClosedLifecycleStates() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            alerts = emptyList(),
            incidents = listOf(
                incident("active", 400L),
                incident("reviewed", 300L, RiskIncidentStatus.REVIEWED),
                incident("confirmed", 200L, RiskIncidentStatus.CONFIRMED),
                incident("resolved", 100L, RiskIncidentStatus.RESOLVED)
            )
        )

        assertTrue(result != null)
        assertTrue(result?.action?.contains("3 señales") == true)
    }

    @Test
    fun communicationRecommendationRemainsNeutral() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            alerts = emptyList(),
            incidents = listOf(incident("incident-1", 200L, direction = CommunicationDirection.OUTGOING))
        )

        val action = result?.action.orEmpty()
        assertFalse(action.contains("culpable", ignoreCase = true))
        assertFalse(action.contains("culpa", ignoreCase = true))
        assertTrue(action.contains("sin asumir intención"))
    }
}
