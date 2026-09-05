package com.famyrex.app

/** Tendencia agregada de tiempo de pantalla. No interpreta intenciones ni emociones. */
data class FamilyUsageTrend(
    val days: List<Long>,
    val todayMinutes: Long,
    val previousAverageMinutes: Double?,
    val direction: FamilyUsageTrendDirection
)

enum class FamilyUsageTrendDirection { INCREASING, STABLE, DECREASING, INSUFFICIENT_DATA }

object FamilyUsageTrendEvaluator {
    fun evaluate(dailyMinutes: List<Long>): FamilyUsageTrend {
        if (dailyMinutes.isEmpty()) {
            return FamilyUsageTrend(emptyList(), 0L, null, FamilyUsageTrendDirection.INSUFFICIENT_DATA)
        }
        val today = dailyMinutes.last()
        if (dailyMinutes.size < 2) {
            return FamilyUsageTrend(dailyMinutes, today, null, FamilyUsageTrendDirection.INSUFFICIENT_DATA)
        }
        val previous = dailyMinutes.dropLast(1)
        val average = previous.average()
        val direction = when {
            average == 0.0 && today > 0L -> FamilyUsageTrendDirection.INCREASING
            average > 0.0 && today >= average * 1.15 -> FamilyUsageTrendDirection.INCREASING
            average > 0.0 && today <= average * 0.85 -> FamilyUsageTrendDirection.DECREASING
            else -> FamilyUsageTrendDirection.STABLE
        }
        return FamilyUsageTrend(dailyMinutes, today, average, direction)
    }
}
