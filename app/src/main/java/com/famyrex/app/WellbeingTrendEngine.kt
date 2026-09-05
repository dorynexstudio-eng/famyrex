package com.famyrex.app

import java.text.DecimalFormat

/**
 * Analiza tendencias de bienestar usando únicamente métricas agregadas de uso.
 * No interpreta contenido privado ni pretende diagnosticar problemas.
 */
object WellbeingTrendEngine {
    private val decimal = DecimalFormat("0.0")

    data class Assessment(
        val score: Int,
        val title: String,
        val summary: String,
        val recommendation: String,
        val sustainedDays: Int,
        val averageMinutes: Int,
        val latestMinutes: Int
    )

    fun evaluate(history: List<DailyUsage>): Assessment? {
        if (history.size < 7) return null

        val ordered = history.sortedBy { it.date }
        val baseline = ordered.dropLast(3).takeLast(4)
        val recent = ordered.takeLast(3)
        if (baseline.size < 4 || recent.size < 3) return null

        val baselineAvg = baseline.map { minutes(it) }.average()
        val recentAvg = recent.map { minutes(it) }.average()
        if (baselineAvg < 30.0) return null

        val elevated = recent.count { minutes(it) >= baselineAvg * 1.25 }
        val veryElevated = recent.count { minutes(it) >= baselineAvg * 1.50 }
        val latest = minutes(ordered.last()).toInt()
        val ratio = recentAvg / baselineAvg

        var score = 0
        if (elevated >= 2) score += 35
        if (elevated == 3) score += 15
        if (veryElevated >= 2) score += 20
        if (ratio >= 1.50) score += 15
        score = score.coerceIn(0, 100)

        if (score < 35) return Assessment(
            score = score,
            title = "Patrón estable",
            summary = "El uso reciente se mantiene cerca de su referencia.",
            recommendation = "Mantén las rutinas acordadas y revisa la tendencia semanal.",
            sustainedDays = elevated,
            averageMinutes = recentAvg.toInt(),
            latestMinutes = latest
        )

        val recommendation = when {
            ratio >= 1.50 -> "Conviene revisar juntos las rutinas de descanso, estudio y ocio y acordar un ajuste si hace falta."
            elevated >= 2 -> "Observa la tendencia durante los próximos días y valora más descansos o límites acordados."
            else -> "No tomes decisiones por un solo día; revisa la evolución antes de intervenir."
        }

        return Assessment(
            score = score,
            title = "Tendencia de uso elevada",
            summary = "El uso reciente está por encima de la referencia durante varios días (${decimal.format(ratio)}× aproximadamente).",
            recommendation = recommendation,
            sustainedDays = elevated,
            averageMinutes = recentAvg.toInt(),
            latestMinutes = latest
        )
    }

    private fun minutes(day: DailyUsage): Double = day.totalTimeMs / 60_000.0
}
