package com.famyrex.app

import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyIntelligenceExplanationTest {
    @Test
    fun whiteExplainsMissingEvidence() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.WHITE, null, 0, false, emptyList())
        assertTrue(FamilyIntelligenceExplanation.explain(summary, null).contains("faltan datos"))
    }

    @Test
    fun redExplainsConfiguredLimit() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.RED, 180, 0, true, emptyList())
        assertTrue(FamilyIntelligenceExplanation.explain(summary, null).contains("límite configurado"))
    }

    @Test
    fun communicationSignalGetsPriority() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 60, 1, true, emptyList())
        assertTrue(FamilyIntelligenceExplanation.explain(summary, null).contains("señal de comunicación"))
    }

    @Test
    fun greenIncreasingUsageIsNotPresentedAsDanger() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 120, 0, true, emptyList())
        val trend = FamilyUsageTrend(listOf(60, 70, 80, 120), 120, 70.0, FamilyUsageTrendDirection.INCREASING)
        val explanation = FamilyIntelligenceExplanation.explain(summary, trend)
        assertTrue(explanation.contains("protección está en orden"))
    }
}
