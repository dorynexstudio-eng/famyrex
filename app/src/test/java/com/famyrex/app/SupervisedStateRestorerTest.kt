package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupervisedStateRestorerTest {
    private val validIdentity = VerifiedFamilyIdentity(
        familyId = "family-1",
        secret = "0123456789abcdef0123456789abcdef",
        fingerprint = "0123456789ab",
        verifiedAtMs = 123L
    )

    @Test
    fun unlinkedOrMissingIdentityCannotRestore() {
        assertFalse(SupervisedStateRestorer.isRestorable(null, linkedDevice = true))
        assertFalse(SupervisedStateRestorer.isRestorable(validIdentity, linkedDevice = false))
    }

    @Test
    fun validLinkedIdentityCanRestore() {
        assertTrue(SupervisedStateRestorer.isRestorable(validIdentity, linkedDevice = true))
    }

    @Test
    fun malformedIdentityCannotRestore() {
        val malformed = validIdentity.copy(secret = "short")
        assertFalse(SupervisedStateRestorer.isRestorable(malformed, linkedDevice = true))
    }
}
