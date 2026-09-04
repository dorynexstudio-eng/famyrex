package com.famyrex.app

import java.time.LocalDate

data class WeeklyTrend(
    val currentMinutes: Long,
    val previousMinutes: Long,
    val changePercent: Int?
)

data class DailyPoint(val date: String, val minutes: Long)

object UsageAnalytics {
    fun last7Days(history: List<DailyUsage>): List<DailyPoint> {
        val byDate = history.associateBy { it.date }
        return (6 downTo 0).map { offset ->
            val d = LocalDate.now().minusDays(offset.toLong())
            DailyPoint(d.toString(), (byDate[d.toString()]?.totalTimeMs ?: 0L) / 60_000L)
        }
    }

    fun weeklyTrend(history: List<DailyUsage>): WeeklyTrend {
        val now = LocalDate.now()
        val currentStart = now.minusDays(6)
        val previousStart = now.minusDays(13)
        val current = history.filter {
            val d = LocalDate.parse(it.date)
            !d.isBefore(currentStart) && !d.isAfter(now)
        }.sumOf { it.totalTimeMs } / 60_000L
        val previous = history.filter {
            val d = LocalDate.parse(it.date)
            !d.isBefore(previousStart) && d.isBefore(currentStart)
        }.sumOf { it.totalTimeMs } / 60_000L
        val change: Int? = if (previous > 0L) {
            ((current - previous) * 100L / previous).toInt()
        } else {
            null
        }
        return WeeklyTrend(current, previous, change)
    }

    fun appTrend(history: List<DailyUsage>, packageName: String): Int? {
        val today = history.maxByOrNull { it.date } ?: return null
        val todayMs = today.topApps.firstOrNull { it.packageName == packageName }?.totalTimeMs ?: 0L
        val previous = history.filter { it.date != today.date }.take(6)
            .mapNotNull { d -> d.topApps.firstOrNull { it.packageName == packageName }?.totalTimeMs }
        if (previous.isEmpty()) return null
        val avg = previous.average()
        return if (avg > 0.0) {
            (((todayMs.toDouble() - avg) / avg) * 100.0).toInt()
        } else {
            null
        }
    }
}
