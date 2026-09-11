package com.famyrex.app

import android.content.Context
import java.time.LocalDate

/** Stores a parent-granted screen-time allowance for the current local day. */
class ExtraTimeAllowanceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun grantedMinutes(today: LocalDate = LocalDate.now()): Int {
        if (prefs.getString(KEY_DATE, null) != today.toString()) return 0
        return prefs.getInt(KEY_MINUTES, 0).coerceAtLeast(0)
    }

    fun grantMinutes(minutes: Int, today: LocalDate = LocalDate.now()): Boolean {
        if (minutes !in 1..1440) return false
        val current = grantedMinutes(today)
        val total = (current + minutes).coerceAtMost(1440)
        return prefs.edit()
            .putString(KEY_DATE, today.toString())
            .putInt(KEY_MINUTES, total)
            .commit()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    companion object {
        private const val PREFS = "famyrex_extra_time"
        private const val KEY_DATE = "date"
        private const val KEY_MINUTES = "minutes"
    }
}
