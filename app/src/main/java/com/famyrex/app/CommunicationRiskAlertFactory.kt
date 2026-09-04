package com.famyrex.app

/**
 * Regla de oro de Famyrex: una sospecha nunca se presenta como una acusación.
 * Los textos describen señales detectadas, no afirman que una persona haya
 * cometido un daño ni que el menor esté necesariamente en peligro.
 */
object CommunicationRiskAlertFactory {
    fun createIncidentAlert(incident: CommunicationRiskIncident): SmartAlert {
        val reasonText = incident.reasons
            .take(3)
            .joinToString("; ") { it.title }
            .ifBlank { "varias señales compatibles con un posible riesgo" }

        val message = "Famyrex ha detectado varias señales compatibles con un posible riesgo: $reasonText. " +
            "Confianza ${incident.confidence.name.lowercase()} y puntuación ${incident.score}/100. " +
            "Revisa el contexto antes de sacar conclusiones."

        return SmartAlert(
            id = "communication_risk_${incident.id}",
            type = AlertType.COMMUNICATION_RISK,
            severity = if (incident.score >= 85) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION,
            title = "Posibles señales de riesgo",
            message = message,
            date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(incident.createdAtMs)),
            packageName = incident.sourcePackage,
            lifecycleStatus = when (incident.status) {
                RiskIncidentStatus.DETECTED -> AlertLifecycleStatus.DETECTED
                RiskIncidentStatus.REVIEWED -> AlertLifecycleStatus.REVIEWED
                RiskIncidentStatus.CONFIRMED -> AlertLifecycleStatus.CONFIRMED
                RiskIncidentStatus.DISMISSED -> AlertLifecycleStatus.DISMISSED
                RiskIncidentStatus.AUTO_DISMISSED -> AlertLifecycleStatus.AUTO_DISMISSED
                RiskIncidentStatus.RESOLVED -> AlertLifecycleStatus.RESOLVED
            }
        )
    }
}
