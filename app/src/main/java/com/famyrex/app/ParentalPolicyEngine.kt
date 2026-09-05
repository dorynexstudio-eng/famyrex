package com.famyrex.app

import java.util.Calendar

/** Resultado de evaluar las reglas parentales contra el uso actual. */
data class ParentalRestrictionResult(
    val restricted: Boolean,
    val reasons: List<String>
)

object ParentalPolicyEngine {
    fun evaluate(
        config: ParentalControlConfig,
        packageName: String,
        usedTodayMinutes: Long,
        nowMs: Long = System.currentTimeMillis()
    ): ParentalRestrictionResult {
        val reasons = mutableListOf<String>()
        val now = Calendar.getInstance().apply { timeInMillis = nowMs }
        val minuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        config.screenTimeLimit?.takeIf { it.enabled }?.let { limit ->
            if (usedTodayMinutes >= limit.dailyMinutes) {
                reasons += "Se ha alcanzado el límite diario de pantalla."
            }
        }

        config.appRestrictions.firstOrNull { it.packageName == packageName }?.let { restriction ->
            if (restriction.blocked) reasons += "La aplicación está bloqueada por la configuración familiar."
            if (restriction.dailyMinutes != null && usedTodayMinutes >= restriction.dailyMinutes) {
                reasons += "Se ha alcanzado el límite diario configurado para esta aplicación."
            }
        }

        if (config.pauseSchedules.any { it.enabled && isInsideSchedule(it.startMinuteOfDay, it.endMinuteOfDay, minuteOfDay) }) {
            reasons += "El dispositivo está dentro de un horario de pausa configurado."
        }

        return ParentalRestrictionResult(restricted = reasons.isNotEmpty(), reasons = reasons.distinct())
    }

    private fun isInsideSchedule(start: Int, end: Int, current: Int): Boolean {
        if (start == end) return true
        return if (start < end) current in start until end else current >= start || current < end
    }
}
