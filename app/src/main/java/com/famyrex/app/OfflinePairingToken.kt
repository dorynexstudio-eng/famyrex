package com.famyrex.app

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Compact offline invitation. It contains a family id plus a secret fingerprint.
 * It is intentionally not a network credential and expires quickly.
 */
data class OfflinePairingToken(
    val familyId: String,
    val secret: String,
    val expiresAtMs: Long
)

object OfflinePairingTokenCodec {
    private const val SECRET_BYTES = 16

    fun create(familyId: String, now: Long, ttlMinutes: Long = 10): OfflinePairingToken {
        require(familyId.isNotBlank())
        require(ttlMinutes > 0)
        val bytes = ByteArray(SECRET_BYTES)
        SecureRandom().nextBytes(bytes)
        val secret = bytes.joinToString("") { "%02x".format(it) }
        return OfflinePairingToken(familyId, secret, now + ttlMinutes * 60_000L)
    }

    fun encode(token: OfflinePairingToken): String =
        "${token.familyId}:${token.secret}:${token.expiresAtMs}"

    fun decode(value: String, now: Long): OfflinePairingToken? {
        val parts = value.trim().split(":")
        if (parts.size != 3) return null
        val expires = parts[2].toLongOrNull() ?: return null
        if (parts[0].isBlank() || parts[1].length != SECRET_BYTES * 2 || expires <= now) return null
        return OfflinePairingToken(parts[0], parts[1], expires)
    }

    fun fingerprint(secret: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray())
        return digest.take(6).joinToString("") { "%02x".format(it) }
    }
}
