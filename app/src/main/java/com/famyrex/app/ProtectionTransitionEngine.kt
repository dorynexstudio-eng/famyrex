package com.famyrex.app

enum class ProtectionTransition { DEGRADED, RESTORED }

data class ProtectionTransitionEvent(
    val component: ProtectionComponent,
    val transition: ProtectionTransition,
    val sinceMs: Long
)

/**
 * Compara estados de protección y emite únicamente transiciones reales.
 * El primer estado conocido se considera una línea base y no genera alerta.
 */
object ProtectionTransitionEngine {
    fun evaluate(
        previous: List<ProtectionComponent>?,
        current: List<ProtectionComponent>,
        nowMs: Long
    ): List<ProtectionTransitionEvent> {
        if (previous == null) return emptyList()
        val oldByKey = previous.associateBy { it.key }
        return current.mapNotNull { component ->
            when (oldByKey[component.key]?.status to component.status) {
                ProtectionComponentStatus.ACTIVE to ProtectionComponentStatus.DEGRADED,
                ProtectionComponentStatus.ACTIVE to ProtectionComponentStatus.NOT_CONFIGURED ->
                    ProtectionTransitionEvent(component, ProtectionTransition.DEGRADED, nowMs)
                ProtectionComponentStatus.DEGRADED to ProtectionComponentStatus.ACTIVE ->
                    ProtectionTransitionEvent(component, ProtectionTransition.RESTORED, nowMs)
                else -> null
            }
        }
    }
}
