package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationSignalDetectorTest {
    @Test
    fun `normal meeting alone is ignored`() {
        val summary = CommunicationSignalDetector.detect(listOf(obs(0, "mañana quedamos")))
        assertTrue(summary.signals.isEmpty())
        assertEquals(0, summary.score)
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `personal information alone is low context`() {
        val summary = CommunicationSignalDetector.detect(listOf(obs(0, "dime dónde vives")))
        assertEquals(1, summary.signals.size)
        assertEquals(CommunicationRiskType.GROOMING, summary.signals.first().type)
        assertEquals(RiskConfidence.LOW, summary.signals.first().confidence)
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `personal information plus secret is contextual grooming signal`() {
        val summary = CommunicationSignalDetector.detect(listOf(obs(0, "dime dónde vives"), obs(5 * 60 * 1000L, "no se lo digas a nadie")))
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.GROOMING })
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `sexual request plus secrecy can reach high risk`() {
        val summary = CommunicationSignalDetector.detect(listOf(obs(0, "manda nudes"), obs(2 * 60 * 1000L, "es nuestro secreto")))
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.SEXUAL_REQUEST })
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.SECRET_KEEPING })
        assertTrue(summary.shouldAlert)
        assertEquals(RiskConfidence.HIGH, summary.confidence)
    }

    @Test
    fun `single self harm phrase does not create detector incident`() {
        val summary = CommunicationSignalDetector.detect(listOf(obs(0, "no quiero vivir")))
        assertTrue(summary.signals.isEmpty())
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `repeated self harm expressions become high signal`() {
        val summary = CommunicationSignalDetector.detect(listOf(obs(0, "no quiero vivir"), obs(10 * 60 * 1000L, "me quiero morir")))
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.SELF_HARM })
        assertTrue(!summary.signals.any { it.type == CommunicationRiskType.THREAT })
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `repeated incoming bullying becomes attention signal`() {
        val summary = CommunicationSignalDetector.detect(listOf(
            obs(0, "eres un inútil", incoming = true),
            obs(60 * 1000L, "das asco", incoming = true),
            obs(2 * 60 * 1000L, "nadie te quiere", incoming = true)
        ))
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.BULLYING })
        assertTrue(summary.signals.any { it.direction == CommunicationDirection.INCOMING })
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `outgoing hostile language produces an outgoing signal`() {
        val summary = CommunicationSignalDetector.detect(listOf(
            obs(0, "eres un inútil", incoming = false),
            obs(60 * 1000L, "das asco", incoming = false),
            obs(2 * 60 * 1000L, "nadie te quiere", incoming = false)
        ))
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.BULLYING })
        assertTrue(summary.signals.any { it.direction == CommunicationDirection.OUTGOING })
        assertTrue(summary.signals.none { it.direction == CommunicationDirection.INCOMING })
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `incoming and outgoing hostile language remain distinguishable`() {
        val summary = CommunicationSignalDetector.detect(listOf(
            obs(0, "eres un inútil", incoming = true),
            obs(60 * 1000L, "das asco", incoming = false),
            obs(2 * 60 * 1000L, "nadie te quiere", incoming = true)
        ))
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.BULLYING && it.direction == CommunicationDirection.INCOMING })
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.BULLYING && it.direction == CommunicationDirection.OUTGOING })
    }

    @Test
    fun `old observations outside window are ignored`() {
        val summary = CommunicationSignalDetector.detect(listOf(obs(0, "manda nudes"), obs(31 * 60 * 1000L, "hola")))
        assertTrue(summary.signals.isEmpty())
        assertEquals(0, summary.score)
    }

    @Test
    fun `generic word threat does not trigger`() {
        val summary = CommunicationSignalDetector.detect(listOf(obs(0, "he leído una noticia sobre una amenaza")))
        assertTrue(summary.signals.isEmpty())
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `single romantic conflict is early signal not alert`() {
        val summary = CommunicationSignalDetector.detect(listOf(obs(0, "a mi amiga le gusta el mismo chico")))
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.SOCIAL_CONFLICT })
        assertEquals(RiskConfidence.LOW, summary.signals.first { it.type == CommunicationRiskType.SOCIAL_CONFLICT }.confidence)
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `generic attraction phrase alone is ignored`() {
        val summary = CommunicationSignalDetector.detect(listOf(obs(0, "me gusta un chico")))
        assertTrue(summary.signals.none { it.type == CommunicationRiskType.SOCIAL_CONFLICT })
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `social conflict across three days escalates to medium`() {
        val day = 24 * 60 * 60 * 1000L
        val summary = CommunicationSignalDetector.detect(listOf(
            obs(0, "a mi amiga le gusta el mismo chico"),
            obs(day, "esta celosa por el chico"),
            obs(2 * day, "mis amigas se han puesto en mi contra")
        ))
        val conflict = summary.signals.first { it.type == CommunicationRiskType.SOCIAL_CONFLICT }
        assertEquals(RiskConfidence.MEDIUM, conflict.confidence)
        assertTrue(!summary.shouldAlert)
    }

    @Test
    fun `social conflict plus exclusion escalates but remains contextual`() {
        val day = 24 * 60 * 60 * 1000L
        val summary = CommunicationSignalDetector.detect(listOf(
            obs(0, "a mi amiga le gusta el mismo chico"),
            obs(day, "me dejan de lado")
        ))
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.SOCIAL_CONFLICT })
        assertTrue(summary.signals.any { it.type == CommunicationRiskType.SOCIAL_ISOLATION })
        assertTrue(!summary.shouldAlert)
    }

    private fun obs(timeMs: Long, text: String, incoming: Boolean = false) =
        CommunicationObservation(
            timestampMs = timeMs,
            sourcePackage = "test.package",
            normalizedText = text,
            isIncoming = incoming
        )
}
