package com.famyrex.app

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Local explainable analysis layer.
 *
 * It is intentionally deterministic: it summarizes observed usage and does not
 * claim to infer emotions, intent, diagnoses, or private content.
 */
object AiUsageAnalyzer {

    fun summarize(
        history: List<DailyUsage>,
        alerts: List<SmartAlert>,
        wellbeing: WellbeingAssessment?
    ): AiDailySummary {
        if (history.isEmpty()) {
            return AiDailySummary(
                headline = "Aún no hay suficiente historial",
                body = "Famyrex necesita varios registros de uso antes de generar tendencias fiables.",
                insights = emptyList()
            )
        }

        val today = history.maxByOrNull { it.date } ?: return AiDailySummary(
            "Sin datos de hoy", "No hay datos suficientes.", emptyList()
        )

        val previous = history.dropLast(1).takeLast(7)
        val previousAvg = previous.map { it.totalTimeMs }.average()
        val todayMinutes = today.totalTimeMs / 60_000L
        val insights = mutableListOf<AiInsight>()

        if (previous.size >= 3 && previousAvg > 0) {
            val change = ((today.totalTimeMs / previousAvg) - 1.0) * 100.0
            val rounded = change.roundToInt()
            if (abs(rounded) >= 15) {
                val direction = if (rounded > 0) "ha aumentado" else "ha disminuido"
                insights += AiInsight(
                    title = "Cambio respecto a la referencia",
                    summary = "El uso de hoy $direction aproximadamente un ${abs(rounded)}% frente al promedio reciente.",
                    confidence = if (previous.size >= 6) 85 else 70,
                    supportingSignals = listOf("promedio de días anteriores", "uso de hoy")
                )
            }
        }

        val topApp = today.topApps.firstOrNull()
        if (topApp != null && today.totalTimeMs > 0) {
            val share = topApp.totalTimeMs.toDouble() / today.totalTimeMs
            if (share >= 0.45) {
                insights += AiInsight(
                    title = "Uso concentrado",
                    summary = "${topApp.label} concentra aproximadamente ${(share * 100).roundToInt()}% del uso registrado de hoy.",
                    confidence = 90,
                    supportingSignals = listOf("uso por aplicación")
                )
            }
        }

        if (alerts.isNotEmpty()) {
            insights += AiInsight(
                title = "Señales que requieren atención",
                summary = "Hay ${alerts.size} alerta(s) activas. Famyrex puede priorizarlas por gravedad, pero una alerta no demuestra por sí sola que exista un problema personal.",
                confidence = 95,
                supportingSignals = alerts.take(4).map { it.title }
            )
        }

        if (wellbeing != null) {
            insights += AiInsight(
                title = "Bienestar digital",
                summary = wellbeing.recommendation,
                confidence = 80,
                supportingSignals = listOf(
                    "objetivo diario",
                    "uso nocturno",
                    "muestras disponibles"
                )
            )
        }

        val headline = when {
            alerts.any { it.severity == AlertSeverity.IMPORTANT } -> "Hay señales de uso que conviene revisar"
            todayMinutes == 0L -> "Todavía no hay uso registrado hoy"
            insights.isEmpty() -> "El uso de hoy está dentro de un patrón estable"
            else -> "Resumen inteligente del uso de hoy"
        }

        val body = buildString {
            append("Hoy se han registrado aproximadamente $todayMinutes minutos de uso.")
            if (previous.size >= 3) {
                append(" El análisis compara el dato con el historial reciente disponible.")
            } else {
                append(" El historial todavía es corto, por lo que las tendencias tienen menor confianza.")
            }
        }

        return AiDailySummary(headline, body, insights.take(5))
    }
}
