package com.famyrex.app

/**
 * Decide si una nueva detección pertenece al mismo episodio de riesgo.
 * Mantiene la regla fuera del servicio Android para poder probarla con JUnit.
 */
object CommunicationRiskEpisodeMatcher {
    private const val COOLDOWN_MS = 30 * 60 * 1000L

    fun isSameEpisode(
        summary: CommunicationRiskSummary,
        incident: CommunicationRiskIncident,
        sourcePackage: String,
        nowMs: Long
    ): Boolean {
        if (incident.sourcePackage != sourcePackage) return false
        if (incident.status.isTerminal()) return false

        val ageMs = nowMs - incident.createdAtMs
        if (ageMs !in 0..COOLDOWN_MS) return false

        val directions = summary.signals.map { it.direction }.toSet()
        if (incident.direction !in directions) return false

        return summary.signals.any { it.type == incident.type }
    }

    private fun RiskIncidentStatus.isTerminal(): Boolean = when (this) {
        RiskIncidentStatus.DISMISSED,
        RiskIncidentStatus.AUTO_DISMISSED,
        RiskIncidentStatus.RESOLVED -> true
        RiskIncidentStatus.DETECTED,
        RiskIncidentStatus.REVIEWED,
        RiskIncidentStatus.CONFIRMED -> false
    }
}
