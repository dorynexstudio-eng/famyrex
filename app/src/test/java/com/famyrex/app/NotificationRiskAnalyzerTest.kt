package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRiskAnalyzerTest {
    @Test
    fun blankNotificationProducesNoSignals() {
        assertTrue(NotificationRiskAnalyzer.analyze("   ").isEmpty())
    }

    @Test
    fun normalNotificationProducesNoSignals() {
        assertTrue(NotificationRiskAnalyzer.analyze("Mañana quedamos para estudiar matemáticas").isEmpty())
    }

    @Test
    fun secretRequestProducesSecretKeepingSignal() {
        val signals = NotificationRiskAnalyzer.analyze("No se lo digas a nadie, es nuestro secreto")
        assertTrue(signals.any { it.type == CommunicationRiskType.SECRET_KEEPING })
    }

    @Test
    fun intimateRequestProducesSexualSignal() {
        val signals = NotificationRiskAnalyzer.analyze("Manda nudes")
        assertTrue(signals.any { it.type == CommunicationRiskType.SEXUAL_REQUEST })
    }

    @Test
    fun analyzerDoesNotPersistOrReturnOriginalText() {
        val text = "Manda nudes y no se lo digas a nadie"
        val signals = NotificationRiskAnalyzer.analyze(text, nowMs = 1234L)
        assertTrue(signals.isNotEmpty())
        assertTrue(signals.all { it.reason != text })
        assertEquals(1234L, signals.first().timestampMs)
    }
}
