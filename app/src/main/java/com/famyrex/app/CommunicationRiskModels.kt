package com.famyrex.app

/** Señal agregada de posible riesgo. Nunca contiene el chat completo. */
enum class CommunicationRiskType {
    GROOMING,
    BULLYING,
    THREAT,
    SEXUAL_REQUEST,
    SECRET_KEEPING,
    SELF_HARM,
    SOCIAL_ISOLATION,
    SOCIAL_CONFLICT
}

enum class RiskConfidence { LOW, MEDIUM, HIGH }

enum class CommunicationDirection { INCOMING, OUTGOING, UNKNOWN }

data class CommunicationRiskSignal(
    val type: CommunicationRiskType,
    val confidence: RiskConfidence,
    val reason: String,
    val sourcePackage: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
    val direction: CommunicationDirection = CommunicationDirection.UNKNOWN
)

data class CommunicationRiskSummary(
    val score: Int,
    val confidence: RiskConfidence,
    val signals: List<CommunicationRiskSignal>
) {
    val shouldAlert: Boolean
        get() = score >= 70 && signals.isNotEmpty()
}
