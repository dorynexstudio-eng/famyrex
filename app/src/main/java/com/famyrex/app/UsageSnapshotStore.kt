package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

data class DailyUsage(
    val date: String,
    val totalTimeMs: Long,
    val topApps: List<AppUsage>
)

class UsageSnapshotStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_usage", Context.MODE_PRIVATE)

    fun save(items: List<AppUsage>) {
        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        val history = loadHistory().toMutableList()
        val current = DailyUsage(today, items.sumOf { it.totalTimeMs }, items.take(20))
        val index = history.indexOfFirst { it.date == today }
        if (index >= 0) history[index] = current else history.add(current)

        val array = JSONArray()
        history.sortedByDescending { it.date }.take(60).forEach { day ->
            array.put(JSONObject().apply {
                put("date", day.date)
                put("totalTimeMs", day.totalTimeMs)
                put("topApps", JSONArray().apply {
                    day.topApps.forEach { app ->
                        put(JSONObject().apply {
                            put("packageName", app.packageName)
                            put("totalTimeMs", app.totalTimeMs)
                            put("label", app.label)
                        })
                    }
                })
            })
        }
        prefs.edit()
            .putLong("lastSnapshotAt", System.currentTimeMillis())
            .putString("history", array.toString())
            .apply()
    }

    fun loadHistory(): List<DailyUsage> {
        val raw = prefs.getString("history", null) ?: return emptyList()
        return parseHistory(raw)
    }

    companion object {
        internal fun parseHistory(raw: String): List<DailyUsage> {
            val array = try {
                JSONArray(raw)
            } catch (_: Exception) {
                return emptyList()
            }

            val history = mutableListOf<DailyUsage>()
            for (i in 0 until array.length()) {
                val day = try {
                    val item = array.getJSONObject(i)
                    val date = item.getString("date")
                    val totalTimeMs = item.getLong("totalTimeMs")
                    val apps = item.optJSONArray("topApps")
                    val top = mutableListOf<AppUsage>()
                    if (apps != null) {
                        for (j in 0 until apps.length()) {
                            try {
                                val app = apps.getJSONObject(j)
                                val packageName = app.getString("packageName")
                                val total = app.getLong("totalTimeMs")
                                val label = app.optString("label").ifBlank { packageName }
                                top.add(AppUsage(packageName, total, label))
                            } catch (_: Exception) {
                                // Preserve the day and every other valid app.
                            }
                        }
                    }
                    DailyUsage(date, totalTimeMs, top)
                } catch (_: Exception) {
                    null
                }
                if (day != null) history.add(day)
            }
            return history
        }
    }
}
