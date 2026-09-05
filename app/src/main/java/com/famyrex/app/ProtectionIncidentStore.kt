package com.famyrex.app

import android.content.Context

/** Persiste solo estado técnico de protección, nunca contenido privado. */
class ProtectionIncidentStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_protection_incidents", Context.MODE_PRIVATE)
    private val componentKeys = "component_keys"
    private val degradedPrefix = "degraded_since_"
    private val statusPrefix = "status_"

    fun loadComponents(): List<ProtectionComponent>? {
        if (!prefs.contains(componentKeys)) return null
        val keys = prefs.getStringSet(componentKeys, emptySet()).orEmpty()
        return keys.mapNotNull { key ->
            val status = prefs.getString(statusPrefix + key, null)?.let { runCatching { ProtectionComponentStatus.valueOf(it) }.getOrNull() }
                ?: return@mapNotNull null
            ProtectionComponent(key, key, status, "Estado anterior registrado por Famyrex.")
        }
    }

    fun saveComponents(components: List<ProtectionComponent>) {
        prefs.edit().apply {
            putStringSet(componentKeys, components.map { it.key }.toSet())
            components.forEach { putString(statusPrefix + it.key, it.status.name) }
        }.apply()
    }

    fun markDegraded(key: String, sinceMs: Long) {
        if (!prefs.contains(degradedPrefix + key)) {
            prefs.edit().putLong(degradedPrefix + key, sinceMs).apply()
        }
    }

    fun degradedSince(key: String): Long? =
        if (prefs.contains(degradedPrefix + key)) prefs.getLong(degradedPrefix + key, 0L) else null

    fun clearDegraded(key: String) {
        prefs.edit().remove(degradedPrefix + key).apply()
    }
}
