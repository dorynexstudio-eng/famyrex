package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvasionRiskEngineTest {
    private fun signal(key: String, confidence: SignalConfidence) = EvasionSignal(
        key = key,
        title = key,
        detail = "test",
        confidence = confidence
    )

    @Test
    fun `single developer signal does not accuse evasion`() {
        val result = EvasionRiskEngine.evaluate(listOf(signal("developer_options", SignalConfidence.MEDIUM)))
        assertFalse(result.shouldAlert)
        assertTrue(result.confidence != SignalConfidence.HIGH)
    }

    @Test
    fun `vpn plus advanced configuration can trigger correlated alert`() {
        val result = EvasionRiskEngine.evaluate(
            listOf(
                signal("vpn_active", SignalConfidence.HIGH),
                signal("adb_enabled", SignalConfidence.MEDIUM)
            )
        )
        assertTrue(result.shouldAlert)
        assertTrue(result.score >= 70)
        assertTrue(result.confidence == SignalConfidence.HIGH)
    }

    @Test
    fun `duplicate technical signals are counted once`() {
        val result = EvasionRiskEngine.evaluate(
            listOf(
                signal("developer_options", SignalConfidence.MEDIUM),
                signal("developer_options", SignalConfidence.MEDIUM)
            )
        )
        assertFalse(result.shouldAlert)
    }
}
