package com.famyrex.app

/**
 * Motor conservador para señales de comunicación.
 * Recibe señales ya extraídas; no almacena ni procesa conversaciones completas.
 */
object CommunicationRiskEngine {
    fun evaluate(signals: List<CommunicationRiskSignal>): CommunicationRiskSummary {
        if (signals.isEmpty()) {
            return CommunicationRiskSummary(0, RiskConfidence.LOW, emptyList())
        }

        val distinctTypes = signals.map { it.type }.distinct().size
        val high = signals.count { it.confidence == RiskConfidence.HIGH }
        val medium = signals.count { it.confidence == RiskConfidence.MEDIUM }

        var score = (high * 35) + (medium * 18) + (signals.size.coerceAtMost(4) * 5)
        if (distinctTypes >= 2) score += 20
        if (distinctTypes >= 3) score += 15
        score = score.coerceAtMost(100)

        val confidence = when {
            high >= 2 || distinctTypes >= 3 -> RiskConfidence.HIGH
            high >= 1 || medium >= 2 -> RiskConfidence.MEDIUM
            else -> RiskConfidence.LOW
        }

        return CommunicationRiskSummary(
            score = score,
            confidence = confidence,
            signals = signals.distinctBy { "${it.type}:${it.reason}" }.take(8)
        )
    }
}
