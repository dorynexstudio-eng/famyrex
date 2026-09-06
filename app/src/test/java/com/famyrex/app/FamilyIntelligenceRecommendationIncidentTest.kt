package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilyIntelligenceRecommendationIncidentTest {
    private fun incident(
        id: String,
        createdAtMs: Long,
        status: RiskIncidentStatus = RiskIncidentStatus.DETECTED
    ) = CommunicationRiskIncident(
        id = id,
        createdAtMs = createdAtMs,
        type = CommunicationRiskType.SOCIAL_CONFLICT,
        confidence = RiskConfidence.MEDIUM,
        score = 70,
        reasons = listOf(RiskReason("SOCIAL_CONFLICT", "Posible conflicto entre iguales", "Señal de prueba")),
        sourcePackage = "test.package",
        direction = CommunicationDirection.UNKNOWN,
        status = status
    )

    @Test
    fun activeIncidentProducesAlertRecommendationWithoutBlame() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            emptyList(),
            listOf(incident("incident-1", 100L))
        )

        assertEquals(FamilyIntelligenceRecommendationDestination.ALERTS, result?.destination)
        assertFalse(result?.action.orEmpty().contains("culpable", ignoreCase = true))
        assertFalse(result?.action.orEmpty().contains("culpabilidad", ignoreCase = true))
        assertFalse(result?.action.orEmpty().contains("intención", ignoreCase = true))
    }

    @Test
    fun dismissedAndResolvedIncidentsProduceNoRecommendation() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            emptyList(),
            listOf(
                incident("dismissed", 200L, RiskIncidentStatus.DISMISSED),
                incident("resolved", 100L, RiskIncidentStatus.RESOLVED),
                incident("auto-dismissed", 50L, RiskIncidentStatus.AUTO_DISMISSED)
            )
        )

        assertNull(result)
    }

    @Test
    fun activeIncidentTakesPriorityOverLowerPriorityAlert() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            listOf(
                SmartAlert(
                    id = "observe",
                    type = AlertType.PROTECTION_RESTORED,
                    severity = AlertSeverity.INFO,
                    title = "Protección restaurada",
                    message = "Todo vuelve a estar disponible.",
                    date = "2026-09-06"
                )
            ),
            listOf(incident("incident-1", 100L))
        )

        assertEquals(FamilyIntelligenceRecommendationDestination.ALERTS, result?.destination)
        assertEquals("Revisad la situación con apoyo", result?.title)
    }

    @Test
    fun newestActiveIncidentMakesRecommendationDeterministic() {
        val incidents = listOf(
            incident("older", 100L),
            incident("newer", 200L)
        )

        val first = FamilyIntelligenceRecommendationEngine.recommend(emptyList(), incidents)
        val second = FamilyIntelligenceRecommendationEngine.recommend(emptyList(), incidents.reversed())

        assertEquals(first, second)
    }
}
