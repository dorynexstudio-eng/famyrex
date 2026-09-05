package com.famyrex.app

/** Correlaciona señales técnicas antes de alertar. */
object EvasionRiskEngine {
    data class Assessment(
        val score: Int,
        val confidence: SignalConfidence,
        val shouldAlert: Boolean,
        val title: String,
        val message: String
    )

    fun evaluate(signals: List<EvasionSignal>): Assessment {
        if (signals.isEmpty()) return Assessment(0, SignalConfidence.LOW, false, "Sin señales de evasión", "No se han detectado señales técnicas relevantes.")

        val distinct = signals.distinctBy { it.key }
        val high = distinct.count { it.confidence == SignalConfidence.HIGH }
        val medium = distinct.count { it.confidence == SignalConfidence.MEDIUM }
        val low = distinct.count { it.confidence == SignalConfidence.LOW }

        var score = high * 45 + medium * 18 + low * 6
        if (distinct.size >= 2) score += 15
        if (high >= 1 && medium >= 1) score += 15
        if (medium >= 2) score += 10
        score = score.coerceIn(0, 100)

        val confidence = when {
            high >= 1 && medium >= 1 -> SignalConfidence.HIGH
            high >= 1 || medium >= 2 -> SignalConfidence.MEDIUM
            else -> SignalConfidence.LOW
        }
        val shouldAlert = confidence == SignalConfidence.HIGH && score >= 70
        val names = distinct.take(4).joinToString(", ") { it.title }
        val title = if (shouldAlert) "Posible intento de evasión" else "Señales técnicas detectadas"
        val message = if (shouldAlert) {
            "Famyrex ha correlacionado varias señales técnicas que podrían afectar a la protección: $names. Revisa el dispositivo; esto no constituye una acusación ni confirma evasión."
        } else {
            "Famyrex ha detectado una señal técnica que puede afectar a la protección: $names. Por sí sola no demuestra evasión."
        }
        return Assessment(score, confidence, shouldAlert, title, message)
    }
}
