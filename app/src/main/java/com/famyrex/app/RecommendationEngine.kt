package com.famyrex.app

/**
 * Deterministic, on-device family guidance derived from existing alerts.
 * It does not diagnose, assign intent, or decide who is at fault.
 */
object RecommendationEngine {

    fun evaluate(alerts: List<SmartAlert>): List<FamilyRecommendation> =
        alerts
            .asSequence()
            .filter { it.lifecycleStatus != AlertLifecycleStatus.DISMISSED && it.lifecycleStatus != AlertLifecycleStatus.AUTO_DISMISSED }
            .mapNotNull(::fromAlert)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<FamilyRecommendation> { priorityRank(it.priority) }.thenBy { it.id })
            .take(8)
            .toList()

    private fun fromAlert(alert: SmartAlert): FamilyRecommendation? {
        val (title, message, priority, action) = when (alert.type) {
            AlertType.NIGHT_USE -> Triple(
                "Hablad sobre el horario de uso",
                "Revisad juntos si el horario nocturno está siendo útil y si conviene ajustar rutinas o límites.",
                RecommendationPriority.MEDIUM to RecommendationAction.TALK
            )
            AlertType.DAILY_LIMIT -> Triple(
                "Revisad el uso diario",
                "Comprobad el contexto del aumento de uso antes de cambiar límites o tomar decisiones.",
                if (alert.severity == AlertSeverity.IMPORTANT) RecommendationPriority.HIGH else RecommendationPriority.MEDIUM to RecommendationAction.REVIEW_CONTEXT
            )
            AlertType.APP_SPIKE -> Triple(
                "Comprobad el cambio de uso",
                "Un aumento aislado no explica por sí solo su causa. Revisad qué ocurrió y preguntad con calma.",
                RecommendationPriority.MEDIUM to RecommendationAction.TALK
            )
            AlertType.PATTERN_CHANGE -> Triple(
                "Observad el cambio de patrón",
                "Comparad varios días y el contexto familiar antes de sacar conclusiones sobre el motivo del cambio.",
                RecommendationPriority.MEDIUM to RecommendationAction.REVIEW_CONTEXT
            )
            AlertType.COMMUNICATION_RISK -> Triple(
                "Revisad la situación con apoyo",
                "Mirad el contexto de la comunicación y hablad con las personas implicadas sin asumir de antemano quién tiene la razón.",
                if (alert.severity == AlertSeverity.IMPORTANT) RecommendationPriority.HIGH else RecommendationPriority.MEDIUM to RecommendationAction.SUPPORT
            )
            AlertType.EVASION_SIGNAL -> Triple(
                "Revisad la protección",
                "Comprobad qué ha ocurrido con la configuración y recuperad una protección adecuada sin asumir el motivo.",
                RecommendationPriority.HIGH to RecommendationAction.REVIEW_CONTEXT
            )
            AlertType.PROTECTION_DEGRADED -> Triple(
                "Restableced la protección",
                "Comprobad la configuración y los permisos necesarios para que la protección vuelva a funcionar correctamente.",
                RecommendationPriority.HIGH to RecommendationAction.REVIEW_CONTEXT
            )
            AlertType.PROTECTION_RESTORED -> Triple(
                "Confirmad que todo funciona",
                "Verificad que la protección se mantiene activa y que la configuración sigue siendo la acordada.",
                RecommendationPriority.LOW to RecommendationAction.OBSERVE
            )
            AlertType.GEOFENCE_ENTER, AlertType.GEOFENCE_EXIT -> Triple(
                "Comprobad el contexto de ubicación",
                "Contrastad el aviso con la rutina o el acuerdo familiar antes de interpretarlo como un problema.",
                RecommendationPriority.LOW to RecommendationAction.REVIEW_CONTEXT
            )
            AlertType.APP_INSTALLED, AlertType.APP_UNINSTALLED -> Triple(
                "Revisad el cambio de aplicaciones",
                "Comprobad si el cambio era esperado y, si hace falta, hablad sobre su uso y configuración.",
                RecommendationPriority.LOW to RecommendationAction.TALK
            )
        }.let { Triple(it.first, it.second, it.third) to it.third }

        val priorityAction = when (alert.type) {
            AlertType.DAILY_LIMIT -> if (alert.severity == AlertSeverity.IMPORTANT) RecommendationPriority.HIGH to RecommendationAction.REVIEW_CONTEXT else RecommendationPriority.MEDIUM to RecommendationAction.REVIEW_CONTEXT
            AlertType.COMMUNICATION_RISK -> if (alert.severity == AlertSeverity.IMPORTANT) RecommendationPriority.HIGH to RecommendationAction.SUPPORT else RecommendationPriority.MEDIUM to RecommendationAction.SUPPORT
            else -> priority to action
        }

        return FamilyRecommendation(
            id = "recommendation_${alert.id}",
            title = title,
            message = message,
            priority = priorityAction.first,
            action = priorityAction.second,
            alertId = alert.id
        )
    }

    private fun priorityRank(priority: RecommendationPriority): Int = when (priority) {
        RecommendationPriority.HIGH -> 3
        RecommendationPriority.MEDIUM -> 2
        RecommendationPriority.LOW -> 1
    }
}
