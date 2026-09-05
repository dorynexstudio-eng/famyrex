package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingJoinPolicyTest {
    @Test
    fun sixDigitCodeIsRequired() {
        assertTrue(PairingCodeProtocol.isValidFormat("123456"))
        assertFalse(PairingCodeProtocol.isValidFormat("12345"))
        assertFalse(PairingCodeProtocol.isValidFormat("1234567"))
    }

    @Test
    fun codeMustBeValidAndUnexpiredBeforeConsumption() {
        val code = PairingCode("123456", 1_000L, 2_000L)
        assertTrue(PairingCodeProtocol.matches("123456", code, 1_999L))
        assertFalse(PairingCodeProtocol.matches("123456", code, 2_000L))
        assertFalse(PairingCodeProtocol.matches("654321", code, 1_999L))
    }
}
