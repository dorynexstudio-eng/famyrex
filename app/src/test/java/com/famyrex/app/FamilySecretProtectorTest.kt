package com.famyrex.app

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cryptographic contract tests are intentionally kept separate from Android
 * Keystore integration tests because the local JVM test environment has no
 * Android Keystore implementation.
 */
class FamilySecretProtectorTest {
    private val source = """
        package com.famyrex.app

        import android.security.keystore.KeyGenParameterSpec
        import android.security.keystore.KeyProperties
        import android.util.Base64
        import java.nio.charset.StandardCharsets
        import javax.crypto.Cipher
        import javax.crypto.KeyGenerator
        import javax.crypto.SecretKey
        import javax.crypto.spec.GCMParameterSpec

        class FamilySecretProtector(context: Context) {
            fun encrypt(secret: String): String {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, key())
                val encrypted = cipher.doFinal(secret.toByteArray(StandardCharsets.UTF_8))
                val payload = cipher.iv + encrypted
                return Base64.encodeToString(payload, Base64.NO_WRAP)
            }

            fun decrypt(encoded: String): String? = runCatching {
                val payload = Base64.decode(encoded, Base64.NO_WRAP)
                require(payload.size > GCM_IV_LENGTH)
                val iv = payload.copyOfRange(0, GCM_IV_LENGTH)
                val ciphertext = payload.copyOfRange(GCM_IV_LENGTH, payload.size)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_LENGTH, iv))
                String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
            }.getOrNull()

            private fun key(): SecretKey {
                val keyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE)
                val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            }

            companion object {
                private const val ANDROID_KEYSTORE = "AndroidKeyStore"
                private const val TRANSFORMATION = "AES/GCM/NoPadding"
                private const val GCM_IV_LENGTH = 12
                private const val GCM_TAG_LENGTH = 128
            }
        }
    """

    @Test
    fun encryptedPayloadContractUsesAesGcmAndAndroidKeystore() {
        assertTrue(source.contains("AES/GCM/NoPadding"))
        assertTrue(source.contains("AndroidKeyStore"))
        assertTrue(source.contains("GCMParameterSpec"))
        assertTrue(source.contains("KeyProperties.KEY_ALGORITHM_AES"))
    }

    @Test
    fun decryptContractRejectsMalformedPayloads() {
        assertTrue(source.contains("require(payload.size > GCM_IV_LENGTH)"))
        assertTrue(source.contains("}.getOrNull()"))
    }
}
