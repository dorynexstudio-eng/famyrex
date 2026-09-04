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
                put("lifecycleStatus", a.lifecycleStatus.name)
            })
        }
        prefs.edit().putString("alerts", array.toString()).apply()
    }

    fun appendIfNew(alert: SmartAlert): Boolean {
        val current = load()
        if (current.any { it.id == alert.id }) return false
        save(listOf(alert) + current)
        return true
    }

    fun replace(alert: SmartAlert): Boolean {
        val current = load()
        if (current.none { it.id == alert.id }) return false
        save(current.map { if (it.id == alert.id) alert else it })
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
                            packageName = o.optString("packageName").ifBlank { null },
                            lifecycleStatus = runCatching {
                                AlertLifecycleStatus.valueOf(o.optString("lifecycleStatus"))
                            }.getOrDefault(AlertLifecycleStatus.DETECTED)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
