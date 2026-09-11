package com.famyrex.app

import android.app.usage.UsageEvents
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

    /**
     * UsageStats buckets are not suitable for narrow windows: a daily bucket can
     * contain time outside the requested hour. UsageEvents lets us calculate the
     * actual foreground intervals and clip them exactly to [start, end).
     */
    private fun query(context: Context, start: Long, end: Long): List<AppUsage> {
        if (end <= start) return emptyList()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = manager.queryEvents(start, end)
        val event = UsageEvents.Event()
        val foregroundSince = mutableMapOf<String, Long>()
        val totals = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg == context.packageName) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    foregroundSince.putIfAbsent(pkg, event.timeStamp.coerceIn(start, end))
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    val foregroundStart = foregroundSince.remove(pkg) ?: continue
                    val duration = event.timeStamp.coerceAtMost(end) - foregroundStart
                    if (duration > 0L) totals[pkg] = (totals[pkg] ?: 0L) + duration
                }
            }
        }

        val now = end
        foregroundSince.forEach { (pkg, foregroundStart) ->
            val duration = now - foregroundStart
            if (duration > 0L) totals[pkg] = (totals[pkg] ?: 0L) + duration
        }

        return totals
            .filterValues { it > 0L }
            .map { (pkg, totalMs) ->
                AppUsage(
                    pkg,
                    totalMs,
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
