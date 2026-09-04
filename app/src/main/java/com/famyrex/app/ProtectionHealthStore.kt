package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ProtectionHealthStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_protection_health", Context.MODE_PRIVATE)

    fun save(health: ProtectionHealth) {
        val json = JSONObject()
            .put("active", health.active)
            .put("checkedAtMs", health.checkedAtMs)
            .put("reasons", JSONArray(health.reasons))
        prefs.edit().putString("latest", json.toString()).apply()
    }

    fun load(): ProtectionHealth? {
        val raw = prefs.getString("latest", null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val array = json.optJSONArray("reasons")
            val reasons = buildList {
                if (array != null) for (i in 0 until array.length()) add(array.optString(i))
            }
            ProtectionHealth(
                active = json.optBoolean("active", false),
                reasons = reasons,
                checkedAtMs = json.optLong("checkedAtMs", 0L)
            )
        }.getOrNull()
    }
}
