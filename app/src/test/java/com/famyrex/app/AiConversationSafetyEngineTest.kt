package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiConversationSafetyEngineTest {
    private val chatGpt = "com.openai.chatgpt"

    @Test
    fun `explicit personal self harm is high confidence`() {
        val signals = AiConversationSafetyEngine.analyze(
            "Me quiero suicidar y no sé qué hacer",
            sourcePackage = chatGpt
        )

        assertEquals(1, signals.size)
        assertEquals(CommunicationRiskType.SELF_HARM, signals.first().type)
        assertEquals(RiskConfidence.HIGH, signals.first().confidence)
    }

    @Test
    fun `social isolation becomes wellbeing signal`() {
        val signals = AiConversationSafetyEngine.analyze(
            "¿Es normal que en clase todas mis amigas me estén dejando de lado?",
            sourcePackage = chatGpt
        )

        assertTrue(signals.any { it.type == CommunicationRiskType.SOCIAL_ISOLATION })
        assertEquals(RiskConfidence.MEDIUM, signals.first().confidence)
    }

    @Test
    fun `fictional self harm example is ignored`() {
        val signals = AiConversationSafetyEngine.analyze(
            "Para una novela, el personaje dice: quiero suicidarme al final de la historia",
            sourcePackage = chatGpt
        )

        assertTrue(signals.isEmpty())
    }

    @Test
    fun `academic question is not treated as personal danger`() {
        val signals = AiConversationSafetyEngine.analyze(
            "Para una tarea, ¿qué significa no quiero vivir en una novela?",
            sourcePackage = chatGpt
        )

        assertTrue(signals.isEmpty())
    }

    @Test
    fun `unknown source is still analyzable but source is preserved`() {
        val signals = AiConversationSafetyEngine.analyze(
            "No quiero que mis padres sepan esto",
            sourcePackage = "example.app"
        )

        assertEquals(1, signals.size)
        assertEquals("example.app", signals.first().sourcePackage)
        assertEquals(CommunicationRiskType.SECRET_KEEPING, signals.first().type)
    }

    @Test
    fun `empty text produces no signal`() {
        assertTrue(AiConversationSafetyEngine.analyze("   ").isEmpty())
    }

    @Test
    fun `assistant package detection includes Gemini`() {
        assertTrue(AiConversationSafetyEngine.isAiAssistantPackage("com.google.android.apps.gemini"))
    }

    @Test
    fun `normal wellbeing question without risk phrase stays empty`() {
        val signals = AiConversationSafetyEngine.analyze(
            "¿Cómo puedo estudiar mejor para el examen de matemáticas?",
            sourcePackage = chatGpt
        )

        assertTrue(signals.isEmpty())
    }
}
