package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class HourlyUsageSnapshot(
    val hour: String,
    val totalTimeMs: Long,
    val topApps: List<AppUsage>
)

class HourlyUsageSnapshotStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_hourly_usage", Context.MODE_PRIVATE)
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun save(items: List<AppUsage>, now: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())) {
        val hour = now.withMinute(0).withSecond(0).withNano(0).format(formatter)
        val history = loadHistory().toMutableList()
        val snapshot = HourlyUsageSnapshot(hour, items.sumOf { it.totalTimeMs }, items.take(20))
        val index = history.indexOfFirst { it.hour == hour }
        if (index >= 0) history[index] = snapshot else history.add(snapshot)

        val array = JSONArray()
        history.sortedByDescending { it.hour }.take(MAX_SNAPSHOTS).forEach { item ->
            array.put(JSONObject().apply {
                put("hour", item.hour)
                put("totalTimeMs", item.totalTimeMs)
                put("topApps", JSONArray().apply {
                    item.topApps.forEach { app ->
                        put(JSONObject().apply {
                            put("packageName", app.packageName)
                            put("totalTimeMs", app.totalTimeMs)
                            put("label", app.label)
                        })
                    }
                })
            })
        }
        prefs.edit().putString("history", array.toString()).apply()
    }

    fun loadHistory(): List<HourlyUsageSnapshot> =
        prefs.getString("history", null)?.let(::parseHistory).orEmpty()

    companion object {
        private const val MAX_SNAPSHOTS = 24 * 14

        internal fun parseHistory(raw: String): List<HourlyUsageSnapshot> {
            val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
            val result = mutableListOf<HourlyUsageSnapshot>()
            for (i in 0 until array.length()) {
                val snapshot = runCatching {
                    val item = array.getJSONObject(i)
                    val hour = item.getString("hour")
                    LocalDateTime.parse(hour, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    val total = item.getLong("totalTimeMs")
                    require(total >= 0)
                    val apps = item.optJSONArray("topApps")
                    val top = mutableListOf<AppUsage>()
                    if (apps != null) {
                        for (j in 0 until apps.length()) {
                            runCatching {
                                val app = apps.getJSONObject(j)
                                val pkg = app.getString("packageName")
                                val appTotal = app.getLong("totalTimeMs")
                                require(pkg.isNotBlank() && appTotal >= 0)
                                top += AppUsage(pkg, appTotal, app.optString("label").ifBlank { pkg })
                            }
                        }
                    }
                    HourlyUsageSnapshot(hour, total, top)
                }.getOrNull()
                if (snapshot != null) result += snapshot
            }
            return result
        }
    }
}
