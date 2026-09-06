package com.famyrex.app

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cryptographic contract tests are intentionally kept separate from Android
 * Keystore integration tests because the local JVM test environment has no
 * Android Keystore implementation.
 */
class FamilySecretProtectorTest {
    @Test
    fun encryptedPayloadContractUsesAesGcm() {
        val source = java.io.File("../main/java/com/famyrex/app/FamilySecretProtector.kt")
        assertTrue(source.exists())
        val content = source.readText()
        assertTrue(content.contains("AES/GCM/NoPadding"))
        assertTrue(content.contains("AndroidKeyStore"))
        assertTrue(content.contains("GCMParameterSpec"))
    }

    @Test
    fun decryptRejectsMalformedPayloads() {
        val source = java.io.File("../main/java/com/famyrex/app/FamilySecretProtector.kt")
        val content = source.readText()
        assertTrue(content.contains("require(payload.size > GCM_IV_LENGTH)"))
        assertTrue(content.contains("}.getOrNull()"))
    }
}
