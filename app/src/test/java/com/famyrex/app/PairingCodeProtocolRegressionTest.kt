package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingCodeProtocolRegressionTest {
    @Test
    fun overlongCodeIsNotAValidFormat() {
        assertFalse(PairingCodeProtocol.isValidFormat("1234567"))
    }

    @Test
    fun sixDigitCodeIsAValidFormat() {
        assertTrue(PairingCodeProtocol.isValidFormat("123456"))
    }
}
