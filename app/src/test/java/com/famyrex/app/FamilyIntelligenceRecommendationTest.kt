package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyIntelligenceRecommendationTest {
    private fun summary(
        status: ParentalStatus,
        alerts: Int = 0,
        usageAccess: Boolean = true,
        accessibilityEnabled: Boolean = true
    ) = FamilyIntelligenceSummary(
        parentalStatus = status,
        totalScreenMinutes = 120L,
        communicationAlertCount = alerts,
        protectionReady = usageAccess && accessibilityEnabled,
        reasons = emptyList()
    )

    @Test
    fun communicationAlertsTakePriority() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            summary(ParentalStatus.GREEN, alerts = 2),
            null
        )
        assertEquals("Revisar comunicaciones", result.title)
    }

    @Test
    fun missingDataRecommendsCompletingProtection() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            summary(ParentalStatus.WHITE, usageAccess = false, accessibilityEnabled = false),
            null
        )
        assertEquals("Completar protección", result.title)
    }

    @Test
    fun redStatusRecommendsReviewingLimit() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            summary(ParentalStatus.RED),
            null
        )
        assertEquals("Revisar el límite", result.title)
    }

    @Test
    fun orangeStatusRecommendsReviewingRoutine() {
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            summary(ParentalStatus.ORANGE),
            null
        )
        assertEquals("Revisar la rutina", result.title)
    }

    @Test
    fun increasingGreenTrendRecommendsObservation() {
        val trend = FamilyUsageTrendEvaluator.evaluate(listOf(100L, 100L, 150L))
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            summary(ParentalStatus.GREEN),
            trend
        )
        assertEquals("Observar la evolución", result.title)
    }

    @Test
    fun stableGreenStatusDoesNotInventAProblem() {
        val trend = FamilyUsageTrendEvaluator.evaluate(listOf(100L, 105L, 102L))
        val result = FamilyIntelligenceRecommendationEngine.recommend(
            summary(ParentalStatus.GREEN),
            trend
        )
        assertEquals("Seguir observando", result.title)
    }
}
