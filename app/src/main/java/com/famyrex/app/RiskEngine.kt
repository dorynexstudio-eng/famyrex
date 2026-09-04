package com.famyrex.app

object RiskEngine {
    fun evaluate(
        alerts: List<SmartAlert>,
        settings: ProtectionSettings
    ): RiskAssessment {
        if (alerts.isEmpty()) return RiskAssessment(0, RiskLevel.NORMAL, emptyList())

        val multiplier = when (settings.sensitivity.coerceIn(1, 3)) {
            1 -> 0.75
            3 -> 1.25
            else -> 1.0
        }

        val uniqueTypes = alerts.map { it.type }.distinct()
        var raw = 0
        val reasons = mutableListOf<String>()

        alerts.forEach { alert ->
            val points = when (alert.severity) {
                AlertSeverity.INFO -> 5
                AlertSeverity.ATTENTION -> 20
                AlertSeverity.IMPORTANT -> 40
            }
            raw += points
            reasons += alert.title
        }

        // Different signal families in the same period indicate broader change.
        if (uniqueTypes.size >= 3) raw += 15
        if (alerts.count { it.type == AlertType.PATTERN_CHANGE } >= 2) raw += 10

        val score = (raw * multiplier).toInt().coerceIn(0, 100)
        val level = when {
            score >= 70 -> RiskLevel.IMPORTANT
            score >= 40 -> RiskLevel.ELEVATED
            score >= 20 -> RiskLevel.ATTENTION
            else -> RiskLevel.NORMAL
        }

        return RiskAssessment(score, level, reasons.distinct().take(4))
    }
}
