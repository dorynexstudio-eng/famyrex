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

        val directionText = when (incident.direction) {
            CommunicationDirection.INCOMING -> "La señal procede de un mensaje recibido en el dispositivo."
            CommunicationDirection.OUTGOING -> "La señal procede de lenguaje enviado desde el dispositivo; esto no determina por sí solo la intención ni la situación."
            CommunicationDirection.UNKNOWN -> "No se ha podido determinar con fiabilidad la dirección del mensaje."
        }

        val (title, prefix, action) = when (incident.type) {
            CommunicationRiskType.SELF_HARM -> Triple(
                "Posible riesgo de autolesión",
                "Famyrex ha detectado una señal explícita compatible con posible autolesión",
                "Habla con el menor cuanto antes y, si existe peligro inmediato, busca ayuda de emergencia."
            )
            CommunicationRiskType.SOCIAL_ISOLATION -> Triple(
                "Posible aislamiento social",
                "Famyrex ha detectado señales compatibles con posible aislamiento social",
                "Habla con el menor con calma y observa si la situación se repite o empeora."
            )
            CommunicationRiskType.SOCIAL_CONFLICT -> Triple(
                "Posible conflicto entre iguales",
                "Famyrex ha detectado señales compatibles con tensión o conflicto entre iguales",
                "Habla con el menor sin tomar partido y comprueba si se trata de un problema puntual o si está derivando en exclusión, humillación o acoso."
            )
            CommunicationRiskType.GROOMING -> Triple(
                "Posible contacto inapropiado",
                "Famyrex ha detectado señales compatibles con posible contacto inapropiado",
                "Habla con el menor sin culpabilizarle y, si procede, ayúdale a bloquear o reportar el contacto."
            )
            CommunicationRiskType.BULLYING -> when (incident.direction) {
                CommunicationDirection.OUTGOING -> Triple(
                    "Lenguaje hostil enviado",
                    "Famyrex ha detectado señales compatibles con lenguaje hostil enviado desde el dispositivo",
                    "Habla con el menor sin etiquetarle y revisa el contexto, la frecuencia y cómo está evolucionando la situación."
                )
                else -> Triple(
                    "Posible acoso",
                    "Famyrex ha detectado señales compatibles con posible acoso",
                    "Habla con el menor y valora si necesita apoyo adicional en casa o en el centro educativo."
                )
            }
            CommunicationRiskType.THREAT -> Triple(
                "Posible amenaza",
                "Famyrex ha detectado señales compatibles con una posible amenaza",
                "Revisa el contexto y, si la amenaza parece concreta o inmediata, busca ayuda adecuada."
            )
            CommunicationRiskType.SEXUAL_REQUEST -> Triple(
                "Posible petición de contenido sexual",
                "Famyrex ha detectado señales compatibles con una posible petición de contenido sexual",
                "Habla con el menor sin culpabilizarle y considera bloquear o reportar el contacto si procede."
            )
            CommunicationRiskType.SECRET_KEEPING -> Triple(
                "Posible petición de secreto",
                "Famyrex ha detectado señales compatibles con una posible petición de mantener un secreto",
                "Habla con el menor y comprueba si alguien le está presionando para ocultar algo."
            )
        }

        val signalCount = incident.reasons.size
        val progression = when {
            incident.type == CommunicationRiskType.SELF_HARM && incident.confidence == RiskConfidence.HIGH ->
                "Intervención prioritaria"
            incident.type == CommunicationRiskType.SEXUAL_REQUEST && incident.confidence == RiskConfidence.HIGH ->
                "Intervención prioritaria"
            signalCount >= 3 && incident.confidence == RiskConfidence.HIGH ->
                "Escalada de riesgo: varias señales relacionadas"
            signalCount >= 2 ->
                "Evolución: se han acumulado varias señales relacionadas"
            else ->
                "Señal temprana: conviene observar su evolución"
        }

        val message = "$prefix. $progression. $directionText " +
            "Señales detectadas: $reasonText. " +
            "Confianza ${incident.confidence.name.lowercase()} y puntuación ${incident.score}/100. " +
            "Recomendación: $action " +
            "Famyrex no guarda ni muestra la conversación completa; revisa el contexto antes de sacar conclusiones."

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
