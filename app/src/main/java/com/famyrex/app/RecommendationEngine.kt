package com.famyrex.app

/**
 * Deterministic, on-device family guidance derived from existing alerts.
 * It does not diagnose, assign intent, or decide who is at fault.
 */
object RecommendationEngine {

    fun evaluate(alerts: List<SmartAlert>): List<FamilyRecommendation> =
        alerts.asSequence()
            .filter { it.lifecycleStatus != AlertLifecycleStatus.DISMISSED && it.lifecycleStatus != AlertLifecycleStatus.AUTO_DISMISSED }
            .map(::fromAlert)
            .distinctBy { it.id }
            .sortedWith(compareByDescending<FamilyRecommendation> { priorityRank(it.priority) }.thenBy { it.id })
            .take(8)
            .toList()

    private fun fromAlert(alert: SmartAlert): FamilyRecommendation {
        val draft = when (alert.type) {
            AlertType.NIGHT_USE -> Draft("Hablad sobre el horario de uso", "Revisad juntos si el horario nocturno está siendo útil y si conviene ajustar rutinas o límites.", RecommendationPriority.MEDIUM, RecommendationAction.TALK)
            AlertType.DAILY_LIMIT -> Draft("Revisad el uso diario", "Comprobad el contexto del aumento de uso antes de cambiar límites o tomar decisiones.", if (alert.severity == AlertSeverity.IMPORTANT) RecommendationPriority.HIGH else RecommendationPriority.MEDIUM, RecommendationAction.REVIEW_CONTEXT)
            AlertType.APP_SPIKE -> Draft("Comprobad el cambio de uso", "Un aumento aislado no explica por sí solo su causa. Revisad qué ocurrió y preguntad con calma.", RecommendationPriority.MEDIUM, RecommendationAction.TALK)
            AlertType.PATTERN_CHANGE -> Draft("Observad el cambio de patrón", "Comparad varios días y el contexto familiar antes de sacar conclusiones sobre el motivo del cambio.", RecommendationPriority.MEDIUM, RecommendationAction.REVIEW_CONTEXT)
            AlertType.COMMUNICATION_RISK -> Draft("Revisad la situación con apoyo", "Mirad el contexto de la comunicación y hablad con las personas implicadas sin asumir de antemano quién tiene la razón.", if (alert.severity == AlertSeverity.IMPORTANT) RecommendationPriority.HIGH else RecommendationPriority.MEDIUM, RecommendationAction.SUPPORT)
            AlertType.EVASION_SIGNAL -> Draft("Revisad la protección", "Comprobad qué ha ocurrido con la configuración y recuperad una protección adecuada sin asumir el motivo.", RecommendationPriority.HIGH, RecommendationAction.REVIEW_CONTEXT)
            AlertType.PROTECTION_DEGRADED -> Draft("Restableced la protección", "Comprobad la configuración y los permisos necesarios para que la protección vuelva a funcionar correctamente.", RecommendationPriority.HIGH, RecommendationAction.REVIEW_CONTEXT)
            AlertType.PROTECTION_RESTORED -> Draft("Confirmad que todo funciona", "Verificad que la protección se mantiene activa y que la configuración sigue siendo la acordada.", RecommendationPriority.LOW, RecommendationAction.OBSERVE)
            AlertType.GEOFENCE_ENTER, AlertType.GEOFENCE_EXIT -> Draft("Comprobad el contexto de ubicación", "Contrastad el aviso con la rutina o el acuerdo familiar antes de interpretarlo como un problema.", RecommendationPriority.LOW, RecommendationAction.REVIEW_CONTEXT)
            AlertType.APP_INSTALLED, AlertType.APP_UNINSTALLED -> Draft("Revisad el cambio de aplicaciones", "Comprobad si el cambio era esperado y, si hace falta, hablad sobre su uso y configuración.", RecommendationPriority.LOW, RecommendationAction.TALK)
        }

        return FamilyRecommendation(
            id = "recommendation_${alert.id}",
            title = draft.title,
            message = draft.message,
            priority = draft.priority,
            action = draft.action,
            alertId = alert.id
        )
    }

    private data class Draft(
        val title: String,
        val message: String,
        val priority: RecommendationPriority,
        val action: RecommendationAction
    )

    private fun priorityRank(priority: RecommendationPriority): Int = when (priority) {
        RecommendationPriority.HIGH -> 3
        RecommendationPriority.MEDIUM -> 2
        RecommendationPriority.LOW -> 1
    }
}
