package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyIntelligenceRecommendationEvidenceIntegrationTest {
    @Test
    fun redStatusEvidenceAndRecommendationBothRequireAction() {
        val summary = FamilyIntelligenceSummary(
            parentalStatus = ParentalStatus.RED,
            totalScreenMinutes = 180,
            communicationAlertCount = 0,
            protectionReady = true,
            reasons = emptyList()
        )
        val evidence = FamilyIntelligenceEvidenceBuilder.fromSummary(summary)
        val recommendation = FamilyIntelligenceRecommendationEngine.recommend(
            listOf(
                SmartAlert(
                    id = "risk",
                    type = AlertType.DAILY_LIMIT,
                    severity = AlertSeverity.IMPORTANT,
                    title = "Límite alcanzado",
                    message = "Límite alcanzado",
                    date = "2026-09-06",
                    lifecycleStatus = AlertLifecycleStatus.DETECTED
                )
            )
        )

        assertEquals(FamilyIntelligenceEvidenceType.STATUS, evidence.first().type)
        assertEquals(FamilyIntelligenceRecommendationDestination.PARENTAL_CONTROL, recommendation?.destination)
    }
}
