package com.famyrex.app

/** Evidence item used to make a Family Intelligence conclusion auditable and understandable. */
data class FamilyIntelligenceEvidence(
    val type: FamilyIntelligenceEvidenceType,
    val signal: String,
    val conclusion: String,
    val action: String
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

    fun build(summary: FamilyIntelligenceSummary, trend: FamilyUsageTrend?): List<FamilyIntelligenceEvidence> =
        fromSummary(summary) + fromTrend(trend)
}
