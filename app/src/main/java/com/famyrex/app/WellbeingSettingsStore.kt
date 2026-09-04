package com.famyrex.app

import android.content.Context

class WellbeingSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_wellbeing_settings", Context.MODE_PRIVATE)

    fun load(): WellbeingSettings = WellbeingSettings(
        dailyGoalMinutes = prefs.getLong("dailyGoal", 180L),
        breakAfterMinutes = prefs.getLong("breakAfter", 60L),
        breakDurationMinutes = prefs.getLong("breakDuration", 10L),
        nightStartMinutes = prefs.getInt("nightStart", 0),
        nightEndMinutes = prefs.getInt("nightEnd", 360),
        goal = runCatching {
            WellbeingGoal.valueOf(prefs.getString("goal", WellbeingGoal.BALANCED_USE.name)!!)
        }.getOrDefault(WellbeingGoal.BALANCED_USE)
    )

    fun save(settings: WellbeingSettings) {
        prefs.edit()
            .putLong("dailyGoal", settings.dailyGoalMinutes.coerceIn(30L, 1440L))
            .putLong("breakAfter", settings.breakAfterMinutes.coerceIn(15L, 240L))
            .putLong("breakDuration", settings.breakDurationMinutes.coerceIn(1L, 60L))
            .putInt("nightStart", settings.nightStartMinutes.coerceIn(0, 1439))
            .putInt("nightEnd", settings.nightEndMinutes.coerceIn(0, 1439))
            .putString("goal", settings.goal.name)
            .apply()
    }
}
