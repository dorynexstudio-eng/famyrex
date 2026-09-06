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
    fun highAnomalyExplainsObservedVariationWithoutAssigningCause() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 180, 0, true, emptyList())
        val trend = FamilyUsageTrend(
            listOf(60, 70, 80, 180),
            180,
            70.0,
            FamilyUsageTrendDirection.INCREASING,
            FamilyUsageAnomaly(3, 180, 70.0, 157, FamilyUsageAnomalyType.HIGH)
        )

        val explanation = FamilyIntelligenceExplanation.explain(summary, trend)

        assertTrue(explanation.contains("157%"))
        assertTrue(explanation.contains("conviene observar"))
    }

    @Test
    fun lowAnomalyDoesNotAssumeItsCause() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 20, 0, true, emptyList())
        val trend = FamilyUsageTrend(
            listOf(100, 90, 80, 20),
            20,
            90.0,
            FamilyUsageTrendDirection.DECREASING,
            FamilyUsageAnomaly(3, 20, 90.0, 77, FamilyUsageAnomalyType.LOW)
        )

        val explanation = FamilyIntelligenceExplanation.explain(summary, trend)

        assertTrue(explanation.contains("77%"))
        assertTrue(explanation.contains("sin asumir su causa"))
    }

    @Test
    fun greenIncreasingUsageIsNotPresentedAsDanger() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 120, 0, true, emptyList())
        val trend = FamilyUsageTrend(listOf(60, 70, 80, 120), 120, 70.0, FamilyUsageTrendDirection.INCREASING)
        val explanation = FamilyIntelligenceExplanation.explain(summary, trend)
        assertTrue(explanation.contains("protección está en orden"))
    }

    @Test
    fun greenInsufficientTrendIsExplicit() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 40, 0, true, emptyList())
        val trend = FamilyUsageTrend(listOf(40), 40, null, FamilyUsageTrendDirection.INSUFFICIENT_DATA)
        assertTrue(FamilyIntelligenceExplanation.explain(summary, trend).contains("no hay suficientes datos"))
    }
}
