package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FamilyZoneStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_family_zones", Context.MODE_PRIVATE)

    fun load(): List<FamilyZone> {
        val raw = prefs.getString("zones", null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(raw)
            buildList {
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    add(
                        FamilyZone(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            latitude = o.optDouble("latitude"),
                            longitude = o.optDouble("longitude"),
                            radiusMeters = o.optDouble("radiusMeters", 150.0).toFloat(),
                            enabled = o.optBoolean("enabled", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(zone: FamilyZone) {
        val updated = (load().filterNot { it.id == zone.id } + zone).takeLast(100)
        val a = JSONArray()
        updated.forEach {
            a.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("latitude", it.latitude)
                put("longitude", it.longitude)
                put("radiusMeters", it.radiusMeters)
                put("enabled", it.enabled)
            })
        }
        prefs.edit().putString("zones", a.toString()).apply()
    }

    fun delete(id: String) {
        val a = JSONArray()
        load().filterNot { it.id == id }.forEach {
            a.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("latitude", it.latitude)
                put("longitude", it.longitude)
                put("radiusMeters", it.radiusMeters)
                put("enabled", it.enabled)
            })
        }
        prefs.edit().putString("zones", a.toString()).apply()
    }
}
