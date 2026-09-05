package com.famyrex.app

enum class ProtectionTransition { DEGRADED, RESTORED }

data class ProtectionTransitionEvent(
    val component: ProtectionComponent,
    val transition: ProtectionTransition,
    val sinceMs: Long
)

/**
 * Compares protection snapshots. The first snapshot establishes a baseline and emits no alert.
 * Repeated checks with the same state are silent, so a persistent problem cannot spam the parent.
 */
object ProtectionTransitionEngine {
    fun evaluate(
        previous: List<ProtectionComponent>?,
        current: List<ProtectionComponent>,
        nowMs: Long
    ): List<ProtectionTransitionEvent> {
        if (previous == null) return emptyList()
        val before = previous.associateBy { it.key }
        return current.mapNotNull { component ->
            val old = before[component.key]?.status ?: return@mapNotNull null
            when {
                old == ProtectionComponentStatus.ACTIVE && component.status != ProtectionComponentStatus.ACTIVE ->
                    ProtectionTransitionEvent(component, ProtectionTransition.DEGRADED, nowMs)
                old != ProtectionComponentStatus.ACTIVE && component.status == ProtectionComponentStatus.ACTIVE ->
                    ProtectionTransitionEvent(component, ProtectionTransition.RESTORED, nowMs)
                else -> null
            }
        }
    }
}

/** Persists the last component snapshot and the start time of each degraded component. */
class ProtectionIncidentStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("famyrex_protection_incidents", android.content.Context.MODE_PRIVATE)

    fun loadComponents(): List<ProtectionComponent>? {
        val raw = prefs.getString("components", null) ?: return null
        return raw.split("||").filter { it.isNotBlank() }.mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val status = runCatching { ProtectionComponentStatus.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            ProtectionComponent(parts[0], parts[0], status, "")
        }
    }

    fun saveComponents(components: List<ProtectionComponent>) {
        prefs.edit().putString(
            "components",
            components.joinToString("||") { "${it.key}|${it.status.name}" }
        ).apply()
    }

    fun degradedSince(key: String): Long? = prefs.getLong("degraded_since_$key", 0L).takeIf { it > 0L }

    fun markDegraded(key: String, sinceMs: Long) {
        if (degradedSince(key) == null) prefs.edit().putLong("degraded_since_$key", sinceMs).apply()
    }

    fun clearDegraded(key: String) {
        prefs.edit().remove("degraded_since_$key").apply()
    }
}
