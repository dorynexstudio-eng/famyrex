package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationNotificationDeduplicatorTest {
    @Test
    fun `same notification and same text is ignored`() {
        val deduplicator = CommunicationNotificationDeduplicator()

        assertTrue(deduplicator.shouldProcess("key", "messaging.a", "hola"))
        assertFalse(deduplicator.shouldProcess("key", "messaging.a", "hola"))
    }

    @Test
    fun `updated text from same notification is processed`() {
        val deduplicator = CommunicationNotificationDeduplicator()

        assertTrue(deduplicator.shouldProcess("key", "messaging.a", "hola"))
        assertTrue(deduplicator.shouldProcess("key", "messaging.a", "adios"))
    }

    @Test
    fun `same key in different apps is independent`() {
        val deduplicator = CommunicationNotificationDeduplicator()

        assertTrue(deduplicator.shouldProcess("key", "messaging.a", "hola"))
        assertTrue(deduplicator.shouldProcess("key", "messaging.b", "hola"))
    }
}
