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

        val (title, prefix) = when (incident.type) {
            CommunicationRiskType.SELF_HARM ->
                "Posible riesgo de autolesión" to "Famyrex ha detectado una señal explícita compatible con posible autolesión"
            CommunicationRiskType.SOCIAL_ISOLATION ->
                "Posible aislamiento social" to "Famyrex ha detectado señales compatibles con posible aislamiento social"
            CommunicationRiskType.GROOMING ->
                "Posible contacto inapropiado" to "Famyrex ha detectado señales compatibles con posible contacto inapropiado"
            CommunicationRiskType.BULLYING ->
                "Posible acoso" to "Famyrex ha detectado señales compatibles con posible acoso"
            CommunicationRiskType.THREAT ->
                "Posible amenaza" to "Famyrex ha detectado señales compatibles con una posible amenaza"
            CommunicationRiskType.SEXUAL_REQUEST ->
                "Posible petición de contenido sexual" to "Famyrex ha detectado señales compatibles con una posible petición de contenido sexual"
            CommunicationRiskType.SECRET_KEEPING ->
                "Posible petición de secreto" to "Famyrex ha detectado señales compatibles con una posible petición de mantener un secreto"
        }

        val message = "$prefix: $reasonText. " +
            "Confianza ${incident.confidence.name.lowercase()} y puntuación ${incident.score}/100. " +
            "Revisa el contexto antes de sacar conclusiones."

        return SmartAlert(
            id = "communication_risk_${incident.id}",
            type = AlertType.COMMUNICATION_RISK,
            severity = if (incident.score >= 85) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION,
            title = title,
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
