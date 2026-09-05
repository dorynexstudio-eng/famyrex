package com.famyrex.app

/** Destino de la acción sugerida, sin ejecutar cambios automáticamente. */
enum class FamilyIntelligenceRecommendationDestination {
    ALERTS,
    PARENTAL_CONTROL,
    OBSERVE
}

/** Recomendaciones deterministas basadas solo en señales observadas por Famyrex. */
data class FamilyIntelligenceRecommendation(
    val title: String,
    val action: String,
    val destination: FamilyIntelligenceRecommendationDestination
)

object FamilyIntelligenceRecommendationEngine {
    fun recommend(
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
