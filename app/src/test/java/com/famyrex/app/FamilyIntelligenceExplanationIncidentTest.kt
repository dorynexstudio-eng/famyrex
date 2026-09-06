package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyIntelligenceExplanationIncidentTest {
    private fun incident(
        id: String,
        createdAtMs: Long,
        status: RiskIncidentStatus = RiskIncidentStatus.DETECTED
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
        direction = CommunicationDirection.OUTGOING,
        status = status
    )

    @Test
    fun explanationUsesConcreteActiveIncidentEvidence() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 60, 1, true, emptyList())

        val explanation = FamilyIntelligenceExplanation.explain(
            summary,
            null,
            listOf(incident("incident-42", 200L))
        )

        assertTrue(explanation.contains("Posible conflicto entre iguales"))
        assertTrue(explanation.contains("comunicación saliente"))
        assertTrue(explanation.contains("incidente"))
    }

    @Test
    fun closedIncidentDoesNotBecomeConcreteExplanationEvidence() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 60, 0, true, emptyList())

        val explanation = FamilyIntelligenceExplanation.explain(
            summary,
            null,
            listOf(incident("incident-closed", 200L, RiskIncidentStatus.RESOLVED))
        )

        assertFalse(explanation.contains("Posible conflicto entre iguales"))
        assertFalse(explanation.contains("incident-closed"))
    }

    @Test
    fun explanationRemainsNeutralForCommunicationIncident() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.GREEN, 60, 1, true, emptyList())

        val explanation = FamilyIntelligenceExplanation.explain(
            summary,
            null,
            listOf(incident("incident-43", 200L))
        )

        assertFalse(explanation.contains("culpable", ignoreCase = true))
        assertFalse(explanation.contains("culpa", ignoreCase = true))
        assertTrue(explanation.contains("sin asumir intención"))
    }

    @Test
    fun redStatusTakesPriorityOverCommunicationIncident() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.RED, 180, 1, true, emptyList())

        val explanation = FamilyIntelligenceExplanation.explain(
            summary,
            null,
            listOf(incident("incident-44", 200L))
        )

        assertTrue(explanation.contains("límite configurado"))
        assertFalse(explanation.contains("Posible conflicto entre iguales"))
    }

    @Test
    fun whiteStatusTakesPriorityOverCommunicationIncident() {
        val summary = FamilyIntelligenceSummary(ParentalStatus.WHITE, null, 1, false, emptyList())

        val explanation = FamilyIntelligenceExplanation.explain(
            summary,
            null,
            listOf(incident("incident-45", 200L))
        )

        assertTrue(explanation.contains("faltan datos"))
        assertFalse(explanation.contains("Posible conflicto entre iguales"))
    }
}
