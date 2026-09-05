package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationRiskEpisodeMatcherTest {
    private val sourcePackage = "test.package"
    private val now = 30 * 60 * 1000L

    @Test
    fun `same risk type within cooldown is same episode`() {
        val summary = summary(CommunicationRiskType.BULLYING)
        val incident = incident(CommunicationRiskType.BULLYING)

        assertTrue(
            CommunicationRiskEpisodeMatcher.isSameEpisode(summary, incident, sourcePackage, now)
        )
    }

    @Test
    fun `different risk type is a new episode`() {
        val summary = summary(CommunicationRiskType.SELF_HARM)
        val incident = incident(CommunicationRiskType.BULLYING)

        assertFalse(
            CommunicationRiskEpisodeMatcher.isSameEpisode(summary, incident, sourcePackage, now)
        )
    }

    @Test
    fun `terminal incident is never reused`() {
        val summary = summary(CommunicationRiskType.BULLYING)
        val incident = incident(CommunicationRiskType.BULLYING).copy(status = RiskIncidentStatus.RESOLVED)

        assertFalse(
            CommunicationRiskEpisodeMatcher.isSameEpisode(summary, incident, sourcePackage, now)
        )
    }

    @Test
    fun `different source package is a new episode`() {
        val summary = summary(CommunicationRiskType.BULLYING)
        val incident = incident(CommunicationRiskType.BULLYING).copy(sourcePackage = "other.package")

        assertFalse(
            CommunicationRiskEpisodeMatcher.isSameEpisode(summary, incident, sourcePackage, now)
        )
    }

    @Test
    fun `different direction is a new episode`() {
        val summary = summary(CommunicationRiskType.BULLYING, CommunicationDirection.INCOMING)
        val incident = incident(CommunicationRiskType.BULLYING, CommunicationDirection.OUTGOING)

        assertFalse(
            CommunicationRiskEpisodeMatcher.isSameEpisode(summary, incident, sourcePackage, now)
        )
    }

    @Test
    fun `incident outside cooldown is a new episode`() {
        val summary = summary(CommunicationRiskType.BULLYING)
        val incident = incident(CommunicationRiskType.BULLYING).copy(createdAtMs = 0L)

        assertFalse(
            CommunicationRiskEpisodeMatcher.isSameEpisode(summary, incident, sourcePackage, now + 1)
        )
    }

    private fun summary(
        type: CommunicationRiskType,
        direction: CommunicationDirection = CommunicationDirection.INCOMING
    ) = CommunicationRiskSummary(
        score = 70,
        confidence = RiskConfidence.HIGH,
        signals = listOf(
            CommunicationRiskSignal(
                type = type,
                confidence = RiskConfidence.HIGH,
                reason = "test",
                sourcePackage = sourcePackage,
                direction = direction
            )
        )
    )

    private fun incident(
        type: CommunicationRiskType,
        direction: CommunicationDirection = CommunicationDirection.INCOMING
    ) = CommunicationRiskIncident(
        id = "incident-1",
        createdAtMs = now - 5 * 60 * 1000L,
        type = type,
        confidence = RiskConfidence.HIGH,
        score = 70,
        reasons = emptyList(),
        sourcePackage = sourcePackage,
        direction = direction
    )
}
