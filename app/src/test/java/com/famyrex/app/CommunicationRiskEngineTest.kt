package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationRiskEngineTest {
    private fun signal(type: CommunicationRiskType, confidence: RiskConfidence) =
        CommunicationRiskSignal(type, confidence, "test")

    @Test
    fun emptySignalsProduceNoAlert() {
        val result = CommunicationRiskEngine.evaluate(emptyList())
        assertEquals(0, result.score)
        assertEquals(RiskConfidence.LOW, result.confidence)
        assertFalse(result.shouldAlert)
    }

    @Test
    fun singleLowSignalDoesNotAlert() {
        val result = CommunicationRiskEngine.evaluate(
            listOf(signal(CommunicationRiskType.BULLYING, RiskConfidence.LOW))
        )
        assertFalse(result.shouldAlert)
    }

    @Test
    fun multipleIndependentHighSignalsCanAlert() {
        val result = CommunicationRiskEngine.evaluate(
            listOf(
                signal(CommunicationRiskType.THREAT, RiskConfidence.HIGH),
                signal(CommunicationRiskType.SEXUAL_REQUEST, RiskConfidence.HIGH)
            )
        )
        assertTrue(result.shouldAlert)
        assertTrue(result.score >= 70)
        assertEquals(RiskConfidence.HIGH, result.confidence)
    }

    @Test
    fun scoreNeverExceedsOneHundred() {
        val signals = CommunicationRiskType.entries.flatMap { type ->
            listOf(signal(type, RiskConfidence.HIGH), signal(type, RiskConfidence.MEDIUM))
        }
        val result = CommunicationRiskEngine.evaluate(signals)
        assertEquals(100, result.score)
    }

    @Test
    fun duplicateReasonsAreRemovedFromSummary() {
        val duplicate = signal(CommunicationRiskType.THREAT, RiskConfidence.HIGH)
        val result = CommunicationRiskEngine.evaluate(listOf(duplicate, duplicate))
        assertEquals(1, result.signals.size)
    }
}
