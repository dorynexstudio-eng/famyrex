package com.famyrex.app

/** Resumen local y conservador del estado digital de la familia. */
data class FamilyIntelligenceSummary(
    val parentalStatus: ParentalStatus,
    val totalScreenMinutes: Long?,
    val communicationAlertCount: Int,
    val protectionReady: Boolean,
    val reasons: List<String>,
    val evidence: List<FamilyIntelligenceEvidence> = emptyList()
) {
    /** Requiere atención si hay un riesgo observado o si la protección aún no puede evaluarse por completo. */
    val actionRequired: Boolean
        get() = parentalStatus == ParentalStatus.RED ||
            communicationAlertCount > 0 ||
            !protectionReady
}

/**
 * Agrega señales ya observadas por Famyrex sin inventar datos ausentes.
 * La inteligencia familiar no sustituye a las fuentes: las resume.
 */
object FamilyIntelligenceAggregator {
    fun summarize(
        parentalStatus: ParentalStatus,
        totalScreenMinutes: Long?,
        communicationAlertCount: Int,
        usageAccess: Boolean,
        accessibilityEnabled: Boolean
    ): FamilyIntelligenceSummary {
        val reasons = buildList {
            when (parentalStatus) {
                ParentalStatus.RED -> add("El control parental requiere revisión.")
                ParentalStatus.ORANGE -> add("El uso se acerca a un límite configurado.")
                ParentalStatus.WHITE -> add("Faltan datos o permisos para valorar completamente la protección.")
                ParentalStatus.GREEN -> Unit
            }
            if (communicationAlertCount > 0) {
                add("Hay $communicationAlertCount alerta${if (communicationAlertCount == 1) "" else "s"} de comunicación para revisar.")
            }
            if (!usageAccess) add("No hay acceso a los datos de uso.")
            if (!accessibilityEnabled) add("La guardia parental no está activa.")
        }

        val protectionReady = usageAccess && accessibilityEnabled
        val summary = FamilyIntelligenceSummary(
            parentalStatus = parentalStatus,
            totalScreenMinutes = totalScreenMinutes,
            communicationAlertCount = communicationAlertCount,
            protectionReady = protectionReady,
            reasons = reasons
        )
        return summary.copy(evidence = FamilyIntelligenceEvidenceBuilder.fromSummary(summary))
    }
}
