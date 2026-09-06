package com.famyrex.app

import android.content.Context

/** Persists the explicit in-app disclosure acknowledgement for the parental accessibility service. */
class AccessibilityConsentStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_accessibility_consent", Context.MODE_PRIVATE)

    fun isAccepted(): Boolean = prefs.getBoolean(KEY_ACCEPTED, false)

    fun accept() {
        prefs.edit().putBoolean(KEY_ACCEPTED, true).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_ACCEPTED).apply()
    }

    companion object {
        private const val KEY_ACCEPTED = "accepted"
    }
}
