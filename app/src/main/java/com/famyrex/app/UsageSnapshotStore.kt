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
        /**
         * Parses persisted usage history defensively. A malformed root payload
         * returns no history, while a malformed day or app only discards that
         * affected record and keeps all valid data that can still be recovered.
         */
        internal fun parseHistory(raw: String): List<DailyUsage> = runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    runCatching {
                        val day = array.getJSONObject(i)
                        val date = day.getString("date")
                        val totalTimeMs = day.getLong("totalTimeMs")
                        val apps = day.optJSONArray("topApps") ?: JSONArray()
                        val top = buildList {
                            for (j in 0 until apps.length()) {
                                runCatching {
                                    val a = apps.getJSONObject(j)
                                    val packageName = a.getString("packageName")
                                    val total = a.getLong("totalTimeMs")
                                    val label = a.optString("label").ifBlank { packageName }
                                    add(AppUsage(packageName, total, label))
                                }
                            }
                        }
                        add(DailyUsage(date, totalTimeMs, top))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }
}
