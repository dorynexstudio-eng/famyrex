package com.famyrex.app

import android.content.Context
import android.content.SharedPreferences

internal interface AccessibilityConsentPreferences {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun setBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

private class SharedPreferencesAccessibilityConsentPreferences(
    private val prefs: SharedPreferences
) : AccessibilityConsentPreferences {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(key, defaultValue)

    override fun setBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

/** Persists the explicit in-app disclosure acknowledgement for the parental accessibility service. */
class AccessibilityConsentStore internal constructor(
    private val preferences: AccessibilityConsentPreferences
) {
    constructor(context: Context) : this(
        SharedPreferencesAccessibilityConsentPreferences(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        )
    )

    fun isAccepted(): Boolean = preferences.getBoolean(KEY_ACCEPTED, false)

    fun accept() {
        preferences.setBoolean(KEY_ACCEPTED, true)
    }

    fun clear() {
        preferences.remove(KEY_ACCEPTED)
    }

    companion object {
        private const val PREFS_NAME = "famyrex_accessibility_consent"
        private const val KEY_ACCEPTED = "accepted"
    }
}
