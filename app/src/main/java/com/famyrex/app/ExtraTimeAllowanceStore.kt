package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Stores a parent-granted screen-time allowance for the current local day. */
class ExtraTimeAllowanceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun grantedMinutes(today: LocalDate = LocalDate.now()): Int {
        if (prefs.getString(KEY_DATE, null) != today.toString()) return 0
        return prefs.getInt(KEY_MINUTES, 0).coerceAtLeast(0)
    }

    /**
     * Grants extra time exactly once per command ID. The allowance and the processed-command
     * marker are committed in the same SharedPreferences transaction, making a retry after a
     * process death idempotent instead of granting the same command twice.
     */
    @Synchronized
    fun grantMinutes(minutes: Int, commandId: String, today: LocalDate = LocalDate.now()): Boolean {
        if (minutes !in 1..1440 || commandId.isBlank()) return false

        val storedDate = prefs.getString(KEY_DATE, null)
        val processed = if (storedDate == today.toString()) loadProcessedCommandIds() else emptyList()
        if (commandId in processed) return true

        val current = if (storedDate == today.toString()) prefs.getInt(KEY_MINUTES, 0).coerceAtLeast(0) else 0
        val total = (current + minutes).coerceAtMost(1440)
        val updatedProcessed = (processed + commandId).takeLast(MAX_PROCESSED_COMMANDS)
        val processedJson = JSONArray().apply {
            updatedProcessed.forEach { put(it) }
        }

        return prefs.edit()
            .putString(KEY_DATE, today.toString())
            .putInt(KEY_MINUTES, total)
            .putString(KEY_PROCESSED_COMMANDS, processedJson.toString())
            .commit()
    }

    /** Legacy local API; remote commands should use the command-id overload. */
    @Synchronized
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

    private fun loadProcessedCommandIds(): List<String> = runCatching {
        val array = JSONArray(prefs.getString(KEY_PROCESSED_COMMANDS, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }.takeLast(MAX_PROCESSED_COMMANDS)
    }.getOrDefault(emptyList())

    companion object {
        private const val PREFS = "famyrex_extra_time"
        private const val KEY_DATE = "date"
        private const val KEY_MINUTES = "minutes"
        private const val KEY_PROCESSED_COMMANDS = "processed_command_ids"
        private const val MAX_PROCESSED_COMMANDS = 200
    }
}
