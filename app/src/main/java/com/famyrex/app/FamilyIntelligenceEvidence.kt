package com.famyrex.app

/** Evidence item used to make a Family Intelligence conclusion auditable and understandable. */
data class FamilyIntelligenceEvidence(
    val type: FamilyIntelligenceEvidenceType,
    val signal: String,
    val conclusion: String,
    val action: String,
    val referenceId: String? = null,
    val incidentStatus: RiskIncidentStatus? = null
)

enum class FamilyIntelligenceEvidenceType {
    STATUS,
    COMMUNICATION,
    TREND,
    ANOMALY,
    DATA_GAP
}

/** Builds deterministic evidence from already observed signals. */
object FamilyIntelligenceEvidenceBuilder {
    fun fromSummary(summary: FamilyIntelligenceSummary): List<FamilyIntelligenceEvidence> = buildList {
        when (summary.parentalStatus) {
            ParentalStatus.RED -> add(
                FamilyIntelligenceEvidence(
                    FamilyIntelligenceEvidenceType.STATUS,
                    "Límite configurado alcanzado o superado.",
                    "Estado 🔴 Acción necesaria.",
                    "Revisar el límite y el uso de hoy."
                )
            )
            ParentalStatus.ORANGE -> add(
                FamilyIntelligenceEvidence(
                    FamilyIntelligenceEvidenceType.STATUS,
                    "Uso cercano a un límite configurado.",
                    "Estado 🟠 Revisar.",
                    "Revisar cómo va el día."
                )
            )
            ParentalStatus.GREEN -> add(
                FamilyIntelligenceEvidence(
                    FamilyIntelligenceEvidenceType.STATUS,
                    "Los datos disponibles no muestran un límite alcanzado.",
                    "Estado 🟢 En orden.",
                    "Continuar observando."
                )
            )
            ParentalStatus.WHITE -> add(
                FamilyIntelligenceEvidence(
                    FamilyIntelligenceEvidenceType.DATA_GAP,
                    "Faltan datos o permisos suficientes.",
                    "Estado ⚪ Sin datos suficientes.",
                    "Completar los permisos o datos necesarios."
                )
            )
        }

        if (summary.communicationAlertCount > 0) {
            add(
                FamilyIntelligenceEvidence(
                    FamilyIntelligenceEvidenceType.COMMUNICATION,
                    "Hay ${summary.communicationAlertCount} señal${if (summary.communicationAlertCount == 1) "" else "es"} de comunicación pendiente${if (summary.communicationAlertCount == 1) "" else "s"} de revisión.",
                    "La comunicación requiere revisión, sin atribuir intención.",
                    "Revisar la señal y su contexto."
                )
            )
        }

        if (!summary.protectionReady) {
            add(
                FamilyIntelligenceEvidence(
                    FamilyIntelligenceEvidenceType.DATA_GAP,
                    "La protección no está completamente preparada.",
                    "La valoración puede ser incompleta.",
                    "Revisar acceso de uso y guardia parental."
                )
            )
        }
    }

    /** Converts the current incident records into auditable evidence without copying their lifecycle elsewhere. */
    fun fromIncidents(incidents: List<CommunicationRiskIncident>): List<FamilyIntelligenceEvidence> =
        incidents
            .asSequence()
            .filter { it.status !in setOf(RiskIncidentStatus.DISMISSED, RiskIncidentStatus.AUTO_DISMISSED, RiskIncidentStatus.RESOLVED) }
            .sortedWith(compareByDescending<CommunicationRiskIncident> { it.createdAtMs }.thenBy { it.id })
            .map { incident ->
                val direction = when (incident.direction) {
                    CommunicationDirection.INCOMING -> "entrante"
                    CommunicationDirection.OUTGOING -> "saliente"
                    CommunicationDirection.UNKNOWN -> "sin dirección determinada"
                }
                val reason = incident.reasons.firstOrNull()?.title ?: "Señal de comunicación detectada"
                FamilyIntelligenceEvidence(
                    type = FamilyIntelligenceEvidenceType.COMMUNICATION,
                    signal = "$reason · comunicación $direction",
                    conclusion = "Señal pendiente de valoración; estado: ${incident.status.label()}.",
                    action = "Revisar el contexto y la evidencia asociada, sin asumir intención.",
                    referenceId = incident.id,
                    incidentStatus = incident.status
                )
            }
            .toList()

    fun fromTrend(trend: FamilyUsageTrend?): List<FamilyIntelligenceEvidence> {
        if (trend == null) return emptyList()
        val evidence = mutableListOf<FamilyIntelligenceEvidence>()

        when (trend.direction) {
            FamilyUsageTrendDirection.INCREASING -> evidence += FamilyIntelligenceEvidence(
                FamilyIntelligenceEvidenceType.TREND,
                "El uso de hoy está por encima de la referencia reciente.",
                "Hay una tendencia creciente de uso.",
                "Observar la evolución; la variación no demuestra una causa."
            )
            FamilyUsageTrendDirection.DECREASING -> evidence += FamilyIntelligenceEvidence(
                FamilyIntelligenceEvidenceType.TREND,
                "El uso de hoy está por debajo de la referencia reciente.",
                "Hay una tendencia decreciente de uso.",
                "Continuar observando la evolución."
            )
            FamilyUsageTrendDirection.STABLE,
            FamilyUsageTrendDirection.INSUFFICIENT_DATA -> Unit
        }

        trend.anomaly?.let { anomaly ->
            val direction = if (anomaly.type == FamilyUsageAnomalyType.HIGH) "por encima" else "por debajo"
            evidence += FamilyIntelligenceEvidence(
                FamilyIntelligenceEvidenceType.ANOMALY,
                "El uso está un ${anomaly.deviationPercent}% $direction de la referencia reciente.",
                "Se observa una variación estadística relevante.",
                "Revisar el contexto; esta variación no demuestra una causa."
            )
        }

        return evidence
    }

    fun build(
        summary: FamilyIntelligenceSummary,
        trend: FamilyUsageTrend?,
        incidents: List<CommunicationRiskIncident> = emptyList()
    ): List<FamilyIntelligenceEvidence> =
        fromSummary(summary) + fromIncidents(incidents) + fromTrend(trend)

    /** Single source for explanation priority and wording; the UI only renders the result. */
    fun explain(
        summary: FamilyIntelligenceSummary,
        trend: FamilyUsageTrend?,
        incidents: List<CommunicationRiskIncident> = emptyList()
    ): String {
        val evidence = build(summary, trend, incidents)
        if (summary.parentalStatus == ParentalStatus.WHITE) {
            return evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.DATA_GAP }?.conclusion
                ?: "No puedo valorar completamente la situación todavía: faltan datos o permisos de uso y protección."
        }
        if (summary.parentalStatus == ParentalStatus.RED) {
            return evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.STATUS }?.let {
                "${it.conclusion} ${it.action}"
            } ?: "El control parental requiere revisión."
        }
        if (incidents.any { it.status !in setOf(RiskIncidentStatus.DISMISSED, RiskIncidentStatus.AUTO_DISMISSED, RiskIncidentStatus.RESOLVED) }) {
            return evidence
                .firstOrNull { it.type == FamilyIntelligenceEvidenceType.COMMUNICATION && it.referenceId != null }
                ?.let { "${it.signal}. ${it.conclusion} ${it.action}" }
                ?: "Hay señales de comunicación pendientes de revisión."
        }
        if (summary.communicationAlertCount > 0) {
            return evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.COMMUNICATION }?.signal
                ?: "Hay señales de comunicación pendientes de revisión."
        }
        evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.ANOMALY }?.let {
            return it.signal + " Es una variación que conviene observar."
        }
        evidence.firstOrNull { it.type == FamilyIntelligenceEvidenceType.TREND }?.let {
            return when (summary.parentalStatus) {
                ParentalStatus.GREEN -> "La protección está en orden. ${it.conclusion} ${it.action}"
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

private fun RiskIncidentStatus.label(): String = when (this) {
    RiskIncidentStatus.DETECTED -> "Detectado"
    RiskIncidentStatus.REVIEWED -> "Revisado"
    RiskIncidentStatus.CONFIRMED -> "Confirmado"
    RiskIncidentStatus.DISMISSED -> "Descartado"
    RiskIncidentStatus.AUTO_DISMISSED -> "Descartado automáticamente"
    RiskIncidentStatus.RESOLVED -> "Resuelto"
}
