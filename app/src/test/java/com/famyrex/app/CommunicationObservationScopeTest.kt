package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationObservationScopeTest {
    @Test
    fun `only observations from requested source are returned`() {
        val observations = listOf(
            CommunicationObservation(1L, "messaging.a", "uno", true),
            CommunicationObservation(2L, "messaging.b", "dos", true),
            CommunicationObservation(3L, "messaging.a", "tres", true)
        )

        val result = CommunicationObservationScope.forSource(observations, "messaging.a")

        assertEquals(listOf("uno", "tres"), result.map { it.normalizedText })
    }

    @Test
    fun `unknown source does not receive observations from other apps`() {
        val observations = listOf(
            CommunicationObservation(1L, "messaging.a", "uno", true),
            CommunicationObservation(2L, "messaging.b", "dos", true)
        )

        val result = CommunicationObservationScope.forSource(observations, "messaging.missing")

        assertTrue(result.isEmpty())
    }
}
