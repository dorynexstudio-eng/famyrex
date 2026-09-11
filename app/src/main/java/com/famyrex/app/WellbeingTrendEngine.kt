package com.famyrex.app

import java.text.DecimalFormat
import java.time.LocalDate

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

    fun evaluate(history: List<DailyUsage>, today: LocalDate = LocalDate.now()): Assessment? {
        // The current snapshot is still accumulating during the day. Never use it as
        // one of the completed days that establishes a wellbeing baseline/trend.
        val completed = history.mapNotNull { day ->
            val date = runCatching { LocalDate.parse(day.date.take(10)) }.getOrNull()
            if (date == null || date.isAfter(today.minusDays(1))) null else date to day
        }
            .distinctBy { it.first }
            .sortedBy { it.first }

        // A trend is only meaningful when the seven reference days are consecutive.
        // Missing snapshots must not be treated as zero-usage days.
        if (completed.size < 7) return null
        val recentWindow = completed.takeLast(7)
        if (recentWindow.zipWithNext().any { (a, b) ->
                java.time.temporal.ChronoUnit.DAYS.between(a.first, b.first) != 1L
            }) return null

        val baseline = recentWindow.dropLast(3)
        val recent = recentWindow.takeLast(3)
        if (baseline.size < 4 || recent.size < 3) return null

        val baselineAvg = baseline.map { minutes(it.second) }.average()
        val recentAvg = recent.map { minutes(it.second) }.average()
        if (baselineAvg < 30.0) return null

        val elevated = recent.count { minutes(it.second) >= baselineAvg * 1.25 }
        val veryElevated = recent.count { minutes(it.second) >= baselineAvg * 1.50 }
        val latest = minutes(recent.last().second).toInt()
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
