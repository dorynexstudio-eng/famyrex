package com.famyrex.app

import android.content.Context

/** Persistent, explicit remote lock state used by the supervised-device guard. */
class DeviceEmergencyLockStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isLocked(): Boolean = prefs.getBoolean(KEY_LOCKED, false)

    fun setLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_LOCKED, locked).apply()
    }

    companion object {
        private const val PREFS = "famyrex_emergency_lock"
        private const val KEY_LOCKED = "locked"
    }
}
