package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class UsageIntervalStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_usage_intervals", Context.MODE_PRIVATE)

    fun load(dateKey: String): List<UsageInterval> {
        val raw = prefs.getString(dateKey, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(UsageInterval(
                        timestampMs = obj.optLong("timestampMs"),
                        totalTimeMs = obj.optLong("totalTimeMs")
                    ))
                }
            }.sortedBy { it.timestampMs }
        }.getOrDefault(emptyList())
    }

    fun save(dateKey: String, interval: UsageInterval) {
        val updated = (load(dateKey) + interval)
            .distinctBy { it.timestampMs }
            .sortedBy { it.timestampMs }
            .takeLast(96)
        val arr = JSONArray()
        updated.forEach {
            arr.put(JSONObject().apply {
                put("timestampMs", it.timestampMs)
                put("totalTimeMs", it.totalTimeMs)
            })
        }
        prefs.edit().putString(dateKey, arr.toString()).apply()
    }
}
