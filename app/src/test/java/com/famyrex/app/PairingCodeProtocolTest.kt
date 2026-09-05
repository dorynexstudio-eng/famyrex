package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingCodeProtocolTest {
    private val code = PairingCode("123456", 1_000L, 2_000L)

    @Test
    fun normalizes_non_digits_and_limits_length() {
        assertTrue(PairingCodeProtocol.normalize("12-34 56") == "123456")
        assertTrue(PairingCodeProtocol.normalize("123456789") == "123456")
    }

    @Test
    fun accepts_matching_code_before_expiry() {
        assertTrue(PairingCodeProtocol.matches("123456", code, 1_999L))
    }

    @Test
    fun rejects_wrong_code() {
        assertFalse(PairingCodeProtocol.matches("654321", code, 1_999L))
    }

    @Test
    fun rejects_expired_code() {
        assertFalse(PairingCodeProtocol.matches("123456", code, 2_000L))
    }
}
