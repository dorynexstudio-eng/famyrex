package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyIntelligenceRecommendationEvidenceIntegrationTest {
    @Test
    fun redStatusEvidenceAndRecommendationBothRequireAction() {
        val summary = FamilyIntelligenceSummary(
            ParentalStatus.RED,
            180,
            0,
            true,
            emptyList()
        )
        val evidence = FamilyIntelligenceEvidenceBuilder.fromSummary(summary)
        val recommendation = FamilyIntelligenceRecommendationEngine.recommend(
            listOf(
                SmartAlert(
                    id = "risk",
                    type = AlertType.SCREEN_TIME_LIMIT,
                    severity = AlertSeverity.IMPORTANT,
                    message = "Límite alcanzado",
                    timestamp = 1L,
                    lifecycleStatus = AlertLifecycleStatus.ACTIVE
                )
            )
        )

        assertEquals(FamilyIntelligenceEvidenceType.STATUS, evidence.first().type)
        assertEquals(FamilyIntelligenceRecommendationDestination.ALERTS, recommendation?.destination)
    }
}
