package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OfflinePairingTokenTest {
    @Test
    fun tokenRoundTripsAndExpires() {
        val token = OfflinePairingTokenCodec.create("family-123", "child-1", "Mayor", 1_000L, 10)
        val encoded = OfflinePairingTokenCodec.encode(token)
        val decoded = OfflinePairingTokenCodec.decode(encoded, 1_001L)
        assertNotNull(decoded)
        assertEquals(token.familyId, decoded?.familyId)
        assertEquals(token.childProfileId, decoded?.childProfileId)
        assertEquals(token.childDisplayName, decoded?.childDisplayName)
        assertEquals(token.secret, decoded?.secret)
        assertNull(OfflinePairingTokenCodec.decode(encoded, token.expiresAtMs))
    }

    @Test
    fun generatedSecretsAreDifferent() {
        val first = OfflinePairingTokenCodec.create("family-123", "child-1", "Mayor", 1_000L)
        val second = OfflinePairingTokenCodec.create("family-123", "child-1", "Mayor", 1_000L)
        assertNotEquals(first.secret, second.secret)
    }

    @Test
    fun codeBindsFamilyAndChildAndIsVerifiable() {
        val token = OfflinePairingTokenCodec.create("family-123", "child-1", "Mayor", 1_000L, 10)
        val code = OfflinePairingTokenCodec.code(token)
        assertEquals(code, OfflinePairingTokenCodec.code(token))
        assertNotNull(OfflinePairingTokenCodec.verify(OfflinePairingTokenCodec.encode(token), code, 1_001L))
        val otherChild = token.copy(childProfileId = "child-2")
        assertNotEquals(code, OfflinePairingTokenCodec.code(otherChild))
        val wrongCode = if (code == "000000") "000001" else "000000"
        assertNull(OfflinePairingTokenCodec.verify(OfflinePairingTokenCodec.encode(token), wrongCode, 1_001L))
    }

    @Test
    fun fingerprintIsDeterministic() {
        assertEquals(
            OfflinePairingTokenCodec.fingerprint("abc"),
            OfflinePairingTokenCodec.fingerprint("abc")
        )
    }
}
