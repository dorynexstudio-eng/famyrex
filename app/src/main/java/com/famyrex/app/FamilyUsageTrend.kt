package com.famyrex.app

/** Tendencia agregada de tiempo de pantalla. No interpreta intenciones ni emociones. */
data class FamilyUsageTrend(
    val days: List<Long>,
    val todayMinutes: Long,
    val previousAverageMinutes: Double?,
    val direction: FamilyUsageTrendDirection,
    val anomaly: FamilyUsageAnomaly? = null
)

enum class FamilyUsageTrendDirection { INCREASING, STABLE, DECREASING, INSUFFICIENT_DATA }

data class FamilyUsageAnomaly(
    val dayIndex: Int,
    val minutes: Long,
    val referenceAverageMinutes: Double,
    val deviationPercent: Int,
    val type: FamilyUsageAnomalyType
)

enum class FamilyUsageAnomalyType { HIGH, LOW }

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
        return FamilyUsageTrend(
            days = dailyMinutes,
            todayMinutes = today,
            previousAverageMinutes = average,
            direction = direction,
            anomaly = detectAnomaly(dailyMinutes)
        )
    }

    private fun detectAnomaly(dailyMinutes: List<Long>): FamilyUsageAnomaly? {
        if (dailyMinutes.size < 3) return null
        val today = dailyMinutes.last()
        val previous = dailyMinutes.dropLast(1)
        val average = previous.average()
        if (average <= 0.0) return null

        val deviation = ((today - average) / average) * 100.0
        val type = when {
            deviation >= 50.0 -> FamilyUsageAnomalyType.HIGH
            deviation <= -50.0 -> FamilyUsageAnomalyType.LOW
            else -> return null
        }
        return FamilyUsageAnomaly(
            dayIndex = dailyMinutes.lastIndex,
            minutes = today,
            referenceAverageMinutes = average,
            deviationPercent = kotlin.math.abs(deviation).toInt(),
            type = type
        )
    }
}
