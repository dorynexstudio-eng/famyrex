package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationRiskNotificationPolicyTest {
    @Test
    fun newIncident_isNotifiable() {
        assertTrue(CommunicationRiskNotificationPolicy.shouldNotify(null, incident(score = 70)))
    }

    @Test
    fun unchangedUpdate_isSuppressed() {
        val previous = incident(score = 70)
        assertFalse(CommunicationRiskNotificationPolicy.shouldNotify(previous, previous.copy()))
    }

    @Test
    fun smallScoreIncrease_isSuppressed() {
        assertFalse(
            CommunicationRiskNotificationPolicy.shouldNotify(
                incident(score = 70),
                incident(score = 75)
            )
        )
    }

    @Test
    fun tenPointScoreIncrease_isNotifiable() {
        assertTrue(
            CommunicationRiskNotificationPolicy.shouldNotify(
                incident(score = 70),
                incident(score = 80)
            )
        )
    }

    @Test
    fun confidenceEscalation_isNotifiable() {
        assertTrue(
            CommunicationRiskNotificationPolicy.shouldNotify(
                incident(score = 70, confidence = RiskConfidence.MEDIUM),
                incident(score = 71, confidence = RiskConfidence.HIGH)
            )
        )
    }

    @Test
    fun severityEscalation_isNotifiable() {
        assertTrue(
            CommunicationRiskNotificationPolicy.shouldNotify(
                incident(score = 84),
                incident(score = 85)
            )
        )
    }

    @Test
    fun newReasonWithScoreIncrease_isNotifiable() {
        assertTrue(
            CommunicationRiskNotificationPolicy.shouldNotify(
                incident(score = 70, reasons = listOf(reason("GROOMING_SIGNAL"))),
                incident(score = 75, reasons = listOf(reason("GROOMING_SIGNAL"), reason("SECRET_KEEPING")))
            )
        )
    }

    private fun incident(
        score: Int,
        confidence: RiskConfidence = RiskConfidence.MEDIUM,
        reasons: List<RiskReason> = listOf(reason("GROOMING_SIGNAL"))
    ) = CommunicationRiskIncident(
        id = "episode-1",
        createdAtMs = 1_000L,
        type = CommunicationRiskType.GROOMING,
        confidence = confidence,
        score = score,
        reasons = reasons,
        sourcePackage = "com.example.chat",
        direction = CommunicationDirection.INCOMING
    )

    private fun reason(code: String) = RiskReason(code, code, "test")
}
