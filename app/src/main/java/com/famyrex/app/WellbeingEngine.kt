package com.famyrex.app

import java.util.Calendar

object WellbeingEngine {

    fun evaluate(
        todayMinutes: Long,
        intervals: List<UsageInterval>,
        settings: WellbeingSettings
    ): WellbeingAssessment {
        val goal = settings.dailyGoalMinutes.coerceAtLeast(1L)
        val progress = ((todayMinutes.toDouble() / goal.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)

        val nightMinutes = intervals
            .filter { isInWindow(it.timestampMs, settings.nightStartMinutes, settings.nightEndMinutes) }
            .sumOf { it.totalTimeMs } / 60_000L

        // We use sampling gaps as a conservative proxy for "break opportunities".
        // This is not a claim that the user was continuously active between samples.
        val sorted = intervals.sortedBy { it.timestampMs }
        var breakCount = 0
        for (i in 1 until sorted.size) {
            val gapMinutes = (sorted[i].timestampMs - sorted[i - 1].timestampMs) / 60_000L
            if (gapMinutes >= settings.breakAfterMinutes) breakCount++
        }

        val recommendation = when {
            todayMinutes >= goal * 1.25 ->
                "Hoy el uso va bastante por encima del objetivo. Considera una pausa y revisa qué aplicaciones están concentrando más tiempo."
            nightMinutes >= 30 ->
                "Se ha detectado uso durante el horario nocturno configurado. Si es posible, intenta reservar ese tramo para descanso."
            breakCount == 0 && intervals.size >= 4 ->
                "No se observan pausas largas entre las muestras disponibles. Intenta introducir descansos periódicos."
            todayMinutes >= goal ->
                "Has alcanzado el objetivo diario. Si puedes, mantén el uso restante para actividades realmente necesarias."
            else ->
                "El uso de hoy está dentro del objetivo configurado. Mantén pausas periódicas y una rutina nocturna estable."
        }

        return WellbeingAssessment(
            todayMinutes = todayMinutes,
            goalMinutes = goal,
            goalProgress = progress,
            breakCount = breakCount,
            nightMinutes = nightMinutes,
            recommendation = recommendation
        )
    }

    private fun isInWindow(timestampMs: Long, start: Int, end: Int): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return if (start <= end) minutes in start until end
        else minutes >= start || minutes < end
    }
}
