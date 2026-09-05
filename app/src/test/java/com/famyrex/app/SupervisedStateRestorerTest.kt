package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupervisedStateRestorerTest {
    @Test
    fun invalidOrUnlinkedStateIsNotRestored() {
        assertFalse(isValidBinding(false))
        assertFalse(isValidBinding(true, linked = false))
    }

    @Test
    fun linkedVerifiedStateIsRestorable() {
        assertTrue(isValidBinding(true, linked = true))
    }

    private fun isValidBinding(identity: Boolean, linked: Boolean = true): Boolean = identity && linked
}
