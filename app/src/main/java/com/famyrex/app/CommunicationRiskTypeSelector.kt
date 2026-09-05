package com.famyrex.app

/**
 * El tipo principal de un episodio representa la señal de mayor prioridad,
 * no simplemente la primera señal que llegó ni la de mayor confianza.
 */
object CommunicationRiskTypeSelector {
    fun select(signals: List<CommunicationRiskSignal>): CommunicationRiskType? =
        signals.maxWithOrNull(
            compareBy<CommunicationRiskSignal> { typePriority(it.type) }
                .thenBy { confidenceWeight(it.confidence) }
                .thenBy { it.timestampMs }
        )?.type

    private fun typePriority(type: CommunicationRiskType): Int = when (type) {
        CommunicationRiskType.SELF_HARM -> 8
        CommunicationRiskType.SEXUAL_REQUEST -> 7
        CommunicationRiskType.THREAT -> 6
        CommunicationRiskType.GROOMING -> 5
        CommunicationRiskType.BULLYING -> 4
        CommunicationRiskType.SECRET_KEEPING -> 3
        CommunicationRiskType.SOCIAL_CONFLICT -> 2
        CommunicationRiskType.SOCIAL_ISOLATION -> 1
    }

    private fun confidenceWeight(confidence: RiskConfidence): Int = when (confidence) {
        RiskConfidence.LOW -> 1
        RiskConfidence.MEDIUM -> 2
        RiskConfidence.HIGH -> 3
    }
}
