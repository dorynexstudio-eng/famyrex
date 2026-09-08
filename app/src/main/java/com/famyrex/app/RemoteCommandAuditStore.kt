package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Small local audit trail for remote family commands; no command payload secrets are stored. */
class RemoteCommandAuditStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun record(receipt: FamilyControlReceipt) {
        val items = JSONArray(prefs.getString(KEY_ITEMS, "[]"))
        val entry = JSONObject()
            .put("commandId", receipt.commandId)
            .put("action", receipt.action.name)
            .put("acceptedAtMs", receipt.acceptedAtMs)
            .put("completedAtMs", receipt.completedAtMs)
            .put("success", receipt.success)
            .apply { receipt.reason?.let { put("reason", it.take(300)) } }
        items.put(entry)
        val trimmed = JSONArray()
        val start = (items.length() - MAX_ENTRIES).coerceAtLeast(0)
        for (index in start until items.length()) trimmed.put(items.getJSONObject(index))
        prefs.edit().putString(KEY_ITEMS, trimmed.toString()).apply()
    }

    fun recent(limit: Int = 20): List<RemoteCommandAuditEntry> {
        val items = JSONArray(prefs.getString(KEY_ITEMS, "[]"))
        val start = (items.length() - limit.coerceIn(1, MAX_ENTRIES)).coerceAtLeast(0)
        return buildList {
            for (index in start until items.length()) {
                val item = items.getJSONObject(index)
                add(RemoteCommandAuditEntry(
                    commandId = item.optString("commandId"),
                    action = runCatching { FamilyControlAction.valueOf(item.optString("action")) }.getOrNull(),
                    acceptedAtMs = item.optLong("acceptedAtMs"),
                    completedAtMs = item.optLong("completedAtMs"),
                    success = item.optBoolean("success"),
                    reason = item.optString("reason").takeIf { it.isNotBlank() }
                ))
            }
        }
    }

    companion object {
        private const val PREFS = "famyrex_remote_command_audit"
        private const val KEY_ITEMS = "items"
        private const val MAX_ENTRIES = 50
    }
}

data class RemoteCommandAuditEntry(
    val commandId: String,
    val action: FamilyControlAction?,
    val acceptedAtMs: Long,
    val completedAtMs: Long?,
    val success: Boolean,
    val reason: String?
)
