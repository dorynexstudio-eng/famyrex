package com.famyrex.app

import android.content.Context

class ProtectionSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_protection_settings", Context.MODE_PRIVATE)

    fun load(): ProtectionSettings = ProtectionSettings(
        nightStartMinutes = prefs.getInt("nightStart", 0),
        nightEndMinutes = prefs.getInt("nightEnd", 360),
        nightMinutesThreshold = prefs.getLong("nightThreshold", 30L),
        dailyMinutesThreshold = prefs.getLong("dailyThreshold", 240L),
        appSpikePercent = prefs.getInt("spikePercent", 60),
        sensitivity = prefs.getInt("sensitivity", 2).coerceIn(1, 3)
    )

    fun save(settings: ProtectionSettings) {
        prefs.edit()
            .putInt("nightStart", settings.nightStartMinutes.coerceIn(0, 1439))
            .putInt("nightEnd", settings.nightEndMinutes.coerceIn(0, 1439))
            .putLong("nightThreshold", settings.nightMinutesThreshold.coerceAtLeast(1L))
            .putLong("dailyThreshold", settings.dailyMinutesThreshold.coerceAtLeast(1L))
            .putInt("spikePercent", settings.appSpikePercent.coerceAtLeast(1))
            .putInt("sensitivity", settings.sensitivity.coerceIn(1, 3))
            .apply()
    }
}
