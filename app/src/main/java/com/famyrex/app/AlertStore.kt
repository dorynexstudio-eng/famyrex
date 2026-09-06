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

    /**
     * Adds a new alert or refreshes an existing alert with the same id.
     * The lifecycle status is always owned by the persisted alert and is
     * preserved across detector regenerations. Returns true only for a new id,
     * so callers can keep notification semantics unchanged.
     */
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
        return true
    }

    /**
     * Merges a detector refresh into the existing alert history by id.
     * Alerts produced by other detectors are never discarded.
     * Existing lifecycle status is preserved when an alert is refreshed.
     */
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
}

/**
 * Parses the persisted alert history defensively.
 * A malformed root invalidates the whole payload, but a malformed individual
 * record is isolated so valid alerts remain available to the app.
 */
internal fun parseAlertsJson(raw: String): List<SmartAlert> = runCatching {
    val array = JSONArray(raw)
    buildList {
        for (i in 0 until array.length()) {
            runCatching {
                val o = array.getJSONObject(i)
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
            }.getOrNull()?.let(::add)
        }
    }
}.getOrDefault(emptyList())
