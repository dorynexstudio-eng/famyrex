package com.famyrex.app

import android.content.Context

class WebSafetySettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_web_safety", Context.MODE_PRIVATE)

    fun load(): WebSafetySettings = WebSafetySettings(
        enabled = prefs.getBoolean("enabled", true),
        blockKnownThreats = prefs.getBoolean("blockKnownThreats", true),
        blockedDomains = prefs.getStringSet("blockedDomains", emptySet())?.map(::normalizeHost)?.toSet().orEmpty(),
        allowedDomains = prefs.getStringSet("allowedDomains", emptySet())?.map(::normalizeHost)?.toSet().orEmpty()
    )

    fun save(settings: WebSafetySettings) {
        prefs.edit()
            .putBoolean("enabled", settings.enabled)
            .putBoolean("blockKnownThreats", settings.blockKnownThreats)
            .putStringSet("blockedDomains", settings.blockedDomains.map(::normalizeHost).filter(String::isNotBlank).toSet())
            .putStringSet("allowedDomains", settings.allowedDomains.map(::normalizeHost).filter(String::isNotBlank).toSet())
            .apply()
    }

    private fun normalizeHost(value: String): String =
        value.trim().lowercase().removePrefix("https://").removePrefix("http://").substringBefore("/")
}
