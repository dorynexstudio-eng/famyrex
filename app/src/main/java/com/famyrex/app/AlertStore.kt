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
        val ids = alerts.take(100).mapTo(hashSetOf()) { it.id }
        val delivered = loadDeliveredIds().filterTo(hashSetOf()) { it in ids }
        prefs.edit()
            .putString("alerts", array.toString())
            .putStringSet(KEY_NOTIFICATION_DELIVERED, delivered)
            .apply()
    }

    fun appendIfNew(alert: SmartAlert): Boolean {
        val current = load()
        val existing = current.firstOrNull { it.id == alert.id }
        if (existing != null) {
            val refreshed = alert.copy(lifecycleStatus = existing.lifecycleStatus)
            if (existing != refreshed) {
                save(current.map { if (it.id == alert.id) refreshed else it })
            }
            return false
        }
        save(listOf(alert) + current)
        return true
    }

    fun replace(alert: SmartAlert): Boolean {
        val current = load()
        if (current.none { it.id == alert.id }) return false
        save(current.map { if (it.id == alert.id) alert else it })
        if (alert.lifecycleStatus.isTerminal()) clearNotificationDelivered(alert.id)
        return true
    }

    fun mergeById(incoming: List<SmartAlert>) {
        if (incoming.isEmpty()) return
        val current = load()
        val incomingById = incoming.associateBy { it.id }
        val merged = current.map { existing ->
            incomingById[existing.id]?.let { refreshed ->
                refreshed.copy(lifecycleStatus = existing.lifecycleStatus)
            } ?: existing
        }.toMutableList()
        val existingIds = current.mapTo(hashSetOf()) { it.id }
        incoming.forEach { alert ->
            if (alert.id !in existingIds) merged.add(0, alert)
        }
        save(merged)
    }

    fun load(): List<SmartAlert> {
        val raw = prefs.getString("alerts", null) ?: return emptyList()
        return parseAlertsJson(raw)
    }

    fun isNotificationDelivered(alertId: String): Boolean =
        alertId in loadDeliveredIds()

    fun markNotificationDelivered(alertId: String) {
        val delivered = loadDeliveredIds()
        if (delivered.add(alertId)) {
            prefs.edit().putStringSet(KEY_NOTIFICATION_DELIVERED, delivered).apply()
        }
    }

    fun clearNotificationDelivered(alertId: String) {
        val delivered = loadDeliveredIds()
        if (delivered.remove(alertId)) {
            prefs.edit().putStringSet(KEY_NOTIFICATION_DELIVERED, delivered).apply()
        }
    }

    private fun loadDeliveredIds(): MutableSet<String> =
        prefs.getStringSet(KEY_NOTIFICATION_DELIVERED, emptySet())?.toMutableSet() ?: mutableSetOf()

    private fun AlertLifecycleStatus.isTerminal(): Boolean = when (this) {
        AlertLifecycleStatus.DISMISSED,
        AlertLifecycleStatus.AUTO_DISMISSED,
        AlertLifecycleStatus.RESOLVED -> true
        AlertLifecycleStatus.DETECTED,
        AlertLifecycleStatus.REVIEWED,
        AlertLifecycleStatus.CONFIRMED -> false
    }

    companion object {
        private const val KEY_NOTIFICATION_DELIVERED = "notification_delivered"
    }
}

internal fun parseAlertsJson(raw: String): List<SmartAlert> {
    val array = try {
        JSONArray(raw)
    } catch (_: Exception) {
        return emptyList()
    }

    val alerts = mutableListOf<SmartAlert>()
    for (i in 0 until array.length()) {
        try {
            val o = array.getJSONObject(i)
            val lifecycle = try {
                AlertLifecycleStatus.valueOf(o.optString("lifecycleStatus"))
            } catch (_: Exception) {
                AlertLifecycleStatus.DETECTED
            }
            alerts.add(
                SmartAlert(
                    id = o.getString("id"),
                    type = AlertType.valueOf(o.getString("type")),
                    severity = AlertSeverity.valueOf(o.getString("severity")),
                    title = o.getString("title"),
                    message = o.getString("message"),
                    date = o.getString("date"),
                    packageName = o.optString("packageName").ifBlank { null },
                    lifecycleStatus = lifecycle
                )
            )
        } catch (_: Exception) {
            // Preserve every valid alert even when one record is corrupt.
        }
    }
    return alerts
}
