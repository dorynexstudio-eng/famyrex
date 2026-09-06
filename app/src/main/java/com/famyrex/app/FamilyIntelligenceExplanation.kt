package com.famyrex.app

/** Fachada de explicación: la lógica y prioridad viven en la cadena de evidencias. */
object FamilyIntelligenceExplanation {
    fun explain(
        summary: FamilyIntelligenceSummary,
        trend: FamilyUsageTrend?,
        incidents: List<CommunicationRiskIncident> = emptyList()
    ): String = FamilyIntelligenceEvidenceBuilder.explain(summary, trend, incidents)
}
