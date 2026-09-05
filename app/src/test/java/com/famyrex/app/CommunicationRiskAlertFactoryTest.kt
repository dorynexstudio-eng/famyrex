package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationRiskAlertFactoryTest {
    @Test
    fun selfHarmAlertIsImportantAndIncludesImmediateGuidance() {
        val alert = CommunicationRiskAlertFactory.createIncidentAlert(
            incident(
                type = CommunicationRiskType.SELF_HARM,
                confidence = RiskConfidence.HIGH,
                score = 100
            )
        )

        assertEquals("Posible riesgo de autolesión", alert.title)
        assertEquals(AlertSeverity.IMPORTANT, alert.severity)
        assertTrue(alert.message.contains("Habla con el menor cuanto antes"))
        assertTrue(alert.message.contains("peligro inmediato"))
    }

    @Test
    fun socialIsolationAlertIsAttentionAndIncludesSupportGuidance() {
        val alert = CommunicationRiskAlertFactory.createIncidentAlert(
            incident(
                type = CommunicationRiskType.SOCIAL_ISOLATION,
                confidence = RiskConfidence.MEDIUM,
                score = 50
            )
        )

        assertEquals("Posible aislamiento social", alert.title)
        assertEquals(AlertSeverity.ATTENTION, alert.severity)
        assertTrue(alert.message.contains("Habla con el menor con calma"))
        assertTrue(alert.message.contains("no", ignoreCase = true))
    }

    @Test
    fun alertsDoNotExposeRawConversationText() {
        val rawText = "quiero suicidarme porque nadie me entiende"
        val alert = CommunicationRiskAlertFactory.createIncidentAlert(
            incident(
                type = CommunicationRiskType.SELF_HARM,
                confidence = RiskConfidence.HIGH,
                score = 100,
                reason = rawText
            )
        )

        assertTrue(!alert.message.contains(rawText))
    }

    private fun incident(
        type: CommunicationRiskType,
        confidence: RiskConfidence,
        score: Int,
        reason: String = "Señal compatible detectada"
    ) = CommunicationRiskIncident(
        id = "test-incident",
        createdAtMs = 1_760_000_000_000,
        type = type,
        confidence = confidence,
        score = score,
        reasons = listOf(RiskReason("TEST", "Señal detectada", reason)),
        sourcePackage = "com.openai.chatgpt"
    )
}
