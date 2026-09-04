package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class GeofenceEvent(
    val transition: Int,
    val zoneIds: List<String>,
    val timestampMs: Long
)

class GeofenceEventStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_geofence_events", Context.MODE_PRIVATE)

    fun save(transition: Int, zoneIds: List<String>, timestampMs: Long) {
        val raw = prefs.getString("events", null)
        val a = runCatching { if (raw == null) JSONArray() else JSONArray(raw) }
            .getOrElse { JSONArray() }

        val obj = JSONObject()
            .put("transition", transition)
            .put("timestampMs", timestampMs)
            .put("zoneIds", JSONArray(zoneIds))
        a.put(obj)

        val start = maxOf(0, a.length() - 100)
        val trimmed = JSONArray()
        for (i in start until a.length()) trimmed.put(a.get(i))
        prefs.edit().putString("events", trimmed.toString()).apply()
    }

    fun load(): List<GeofenceEvent> {
        val raw = prefs.getString("events", null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(raw)
            buildList {
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val ids = o.optJSONArray("zoneIds")
                    val zones = buildList {
                        if (ids != null) for (j in 0 until ids.length()) add(ids.optString(j))
                    }
                    add(GeofenceEvent(o.optInt("transition"), zones, o.optLong("timestampMs")))
                }
            }
        }.getOrDefault(emptyList())
    }
}
