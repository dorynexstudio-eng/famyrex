package com.famyrex.app

/**
 * Compara estados de protección y emite únicamente transiciones reales.
 * El primer estado conocido se considera una línea base y no genera alerta.
 */
data class ProtectionTransition(
    val component: ProtectionComponent,
    val kind: Kind,
    val sinceMs: Long
) {
    enum class Kind { DEGRADED, RESTORED }
}

object ProtectionTransitionEngine {
    fun evaluate(
        previous: List<ProtectionComponent>?,
        current: List<ProtectionComponent>,
        nowMs: Long
    ): List<ProtectionTransition> {
        if (previous == null) return emptyList()
        val oldByKey = previous.associateBy { it.key }
        return current.mapNotNull { component ->
            when (oldByKey[component.key]?.status to component.status) {
                ProtectionComponentStatus.ACTIVE to ProtectionComponentStatus.DEGRADED,
                ProtectionComponentStatus.ACTIVE to ProtectionComponentStatus.NOT_CONFIGURED ->
                    ProtectionTransition(component, ProtectionTransition.Kind.DEGRADED, nowMs)
                ProtectionComponentStatus.DEGRADED to ProtectionComponentStatus.ACTIVE ->
                    ProtectionTransition(component, ProtectionTransition.Kind.RESTORED, nowMs)
                else -> null
            }
        }
    }
}
