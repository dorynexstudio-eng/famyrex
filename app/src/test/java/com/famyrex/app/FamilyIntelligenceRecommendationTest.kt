package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilyIntelligenceRecommendationTest {
    private fun alert(
        id: String,
        type: AlertType,
        severity: AlertSeverity = AlertSeverity.ATTENTION,
        lifecycleStatus: AlertLifecycleStatus = AlertLifecycleStatus.DETECTED
    ) = SmartAlert(
        id = id,
        type = type,
        severity = severity,
        title = "Aviso de prueba",
        message = "Mensaje de prueba",
        date = "2026-09-05",
        lifecycleStatus = lifecycleStatus
    )

    @Test
    fun communicationRiskMapsToAlertsAndKeepsNeutralSupport() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            listOf(alert("communication-1", AlertType.COMMUNICATION_RISK, AlertSeverity.IMPORTANT))
        )

        assertEquals("Revisad la situación con apoyo", result?.title)
        assertEquals(FamilyIntelligenceRecommendationDestination.ALERTS, result?.destination)
    }

    @Test
    fun protectionAlertsMapToParentalControl() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            listOf(alert("limit-1", AlertType.DAILY_LIMIT))
        )

        assertEquals(FamilyIntelligenceRecommendationDestination.PARENTAL_CONTROL, result?.destination)
    }

    @Test
    fun resolvedAlertsProduceNoRecommendation() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            listOf(alert("resolved-1", AlertType.DAILY_LIMIT, lifecycleStatus = AlertLifecycleStatus.RESOLVED))
        )

        assertNull(result)
    }

    @Test
    fun observationRecommendationMapsToObserve() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            listOf(alert("restored-1", AlertType.PROTECTION_RESTORED, AlertSeverity.INFO))
        )

        assertEquals(FamilyIntelligenceRecommendationDestination.OBSERVE, result?.destination)
    }
}
