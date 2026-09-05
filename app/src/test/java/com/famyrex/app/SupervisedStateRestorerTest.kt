package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupervisedStateRestorerTest {
    private val validIdentity = VerifiedFamilyIdentity(
        familyId = "family-1",
        secret = "0123456789abcdef0123456789abcdef",
        fingerprint = "3eb1bd439947",
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
        assertFalse(SupervisedStateRestorer.isRestorable(validIdentity.copy(secret = "short"), true))
        assertFalse(SupervisedStateRestorer.isRestorable(validIdentity.copy(secret = "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"), true))
        assertFalse(SupervisedStateRestorer.isRestorable(validIdentity.copy(fingerprint = "000000000000"), true))
        assertFalse(SupervisedStateRestorer.isRestorable(validIdentity.copy(verifiedAtMs = 0L), true))
    }
}
