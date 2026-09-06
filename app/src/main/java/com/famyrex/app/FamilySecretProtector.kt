package com.famyrex.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts the persisted family binding secret with an Android Keystore key. */
class FamilySecretProtector(context: Context) {
    private val appContext = context.applicationContext

    fun encrypt(secret: String): String {
        val cipher = Cipher.getInstance(FamilySecretCrypto.TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(secret.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(FamilySecretCrypto.pack(cipher.iv, encrypted), Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String? = runCatching {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        val parts = FamilySecretCrypto.unpack(payload)
        val cipher = Cipher.getInstance(FamilySecretCrypto.TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(),
            GCMParameterSpec(FamilySecretCrypto.GCM_TAG_LENGTH, parts.iv)
        )
        String(cipher.doFinal(parts.ciphertext), StandardCharsets.UTF_8)
    }.getOrNull()

    private fun key(): SecretKey {
        val keyStore = java.security.KeyStore.getInstance(FamilySecretCrypto.ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(FamilySecretCrypto.KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, FamilySecretCrypto.ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                FamilySecretCrypto.KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }
}

internal object FamilySecretCrypto {
    const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val KEY_ALIAS = "famyrex_family_binding"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    const val GCM_IV_LENGTH = 12
    const val GCM_TAG_LENGTH = 128

    data class PayloadParts(val iv: ByteArray, val ciphertext: ByteArray)

    fun pack(iv: ByteArray, ciphertext: ByteArray): ByteArray {
        require(iv.size == GCM_IV_LENGTH)
        require(ciphertext.isNotEmpty())
        return iv + ciphertext
    }

    fun unpack(payload: ByteArray): PayloadParts {
        require(payload.size > GCM_IV_LENGTH)
        return PayloadParts(
            iv = payload.copyOfRange(0, GCM_IV_LENGTH),
            ciphertext = payload.copyOfRange(GCM_IV_LENGTH, payload.size)
        )
    }
}
