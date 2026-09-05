package com.famyrex.app

import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyStoreBindingTest {
    @Test
    fun verifiedFamilyIdentityModelKeepsAllBindingParts() {
        val identity = VerifiedFamilyIdentity("family-1", "0123456789abcdef0123456789abcdef", "0123456789ab", 123L)
        assertEquals("family-1", identity.familyId)
        assertEquals(32, identity.secret.length)
        assertEquals(12, identity.fingerprint.length)
        assertEquals(123L, identity.verifiedAtMs)
    }

    @Test
    fun invalidBindingShapeIsNotAcceptedByProtocol() {
        assertNull(OfflinePairingTokenCodec.verify("family-1:short:999999999999", "123456", 1L))
    }
}
