package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CommunicationRiskTypeSelectorTest {
    @Test
    fun emptySignals_returnsNull() {
        assertNull(CommunicationRiskTypeSelector.select(emptyList()))
    }

    @Test
    fun higherPriorityTypeWinsEvenWhenLowerPriorityIsMoreRecent() {
        val signals = listOf(
            signal(CommunicationRiskType.SELF_HARM, RiskConfidence.MEDIUM, 1L),
            signal(CommunicationRiskType.BULLYING, RiskConfidence.HIGH, 2L)
        )

        assertEquals(CommunicationRiskType.SELF_HARM, CommunicationRiskTypeSelector.select(signals))
    }

    @Test
    fun confidenceBreaksTieBetweenSamePriorityTypes() {
        val signals = listOf(
            signal(CommunicationRiskType.THREAT, RiskConfidence.LOW, 1L),
            signal(CommunicationRiskType.THREAT, RiskConfidence.HIGH, 2L)
        )

        assertEquals(CommunicationRiskType.THREAT, CommunicationRiskTypeSelector.select(signals))
    }

    private fun signal(
        type: CommunicationRiskType,
        confidence: RiskConfidence,
        timestampMs: Long
    ) = CommunicationRiskSignal(
        type = type,
        confidence = confidence,
        reason = "test",
        sourcePackage = "com.example.chat",
        timestampMs = timestampMs,
        direction = CommunicationDirection.INCOMING
    )
}
