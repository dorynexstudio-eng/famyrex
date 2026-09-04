package com.famyrex.app

import java.text.DecimalFormat

/**
 * Analiza cambios de comportamiento a partir de métricas agregadas.
 * No interpreta emociones, intenciones ni contenido privado.
 */
object BehaviorPatternEngine {
    private val decimal = DecimalFormat("0.0")

    fun evaluate(history: List<DailyUsage>): List<SmartAlert> {
        if (history.size < 7) return emptyList()

        val ordered = history.sortedBy { it.date }
        val today = ordered.last()
        val previous = ordered.dropLast(1)
        val baselineDays = previous.takeLast(6)
        if (baselineDays.size < 5) return emptyList()

        val alerts = mutableListOf<SmartAlert>()
        val baselineMinutes = baselineDays.map { it.totalTimeMs / 60_000.0 }.average()
        val todayMinutes = today.totalTimeMs / 60_000.0

        // Una subida aislada no basta: exigimos persistencia en al menos 2 de 3 días.
        val recent3 = ordered.dropLast(1).takeLast(3)
        if (baselineMinutes > 30) {
            val elevatedDays = recent3.count {
                (it.totalTimeMs / 60_000.0) >= baselineMinutes * 1.35
            }
            if (elevatedDays >= 2 && todayMinutes >= baselineMinutes * 1.35) {
                val ratio = todayMinutes / baselineMinutes
                alerts += SmartAlert(
                    id = "behavior_sustained_increase_${today.date}",
                    type = AlertType.PATTERN_CHANGE,
                    severity = if (ratio >= 1.75 && elevatedDays == 3) AlertSeverity.IMPORTANT else AlertSeverity.ATTENTION,
                    title = "Cambio sostenido de actividad",
                    message = "El uso total lleva varios días por encima de su referencia reciente (${decimal.format(ratio)}× aproximadamente hoy).",
                    date = today.date
                )
            }
        }

        // Detecta la aparición de una aplicación con uso relevante después de no aparecer
        // entre las principales aplicaciones durante varios días. No la etiqueta como peligrosa.
        val olderApps = baselineDays.flatMap { it.topApps }.map { it.packageName }.toSet()
        val emerging = today.topApps.firstOrNull { app ->
            app.totalTimeMs >= 60 * 60_000L && app.packageName !in olderApps
        }
        if (emerging != null) {
            alerts += SmartAlert(
                id = "behavior_new_relevant_app_${today.date}_${emerging.packageName}",
                type = AlertType.APP_SPIKE,
                severity = AlertSeverity.ATTENTION,
                title = "Nueva aplicación con uso relevante",
                message = "${emerging.label} aparece ahora con al menos 60 min de uso y no figuraba entre las aplicaciones principales de la referencia reciente.",
                date = today.date,
                packageName = emerging.packageName
            )
        }

        return alerts
    }
}
