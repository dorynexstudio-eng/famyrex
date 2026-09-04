package com.famyrex.app

import android.content.Context
import kotlin.random.Random

class PairingCodeStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_pairing", Context.MODE_PRIVATE)

    fun create(ttlMinutes: Long = 10): PairingCode {
        val now = System.currentTimeMillis()
        val code = buildString {
            repeat(6) { append(Random.nextInt(0, 10)) }
        }
        val result = PairingCode(code, now, now + ttlMinutes * 60_000L)
        prefs.edit()
            .putString("code", result.code)
            .putLong("created", result.createdAtMs)
            .putLong("expires", result.expiresAtMs)
            .apply()
        return result
    }

    fun current(now: Long = System.currentTimeMillis()): PairingCode? {
        val code = prefs.getString("code", null) ?: return null
        val created = prefs.getLong("created", 0L)
        val expires = prefs.getLong("expires", 0L)
        if (expires <= now || created <= 0L) {
            clear()
            return null
        }
        return PairingCode(code, created, expires)
    }

    fun consume(input: String, now: Long = System.currentTimeMillis()): Boolean {
        val current = current(now) ?: return false
        val normalized = input.filter(Char::isDigit)
        val ok = normalized.length == 6 && normalized == current.code
        if (ok) clear()
        return ok
    }

    fun clear() {
        prefs.edit().remove("code").remove("created").remove("expires").apply()
    }
}
