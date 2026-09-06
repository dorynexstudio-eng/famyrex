package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyIntelligenceEvidenceTest {
    @Test
    fun redStatusProducesAuditableEvidence() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.RED, 180, 0, true, emptyList())
        val evidence = FamilyIntelligenceEvidenceBuilder.fromSummary(summary)

        assertEquals(FamilyIntelligenceEvidenceType.STATUS, evidence.first().type)
        assertTrue(evidence.first().signal.contains("Límite configurado"))
        assertTrue(evidence.first().conclusion.contains("Acción necesaria"))
    }

    @Test
    fun communicationEvidenceIsExplicitAndDoesNotAssignIntent() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 60, 1, true, emptyList())
        val evidence = FamilyIntelligenceEvidenceBuilder.fromSummary(summary)
        val communication = evidence.first { it.type == FamilyIntelligenceEvidenceType.COMMUNICATION }

        assertTrue(communication.signal.contains("1 señal"))
        assertTrue(communication.conclusion.contains("sin atribuir intención"))
    }

    @Test
    fun whiteStatusExplainsTheDataGap() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.WHITE, null, 0, false, emptyList())
        val evidence = FamilyIntelligenceEvidenceBuilder.fromSummary(summary)

        assertTrue(evidence.any { it.type == FamilyIntelligenceEvidenceType.DATA_GAP })
        assertTrue(evidence.first().conclusion.contains("Sin datos suficientes"))
    }

    @Test
    fun anomalyEvidenceStaysNeutralAboutCause() {
        val trend = FamilyUsageTrend(
            days = listOf(60, 60, 120),
            todayMinutes = 120,
            previousAverageMinutes = 60.0,
            direction = FamilyUsageTrendDirection.INCREASING,
            anomaly = FamilyUsageAnomaly(2, 120, 60.0, 100, FamilyUsageAnomalyType.HIGH)
        )

        val evidence = FamilyIntelligenceEvidenceBuilder.fromTrend(trend)
        val anomaly = evidence.first { it.type == FamilyIntelligenceEvidenceType.ANOMALY }

        assertTrue(anomaly.conclusion.contains("variación estadística"))
        assertTrue(anomaly.action.contains("no demuestra una causa"))
    }

    @Test
    fun evidenceOrderIsDeterministic() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 60, 2, false, emptyList())
        val trend = FamilyUsageTrendEvaluator.evaluate(listOf(60, 60, 120))

        val first = FamilyIntelligenceEvidenceBuilder.build(summary, trend).map { it.type }
        val second = FamilyIntelligenceEvidenceBuilder.build(summary, trend).map { it.type }

        assertEquals(first, second)
    }
}
