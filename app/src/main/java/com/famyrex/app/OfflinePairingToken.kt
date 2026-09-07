package com.famyrex.app

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class OfflinePairingToken(
    val familyId: String,
    val childProfileId: String,
    val childDisplayName: String,
    val secret: String,
    val expiresAtMs: Long
)

object OfflinePairingTokenCodec {
    private const val SECRET_BYTES = 16
    private const val CODE_LENGTH = 6

    fun create(
        familyId: String,
        childProfileId: String,
        childDisplayName: String,
        now: Long,
        ttlMinutes: Long = 10
    ): OfflinePairingToken {
        require(familyId.isNotBlank())
        require(childProfileId.isNotBlank())
        require(childDisplayName.isNotBlank())
        require(ttlMinutes > 0)
        val bytes = ByteArray(SECRET_BYTES)
        SecureRandom().nextBytes(bytes)
        val secret = bytes.joinToString("") { "%02x".format(it) }
        return OfflinePairingToken(familyId, childProfileId, childDisplayName.trim(), secret, now + ttlMinutes * 60_000L)
    }

    fun encode(token: OfflinePairingToken): String =
        "${token.familyId}:${token.childProfileId}:${token.childDisplayName.encodeTokenPart()}:${token.secret}:${token.expiresAtMs}"

    fun decode(value: String, now: Long): OfflinePairingToken? {
        val parts = value.trim().split(":")
        if (parts.size != 5) return null
        val expires = parts[4].toLongOrNull() ?: return null
        if (parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank() ||
            parts[3].length != SECRET_BYTES * 2 ||
            !parts[3].all { it.isDigit() || it.lowercaseChar() in 'a'..'f' } || expires <= now
        ) return null
        val displayName = parts[2].decodeTokenPart().takeIf { it.isNotBlank() } ?: return null
        return OfflinePairingToken(parts[0], parts[1], displayName, parts[3].lowercase(), expires)
    }

    fun code(token: OfflinePairingToken): String {
        val payload = "FAMYREX|${token.familyId}|${token.childProfileId}|${token.childDisplayName}|${token.expiresAtMs}"
            .toByteArray(StandardCharsets.UTF_8)
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hexToBytes(token.secret), "HmacSHA256"))
        val digest = mac.doFinal(payload)
        val value = ByteBuffer.wrap(digest.copyOfRange(0, 4)).int.toLong() and 0x7fffffffL
        return value.toString().takeLast(CODE_LENGTH).padStart(CODE_LENGTH, '0')
    }

    fun verify(value: String, expectedCode: String, now: Long): OfflinePairingToken? {
        val token = decode(value, now) ?: return null
        val normalized = expectedCode.filter(Char::isDigit)
        if (normalized.length != CODE_LENGTH) return null
        return token.takeIf { code(it) == normalized }
    }

    fun fingerprint(secret: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray())
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(value: String): ByteArray =
        value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun String.encodeTokenPart(): String =
        java.net.URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

    private fun String.decodeTokenPart(): String =
        runCatching { java.net.URLDecoder.decode(this, StandardCharsets.UTF_8.toString()) }.getOrDefault("")
}
