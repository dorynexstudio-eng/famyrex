package com.famyrex.app

/** Señal agregada de posible riesgo. Nunca contiene el chat completo. */
enum class CommunicationRiskType {
    GROOMING,
    BULLYING,
    THREAT,
    SEXUAL_REQUEST,
    SECRET_KEEPING,
    SELF_HARM,
    SOCIAL_ISOLATION
}

enum class RiskConfidence { LOW, MEDIUM, HIGH }

data class CommunicationRiskSignal(
    val type: CommunicationRiskType,
    val confidence: RiskConfidence,
    val reason: String,
    val sourcePackage: String? = null,
    val timestampMs: Long = System.currentTimeMillis()
)

data class CommunicationRiskSummary(
    val score: Int,
    val confidence: RiskConfidence,
    val signals: List<CommunicationRiskSignal>
) {
    val shouldAlert: Boolean
        get() = score >= 70 && signals.isNotEmpty()
}
