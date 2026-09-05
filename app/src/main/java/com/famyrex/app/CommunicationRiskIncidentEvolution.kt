package com.famyrex.app

/**
 * Combina una nueva detección con el incidente que representa el mismo episodio.
 * Dentro de un episodio no se pierden razones ya observadas ni se rebaja una
 * puntuación/confianza que ya había sido alcanzada.
 */
object CommunicationRiskIncidentEvolution {
    fun merge(
        previous: CommunicationRiskIncident,
        current: CommunicationRiskIncident
    ): CommunicationRiskIncident {
        require(previous.id == current.id) { "Cannot merge incidents with different ids" }

        val reasons = (previous.reasons + current.reasons)
            .distinctBy { it.code }

        return current.copy(
            createdAtMs = previous.createdAtMs,
            score = maxOf(previous.score, current.score),
            confidence = maxConfidence(previous.confidence, current.confidence),
            reasons = reasons,
            status = previous.status,
            statusHistory = previous.statusHistory
        )
    }

    private fun maxConfidence(a: RiskConfidence, b: RiskConfidence): RiskConfidence =
        if (confidenceWeight(a) >= confidenceWeight(b)) a else b

    private fun confidenceWeight(confidence: RiskConfidence): Int = when (confidence) {
        RiskConfidence.LOW -> 1
        RiskConfidence.MEDIUM -> 2
        RiskConfidence.HIGH -> 3
    }
}
