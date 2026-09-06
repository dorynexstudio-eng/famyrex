package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyIntelligenceEvidenceIncidentTest {
    private fun incident(
        id: String,
        createdAtMs: Long,
        status: RiskIncidentStatus = RiskIncidentStatus.DETECTED,
        direction: CommunicationDirection = CommunicationDirection.UNKNOWN
    ) = CommunicationRiskIncident(
        id = id,
        createdAtMs = createdAtMs,
        type = CommunicationRiskType.values().first(),
        confidence = RiskConfidence.values().first(),
        score = 70,
        reasons = listOf(RiskReason("SIGNAL", "Señal concreta", "Contexto observado")),
        sourcePackage = "test.package",
        direction = direction,
        status = status
    )

    @Test
    fun activeIncidentKeepsExactReferenceAndCurrentStatus() {
        val evidence = FamilyIntelligenceEvidenceBuilder.fromIncidents(
            listOf(incident("incident-42", 100L, RiskIncidentStatus.REVIEWED))
        )

        assertEquals(1, evidence.size)
        assertEquals("incident-42", evidence.single().referenceId)
        assertEquals(RiskIncidentStatus.REVIEWED, evidence.single().incidentStatus)
        assertTrue(evidence.single().signal.contains("Señal concreta"))
        assertTrue(evidence.single().conclusion.contains("Revisado"))
    }

    @Test
    fun dismissedAndResolvedIncidentsDoNotBecomeActiveEvidence() {
        val incidents = listOf(
            incident("dismissed", 300L, RiskIncidentStatus.DISMISSED),
            incident("auto-dismissed", 200L, RiskIncidentStatus.AUTO_DISMISSED),
            incident("resolved", 100L, RiskIncidentStatus.RESOLVED)
        )

        assertTrue(FamilyIntelligenceEvidenceBuilder.fromIncidents(incidents).isEmpty())
    }

    @Test
    fun multipleIncidentsHaveDeterministicNewestFirstOrdering() {
        val evidence = FamilyIntelligenceEvidenceBuilder.fromIncidents(
            listOf(
                incident("older", 100L),
                incident("newer-b", 300L),
                incident("newer-a", 300L)
            )
        )

        assertEquals(listOf("newer-a", "newer-b", "older"), evidence.map { it.referenceId })
    }

    @Test
    fun directionIsRepresentedWithoutAssigningIntent() {
        val evidence = FamilyIntelligenceEvidenceBuilder.fromIncidents(
            listOf(incident("outgoing", 100L, direction = CommunicationDirection.OUTGOING))
        ).single()
        val text = "${evidence.signal} ${evidence.conclusion} ${evidence.action}".lowercase()

        assertTrue(text.contains("saliente"))
        assertTrue(text.contains("sin asumir intención"))
        assertFalse(text.contains("culpable"))
        assertFalse(text.contains("víctima"))
    }
}
