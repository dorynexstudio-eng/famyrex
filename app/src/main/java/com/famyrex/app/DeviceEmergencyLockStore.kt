package com.famyrex.app

import android.content.Context

/** Persistent, explicit remote lock state bound to the supervised device identity. */
class DeviceEmergencyLockStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isLocked(deviceId: String): Boolean {
        val normalizedDeviceId = deviceId.trim()
        if (normalizedDeviceId.isBlank()) return false
        if (prefs.getString(KEY_DEVICE_ID, null) != normalizedDeviceId) return false
        return prefs.getBoolean(KEY_LOCKED, false)
    }

    fun setLocked(deviceId: String, locked: Boolean) {
        val normalizedDeviceId = deviceId.trim()
        if (normalizedDeviceId.isBlank()) return
        prefs.edit()
            .putString(KEY_DEVICE_ID, normalizedDeviceId)
            .putBoolean(KEY_LOCKED, locked)
            .commit()
    }

    /** Removes persisted lock state when the supervised enrollment is reset. */
    fun clear() {
        prefs.edit().clear().commit()
    }

    companion object {
        private const val PREFS = "famyrex_emergency_lock"
        private const val KEY_LOCKED = "locked"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
