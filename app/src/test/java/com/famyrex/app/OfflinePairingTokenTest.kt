package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OfflinePairingTokenTest {
    @Test
    fun tokenRoundTripsAndExpires() {
        val token = OfflinePairingTokenCodec.create("family-123", 1_000L, 10)
        val encoded = OfflinePairingTokenCodec.encode(token)
        val decoded = OfflinePairingTokenCodec.decode(encoded, 1_001L)
        assertNotNull(decoded)
        assertEquals(token.familyId, decoded?.familyId)
        assertEquals(token.secret, decoded?.secret)
        assertNull(OfflinePairingTokenCodec.decode(encoded, token.expiresAtMs))
    }

    @Test
    fun generatedSecretsAreDifferent() {
        val first = OfflinePairingTokenCodec.create("family-123", 1_000L)
        val second = OfflinePairingTokenCodec.create("family-123", 1_000L)
        assertNotEquals(first.secret, second.secret)
    }

    @Test
    fun fingerprintIsDeterministic() {
        assertEquals(
            OfflinePairingTokenCodec.fingerprint("abc"),
            OfflinePairingTokenCodec.fingerprint("abc")
        )
    }
}
