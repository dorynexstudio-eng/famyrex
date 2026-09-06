package com.famyrex.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FamilySecretProtectorTest {
    @Test
    fun payloadRoundTripKeepsIvAndCiphertextSeparated() {
        val iv = ByteArray(FamilySecretCrypto.GCM_IV_LENGTH) { it.toByte() }
        val ciphertext = byteArrayOf(1, 2, 3, 4, 5)

        val parts = FamilySecretCrypto.unpack(FamilySecretCrypto.pack(iv, ciphertext))

        assertArrayEquals(iv, parts.iv)
        assertArrayEquals(ciphertext, parts.ciphertext)
    }

    @Test
    fun payloadRequiresExactGcmIvLength() {
        assertThrows(IllegalArgumentException::class.java) {
            FamilySecretCrypto.pack(ByteArray(11), byteArrayOf(1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            FamilySecretCrypto.pack(ByteArray(13), byteArrayOf(1))
        }
    }

    @Test
    fun payloadRejectsMissingCiphertext() {
        assertThrows(IllegalArgumentException::class.java) {
            FamilySecretCrypto.pack(ByteArray(FamilySecretCrypto.GCM_IV_LENGTH), byteArrayOf())
        }
        assertThrows(IllegalArgumentException::class.java) {
            FamilySecretCrypto.unpack(ByteArray(FamilySecretCrypto.GCM_IV_LENGTH))
        }
    }

    @Test
    fun cryptoContractUsesAesGcmAndKeystore() {
        assertEquals("AES/GCM/NoPadding", FamilySecretCrypto.TRANSFORMATION)
        assertEquals("AndroidKeyStore", FamilySecretCrypto.ANDROID_KEYSTORE)
        assertEquals(12, FamilySecretCrypto.GCM_IV_LENGTH)
        assertEquals(128, FamilySecretCrypto.GCM_TAG_LENGTH)
    }
}
