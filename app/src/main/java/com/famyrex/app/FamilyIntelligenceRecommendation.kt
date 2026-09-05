package com.famyrex.app

/** Destino de la acción sugerida, sin ejecutar cambios automáticamente. */
enum class FamilyIntelligenceRecommendationDestination {
    ALERTS,
    PARENTAL_CONTROL,
    OBSERVE
}

/** Recomendación adaptada a la navegación del Centro de inteligencia. */
data class FamilyIntelligenceRecommendation(
    val title: String,
    val action: String,
    val destination: FamilyIntelligenceRecommendationDestination
)

object FamilyIntelligenceRecommendationEngine {
    /**
     * Adaptador de compatibilidad para las llamadas existentes del Centro de inteligencia.
     * La lógica nueva se alimenta de las alertas cuando están disponibles.
     */
    fun recommend(
        summary: FamilyIntelligenceSummary,
        trend: FamilyUsageTrend?
    ): FamilyIntelligenceRecommendation =
        legacyRecommend(summary, trend)

    /**
     * Conecta el Centro de inteligencia con RecommendationEngine, que es la fuente única
     * de recomendaciones derivadas de alertas locales y ya priorizadas por importancia.
     */
    fun recommend(
        summary: FamilyIntelligenceSummary,
        trend: FamilyUsageTrend?,
        alerts: List<SmartAlert>
    ): FamilyIntelligenceRecommendation {
        val recommendation = RecommendationEngine.evaluate(alerts).firstOrNull()
        if (recommendation != null) {
            return FamilyIntelligenceRecommendation(
                title = recommendation.title,
                action = recommendation.message,
                destination = destinationFor(recommendation, alerts)
            )
        }
        return legacyRecommend(summary, trend)
    }

    private fun destinationFor(
        recommendation: FamilyRecommendation,
        alerts: List<SmartAlert>
    ): FamilyIntelligenceRecommendationDestination {
        val alert = recommendation.alertId?.let { id -> alerts.firstOrNull { it.id == id } }
        return when (alert?.type) {
            AlertType.NIGHT_USE,
            AlertType.DAILY_LIMIT,
            AlertType.EVASION_SIGNAL,
            AlertType.PROTECTION_DEGRADED -> FamilyIntelligenceRecommendationDestination.PARENTAL_CONTROL
            AlertType.COMMUNICATION_RISK,
            AlertType.APP_SPIKE,
            AlertType.PATTERN_CHANGE,
            AlertType.PROTECTION_RESTORED,
            AlertType.GEOFENCE_ENTER,
            AlertType.GEOFENCE_EXIT,
            AlertType.APP_INSTALLED,
            AlertType.APP_UNINSTALLED -> FamilyIntelligenceRecommendationDestination.ALERTS
            null -> when (recommendation.action) {
                RecommendationAction.OBSERVE -> FamilyIntelligenceRecommendationDestination.OBSERVE
                else -> FamilyIntelligenceRecommendationDestination.ALERTS
            }
        }
    }

    private fun legacyRecommend(
        summary: FamilyIntelligenceSummary,
        trend: FamilyUsageTrend?
    ): FamilyIntelligenceRecommendation {
        if (summary.communicationAlertCount > 0) {
            return FamilyIntelligenceRecommendation(
                title = "Revisar comunicaciones",
                action = "Consulta las alertas pendientes y, si procede, habla con calma sobre lo ocurrido antes de tomar medidas.",
                destination = FamilyIntelligenceRecommendationDestination.ALERTS
            )
        }

        return when (summary.parentalStatus) {
            ParentalStatus.WHITE -> FamilyIntelligenceRecommendation(
                title = "Completar protección",
                action = "Concede el acceso a los datos de uso y activa la guardia parental para que Famyrex pueda valorar la protección.",
                destination = FamilyIntelligenceRecommendationDestination.PARENTAL_CONTROL
            )
            ParentalStatus.RED -> FamilyIntelligenceRecommendation(
                title = "Revisar el límite",
                action = "Comprueba el límite o las restricciones configuradas y decide con la familia si siguen encajando con vuestra rutina.",
                destination = FamilyIntelligenceRecommendationDestination.PARENTAL_CONTROL
            )
            ParentalStatus.ORANGE -> FamilyIntelligenceRecommendation(
                title = "Revisar la rutina",
                action = "Comprueba si el uso se acerca al límite configurado y si ese límite sigue siendo adecuado para hoy.",
                destination = FamilyIntelligenceRecommendationDestination.PARENTAL_CONTROL
            )
            ParentalStatus.GREEN -> when (trend?.direction) {
                FamilyUsageTrendDirection.INCREASING -> FamilyIntelligenceRecommendation(
                    title = "Observar la evolución",
                    action = "El uso está aumentando respecto a la referencia reciente. Observa cómo evoluciona antes de cambiar límites.",
                    destination = FamilyIntelligenceRecommendationDestination.OBSERVE
                )
                FamilyUsageTrendDirection.DECREASING -> FamilyIntelligenceRecommendation(
                    title = "Mantener el seguimiento",
                    action = "El uso está por debajo de la referencia reciente. Puedes mantener el seguimiento sin necesidad de cambiar nada ahora.",
                    destination = FamilyIntelligenceRecommendationDestination.OBSERVE
                )
                else -> FamilyIntelligenceRecommendation(
                    title = "Seguir observando",
                    action = "No hay una acción inmediata indicada por los datos disponibles. Puedes seguir observando la evolución familiar.",
                    destination = FamilyIntelligenceRecommendationDestination.OBSERVE
                )
            }
        }
    }
}
