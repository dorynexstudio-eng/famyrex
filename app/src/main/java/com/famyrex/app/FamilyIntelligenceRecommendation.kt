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

/**
 * Adaptador de presentación para RecommendationEngine.
 * La lógica de recomendación permanece en una única fuente: las alertas locales.
 */
object FamilyIntelligenceRecommendationEngine {
    fun recommend(alerts: List<SmartAlert>): FamilyIntelligenceRecommendation? {
        val recommendation = RecommendationEngine.evaluate(alerts).firstOrNull() ?: return null
        return FamilyIntelligenceRecommendation(
            title = recommendation.title,
            action = recommendation.message,
            destination = destinationFor(recommendation, alerts)
        )
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
            AlertType.GEOFENCE_ENTER,
            AlertType.GEOFENCE_EXIT,
            AlertType.APP_INSTALLED,
            AlertType.APP_UNINSTALLED -> FamilyIntelligenceRecommendationDestination.ALERTS
            AlertType.PROTECTION_RESTORED -> FamilyIntelligenceRecommendationDestination.OBSERVE
            null -> when (recommendation.action) {
                RecommendationAction.OBSERVE -> FamilyIntelligenceRecommendationDestination.OBSERVE
                else -> FamilyIntelligenceRecommendationDestination.ALERTS
            }
        }
    }
}
