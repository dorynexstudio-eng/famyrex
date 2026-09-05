package com.famyrex.app

/**
 * Motor conservador para señales de comunicación.
 * Recibe señales ya extraídas; no almacena ni procesa conversaciones completas.
 *
 * Una señal normal nunca debe convertirse por sí misma en una acusación.
 * Excepción de seguridad: una señal explícita de posible autolesión con
 * confianza HIGH se considera prioritaria y puede generar una alerta inmediata.
 */
object CommunicationRiskEngine {
    fun evaluate(signals: List<CommunicationRiskSignal>): CommunicationRiskSummary {
        if (signals.isEmpty()) {
            return CommunicationRiskSummary(0, RiskConfidence.LOW, emptyList())
        }

        val distinct = signals.distinctBy { "${it.type}:${it.reason}" }
        val criticalSelfHarm = distinct.any {
            it.type == CommunicationRiskType.SELF_HARM && it.confidence == RiskConfidence.HIGH
        }

        if (criticalSelfHarm) {
            return CommunicationRiskSummary(
                score = 100,
                confidence = RiskConfidence.HIGH,
                signals = distinct.take(8)
            )
        }

        val distinctTypes = distinct.map { it.type }.distinct().size
        val high = distinct.count { it.confidence == RiskConfidence.HIGH }
        val medium = distinct.count { it.confidence == RiskConfidence.MEDIUM }

        var score = (high * 30) + (medium * 12)
        if (distinctTypes >= 2) score += 20
        if (distinctTypes >= 3) score += 15
        if (high >= 2) score += 15
        score = score.coerceIn(0, 100)

        val confidence = when {
            high >= 2 && distinctTypes >= 2 -> RiskConfidence.HIGH
            high >= 1 && distinctTypes >= 2 -> RiskConfidence.MEDIUM
            medium >= 2 && distinctTypes >= 2 -> RiskConfidence.MEDIUM
            else -> RiskConfidence.LOW
        }

        return CommunicationRiskSummary(
            score = score,
            confidence = confidence,
            signals = distinct.take(8)
        )
    }
}
