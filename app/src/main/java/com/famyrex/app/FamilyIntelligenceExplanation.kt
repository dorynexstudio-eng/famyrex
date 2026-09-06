package com.famyrex.app

/** Explicaciones deterministas construidas desde una cadena única de evidencias observadas. */
object FamilyIntelligenceExplanation {
    fun explain(
        summary: FamilyIntelligenceSummary,
        trend: FamilyUsageTrend?
    ): String {
        val evidence = FamilyIntelligenceEvidenceBuilder.build(summary, trend)
        if (summary.parentalStatus == ParentalStatus.WHITE) {
            return evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.DATA_GAP }?.conclusion
                ?: "No puedo valorar completamente la situación todavía: faltan datos o permisos de uso y protección."
        }
        if (summary.communicationAlertCount > 0) {
            return evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.COMMUNICATION }?.signal
                ?: "Hay señales de comunicación pendientes de revisión."
        }
        if (summary.parentalStatus == ParentalStatus.RED) {
            return evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.STATUS }?.let {
                "${it.conclusion} ${it.action}"
            } ?: "El control parental requiere revisión."
        }
        evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.ANOMALY }?.let { return it.signal + " Es una variación que conviene observar." }
        evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.TREND }?.let {
            return when (summary.parentalStatus) {
                ParentalStatus.GREEN -> "${it.conclusion} ${it.action}"
                else -> it.conclusion
            }
        }
        return evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.STATUS }?.let {
            if (summary.parentalStatus == ParentalStatus.ORANGE) {
                "${it.signal} Es un buen momento para revisar cómo va el día."
            } else {
                "${it.conclusion} ${it.action}"
            }
        } ?: "No hay datos suficientes."
    }
}
