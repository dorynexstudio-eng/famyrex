package com.famyrex.app

import android.content.Context

/**
 * Señal de vida técnica de Famyrex. No afirma que Android vaya a ejecutar
 * WorkManager puntualmente; permite distinguir "última comprobación conocida"
 * de "todo está bien ahora".
 */
data class ProtectionHeartbeat(
    val checkedAtMs: Long,
    val workerSucceeded: Boolean
)

class ProtectionWatchdog(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_protection_watchdog", Context.MODE_PRIVATE)

    fun recordSuccess(timestampMs: Long) {
        prefs.edit()
            .putLong(KEY_LAST_SUCCESS, timestampMs)
            .putBoolean(KEY_LAST_SUCCESSFUL, true)
            .apply()
    }

    fun lastSuccessMs(): Long? =
        if (prefs.contains(KEY_LAST_SUCCESS)) prefs.getLong(KEY_LAST_SUCCESS, 0L) else null

    fun heartbeat(nowMs: Long, staleAfterMs: Long = DEFAULT_STALE_AFTER_MS): ProtectionHeartbeat {
        val last = lastSuccessMs()
        return ProtectionHeartbeat(
            checkedAtMs = last ?: 0L,
            workerSucceeded = last != null && nowMs - last <= staleAfterMs
        )
    }

    companion object {
        private const val KEY_LAST_SUCCESS = "last_success_ms"
        private const val KEY_LAST_SUCCESSFUL = "last_successful"
        const val DEFAULT_STALE_AFTER_MS = 30 * 60_000L
    }
}
