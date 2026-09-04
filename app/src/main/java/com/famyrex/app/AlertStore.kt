package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AlertStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_alerts", Context.MODE_PRIVATE)

    fun save(alerts: List<SmartAlert>) {
        val array = JSONArray()
        alerts.take(100).forEach { a ->
            array.put(JSONObject().apply {
                put("id", a.id)
                put("type", a.type.name)
                put("severity", a.severity.name)
                put("title", a.title)
                put("message", a.message)
                put("date", a.date)
                put("packageName", a.packageName ?: "")
            })
        }
        prefs.edit().putString("alerts", array.toString()).apply()
    }

    /** Adds an alert only once, preventing repeated watchdog notifications. */
    fun appendIfNew(alert: SmartAlert): Boolean {
        val existing = load()
        if (existing.any { it.id == alert.id }) return false
        save(listOf(alert) + existing)
        return true
    }

    fun load(): List<SmartAlert> {
        val raw = prefs.getString("alerts", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        SmartAlert(
                            id = o.getString("id"),
                            type = AlertType.valueOf(o.getString("type")),
                            severity = AlertSeverity.valueOf(o.getString("severity")),
                            title = o.getString("title"),
                            message = o.getString("message"),
                            date = o.getString("date"),
                            packageName = o.optString("packageName").ifBlank { null }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
