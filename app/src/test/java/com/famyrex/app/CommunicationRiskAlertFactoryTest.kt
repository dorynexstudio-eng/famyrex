package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(alert.message.contains("Intervención prioritaria"))
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
    fun accumulatedSignalsExplainProgressionWithoutRawConversation() {
        val alert = CommunicationRiskAlertFactory.createIncidentAlert(
            CommunicationRiskIncident(
                id = "test-progression",
                createdAtMs = 1_760_000_000_000,
                type = CommunicationRiskType.GROOMING,
                confidence = RiskConfidence.MEDIUM,
                score = 64,
                reasons = listOf(
                    RiskReason("CONTACT", "Posible contacto inapropiado", "contacto nuevo"),
                    RiskReason("PERSONAL_INFO", "Petición de información personal", "pregunta por el colegio")
                ),
                sourcePackage = "test.package"
            )
        )

        assertTrue(alert.message.contains("Evolución"))
        assertTrue(alert.message.contains("Posible contacto inapropiado"))
        assertTrue(alert.message.contains("Petición de información personal"))
        assertTrue(alert.message.contains("no guarda ni muestra la conversación completa"))
        assertFalse(alert.message.contains("mensaje privado de prueba"))
    }

    @Test
    fun earlySingleSignalIsPresentedAsObservationNotCrisis() {
        val alert = CommunicationRiskAlertFactory.createIncidentAlert(
            incident(
                type = CommunicationRiskType.GROOMING,
                confidence = RiskConfidence.LOW,
                score = 4
            )
        )

        assertTrue(alert.message.contains("Señal temprana"))
        assertTrue(alert.message.contains("observar su evolución"))
        assertFalse(alert.message.contains("crisis"))
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
