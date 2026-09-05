package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationRiskIncidentEvolutionTest {
    @Test
    fun `merge preserves previous reasons and keeps strongest score and confidence`() {
        val previous = incident(
            score = 90,
            confidence = RiskConfidence.HIGH,
            reasons = listOf(reason("BULLYING_SIGNAL"))
        )
        val current = incident(
            score = 70,
            confidence = RiskConfidence.MEDIUM,
            reasons = listOf(reason("THREAT_SIGNAL"))
        )

        val merged = CommunicationRiskIncidentEvolution.merge(previous, current)

        assertEquals(90, merged.score)
        assertEquals(RiskConfidence.HIGH, merged.confidence)
        assertEquals(setOf("BULLYING_SIGNAL", "THREAT_SIGNAL"), merged.reasons.map { it.code }.toSet())
        assertEquals(previous.status, merged.status)
        assertEquals(previous.statusHistory, merged.statusHistory)
        assertEquals(previous.createdAtMs, merged.createdAtMs)
    }

    @Test
    fun `merge deduplicates repeated reasons`() {
        val previous = incident(reasons = listOf(reason("BULLYING_SIGNAL")))
        val current = incident(reasons = listOf(reason("BULLYING_SIGNAL"), reason("THREAT_SIGNAL")))

        val merged = CommunicationRiskIncidentEvolution.merge(previous, current)

        assertEquals(2, merged.reasons.size)
        assertTrue(merged.reasons.any { it.code == "THREAT_SIGNAL" })
    }

    private fun incident(
        score: Int = 70,
        confidence: RiskConfidence = RiskConfidence.MEDIUM,
        reasons: List<RiskReason> = emptyList()
    ) = CommunicationRiskIncident(
        id = "incident-1",
        createdAtMs = 100L,
        type = CommunicationRiskType.BULLYING,
        confidence = confidence,
        score = score,
        reasons = reasons,
        sourcePackage = "test.package",
        direction = CommunicationDirection.INCOMING,
        status = RiskIncidentStatus.REVIEWED,
        statusHistory = listOf(RiskIncidentStatusChange(RiskIncidentStatus.REVIEWED, 200L))
    )

    private fun reason(code: String) = RiskReason(code, code, "test")
}
