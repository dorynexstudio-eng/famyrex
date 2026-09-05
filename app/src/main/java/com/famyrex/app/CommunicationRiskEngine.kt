package com.famyrex.app

/**
 * Motor conservador para señales de comunicación.
 * Recibe señales ya extraídas; no almacena ni procesa conversaciones completas.
 *
 * La puntuación representa acumulación de señales, no una acusación.
 * Las señales tempranas pueden elevar la atención sin disparar una alarma.
 */
object CommunicationRiskEngine {
    fun evaluate(signals: List<CommunicationRiskSignal>): CommunicationRiskSummary {
        if (signals.isEmpty()) {
            return CommunicationRiskSummary(0, RiskConfidence.LOW, emptyList())
        }

        val distinct = signals.distinctBy { "${it.type}:${it.reason}:${it.sourcePackage}" }

        val criticalSelfHarm = distinct.any {
            it.type == CommunicationRiskType.SELF_HARM &&
                it.confidence == RiskConfidence.HIGH &&
                AiConversationSafetyEngine.isAiAssistantPackage(it.sourcePackage)
        }
        val criticalSexualRequest = distinct.any {
            it.type == CommunicationRiskType.SEXUAL_REQUEST &&
                it.confidence == RiskConfidence.HIGH
        }

        if (criticalSelfHarm || criticalSexualRequest) {
            return CommunicationRiskSummary(
                score = 100,
                confidence = RiskConfidence.HIGH,
                signals = distinct.take(8)
            )
        }

        val distinctTypes = distinct.map { it.type }.distinct().size
        val high = distinct.count { it.confidence == RiskConfidence.HIGH }
        val medium = distinct.count { it.confidence == RiskConfidence.MEDIUM }
        val low = distinct.count { it.confidence == RiskConfidence.LOW }

        var score = (high * 30) + (medium * 12) + (low * 4)
        if (distinctTypes >= 2) score += 20
        if (distinctTypes >= 3) score += 15
        if (high >= 2) score += 15

        // Escalada temprana: varias señales débiles relacionadas merecen atención
        // aunque todavía no exista una señal crítica.
        if (low >= 2 && distinctTypes >= 2) score += 8
        if (distinct.count { it.type == CommunicationRiskType.GROOMING } >= 2) score += 8
        if (distinct.count { it.type == CommunicationRiskType.BULLYING } >= 2) score += 8
        if (distinct.any { it.type == CommunicationRiskType.SOCIAL_ISOLATION } &&
            distinct.any { it.type == CommunicationRiskType.BULLYING }) score += 12

        score = score.coerceIn(0, 100)

        val confidence = when {
            high >= 2 && distinctTypes >= 2 -> RiskConfidence.HIGH
            high >= 1 && distinctTypes >= 2 -> RiskConfidence.MEDIUM
            medium >= 2 && distinctTypes >= 2 -> RiskConfidence.MEDIUM
            low >= 2 && distinctTypes >= 2 -> RiskConfidence.MEDIUM
            else -> RiskConfidence.LOW
        }

        return CommunicationRiskSummary(
            score = score,
            confidence = confidence,
            signals = distinct.take(8)
        )
    }
}
