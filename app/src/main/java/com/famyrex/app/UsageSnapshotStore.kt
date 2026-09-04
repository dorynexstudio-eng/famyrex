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
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val day = array.getJSONObject(i)
                val apps = day.optJSONArray("topApps") ?: JSONArray()
                val top = buildList {
                    for (j in 0 until apps.length()) {
                        val a = apps.getJSONObject(j)
                        add(AppUsage(a.getString("packageName"), a.getLong("totalTimeMs"), a.optString("label", a.getString("packageName"))))
                    }
                }
                add(DailyUsage(day.getString("date"), day.getLong("totalTimeMs"), top))
            }
        }
    }
}
