package com.famyrex.app

import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class AppUsage(
    val packageName: String,
    val totalTimeMs: Long,
    val label: String = packageName
)

object UsageRepository {
    fun loadToday(context: Context): List<AppUsage> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        return query(context, start, System.currentTimeMillis())
    }

    fun loadCurrentHour(context: Context): List<AppUsage> {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        return loadHour(context, now.withMinute(0).withSecond(0).withNano(0))
    }

    fun loadPreviousCompletedHour(context: Context, now: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())): List<AppUsage> {
        val hour = now.withMinute(0).withSecond(0).withNano(0).minusHours(1)
        return loadHour(context, hour)
    }

    private fun loadHour(context: Context, hour: LocalDateTime): List<AppUsage> {
        val zone = ZoneId.systemDefault()
        val start = hour.atZone(zone).toInstant().toEpochMilli()
        val end = hour.plusHours(1).atZone(zone).toInstant().toEpochMilli()
        return query(context, start, end)
    }

    private fun query(context: Context, start: Long, end: Long): List<AppUsage> {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            .filter { it.packageName != context.packageName && it.totalTimeInForeground > 0 }
            .groupBy { it.packageName }
            .map { (pkg, stats) ->
                AppUsage(
                    pkg,
                    stats.sumOf { it.totalTimeInForeground },
                    runCatching {
                        context.packageManager.getApplicationLabel(
                            context.packageManager.getApplicationInfo(pkg, 0)
                        ).toString()
                    }.getOrDefault(pkg)
                )
            }
            .sortedByDescending { it.totalTimeMs }
    }
}
