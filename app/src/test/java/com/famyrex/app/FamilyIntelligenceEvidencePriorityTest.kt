package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyIntelligenceEvidencePriorityTest {
    @Test
    fun redRiskRemainsFirstWhenCommunicationAndAnomalyExist() {
        val summary = FamilyIntelligenceSummary(
            ParentalStatus.RED,
            180,
            1,
            true,
            emptyList()
        )
        val trend = FamilyUsageTrendEvaluator.evaluate(listOf(60, 60, 180))

        val evidence = FamilyIntelligenceEvidenceBuilder.build(summary, trend)

        assertEquals(FamilyIntelligenceEvidenceType.STATUS, evidence.first().type)
        assertTrue(evidence.first().conclusion.contains("Acción necesaria"))
        assertTrue(evidence.any { it.type == FamilyIntelligenceEvidenceType.COMMUNICATION })
        assertTrue(evidence.any { it.type == FamilyIntelligenceEvidenceType.ANOMALY })
    }

    @Test
    fun insufficientDataIsNotReportedAsHealthy() {
        val summary = FamilyIntelligenceSummary(
            ParentalStatus.WHITE,
            null,
            0,
            false,
            emptyList()
        )

        val evidence = FamilyIntelligenceEvidenceBuilder.build(summary, null)

        assertEquals(FamilyIntelligenceEvidenceType.DATA_GAP, evidence.first().type)
        assertTrue(evidence.first().conclusion.contains("Sin datos suficientes"))
        assertFalse(evidence.any { it.conclusion.contains("En orden") })
    }

    @Test
    fun communicationEvidenceNeverClaimsGuiltOrIntent() {
        val summary = FamilyIntelligenceSummary(
            ParentalStatus.GREEN,
            60,
            2,
            true,
            emptyList()
        )

        val evidence = FamilyIntelligenceEvidenceBuilder.fromSummary(summary)
        val communication = evidence.first { it.type == FamilyIntelligenceEvidenceType.COMMUNICATION }
        val text = "${communication.signal} ${communication.conclusion} ${communication.action}".lowercase()

        assertTrue(text.contains("sin atribuir intención"))
        assertFalse(text.contains("culpable"))
        assertFalse(text.contains("culpa"))
    }
}
