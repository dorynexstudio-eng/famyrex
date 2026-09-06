package com.famyrex.app

import android.content.Context

class ProtectionSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ProtectionSettings = normalize(
        ProtectionSettings(
            nightStartMinutes = readInt(KEY_NIGHT_START, 0),
            nightEndMinutes = readInt(KEY_NIGHT_END, 360),
            nightMinutesThreshold = readLong(KEY_NIGHT_THRESHOLD, 30L),
            dailyMinutesThreshold = readLong(KEY_DAILY_THRESHOLD, 240L),
            appSpikePercent = readInt(KEY_SPIKE_PERCENT, 60),
            sensitivity = readInt(KEY_SENSITIVITY, 2)
        )
    )

    fun save(settings: ProtectionSettings) {
        val safe = normalize(settings)
        prefs.edit()
            .putInt(KEY_NIGHT_START, safe.nightStartMinutes)
            .putInt(KEY_NIGHT_END, safe.nightEndMinutes)
            .putLong(KEY_NIGHT_THRESHOLD, safe.nightMinutesThreshold)
            .putLong(KEY_DAILY_THRESHOLD, safe.dailyMinutesThreshold)
            .putInt(KEY_SPIKE_PERCENT, safe.appSpikePercent)
            .putInt(KEY_SENSITIVITY, safe.sensitivity)
            .apply()
    }

    private fun readInt(key: String, default: Int): Int =
        runCatching { prefs.getInt(key, default) }.getOrDefault(default)

    private fun readLong(key: String, default: Long): Long =
        runCatching { prefs.getLong(key, default) }.getOrDefault(default)

    companion object {
        private const val PREFS_NAME = "famyrex_protection_settings"
        private const val KEY_NIGHT_START = "nightStart"
        private const val KEY_NIGHT_END = "nightEnd"
        private const val KEY_NIGHT_THRESHOLD = "nightThreshold"
        private const val KEY_DAILY_THRESHOLD = "dailyThreshold"
        private const val KEY_SPIKE_PERCENT = "spikePercent"
        private const val KEY_SENSITIVITY = "sensitivity"

        internal fun normalize(settings: ProtectionSettings): ProtectionSettings = settings.copy(
            nightStartMinutes = settings.nightStartMinutes.coerceIn(0, 1439),
            nightEndMinutes = settings.nightEndMinutes.coerceIn(0, 1439),
            nightMinutesThreshold = settings.nightMinutesThreshold.coerceAtLeast(1L),
            dailyMinutesThreshold = settings.dailyMinutesThreshold.coerceAtLeast(1L),
            appSpikePercent = settings.appSpikePercent.coerceAtLeast(1),
            sensitivity = settings.sensitivity.coerceIn(1, 3)
        )
    }
}
