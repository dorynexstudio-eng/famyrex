package com.famyrex.app

/**
 * Decide si una actualización de un episodio merece una nueva notificación.
 * El incidente y la alerta se guardan siempre; aquí solo evitamos ruido al padre.
 */
object CommunicationRiskNotificationPolicy {
    private const val SCORE_ESCALATION = 10

    fun shouldNotify(
        previous: CommunicationRiskIncident?,
        current: CommunicationRiskIncident
    ): Boolean {
        if (previous == null) return true
        if (current.id != previous.id) return true

        if (current.score - previous.score >= SCORE_ESCALATION) return true
        if (confidenceWeight(current.confidence) > confidenceWeight(previous.confidence)) return true

        val previousSeverity = severityFor(previous.score)
        val currentSeverity = severityFor(current.score)
        if (severityWeight(currentSeverity) > severityWeight(previousSeverity)) return true

        // El tipo principal puede escalar aunque el score ya esté saturado en 100.
        if (current.type != previous.type) return true

        val newReasons = current.reasons
            .map { it.code }
            .toSet() - previous.reasons.map { it.code }.toSet()
        if (newReasons.any(::isCriticalReason)) return true
        if (newReasons.isNotEmpty() && current.score > previous.score) return true

        return false
    }

    private fun isCriticalReason(code: String): Boolean = when (code) {
        "SELF_HARM_SIGNAL",
        "SEXUAL_REQUEST",
        "THREAT_SIGNAL" -> true
        else -> false
    }

    private fun confidenceWeight(confidence: RiskConfidence): Int = when (confidence) {
        RiskConfidence.LOW -> 1
        RiskConfidence.MEDIUM -> 2
        RiskConfidence.HIGH -> 3
    }

    private fun severityFor(score: Int): AlertSeverity =
        if (score >= 85) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION

    private fun severityWeight(severity: AlertSeverity): Int = when (severity) {
        AlertSeverity.INFO -> 1
        AlertSeverity.ATTENTION -> 2
        AlertSeverity.IMPORTANT -> 3
    }
}
