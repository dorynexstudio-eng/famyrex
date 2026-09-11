package com.famyrex.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
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
     *
     * A small lookback reconstructs an app that was already foreground at the
     * window boundary. The lookback event is never counted before [start].
     *
     * Android 10+ exposes activity-level RESUMED/PAUSED events alongside the older
     * app-level FOREGROUND/BACKGROUND events. Mixing both streams can overwrite
     * the same package's start time or close an interval twice, undercounting use.
     * Use one event model per platform instead.
     */
    private fun query(context: Context, start: Long, end: Long): List<AppUsage> {
        if (end <= start) return emptyList()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val lookbackStart = (start - LOOKBACK_MS).coerceAtLeast(0L)
        val events = manager.queryEvents(lookbackStart, end)
        val event = UsageEvents.Event()
        val foregroundSince = mutableMapOf<String, Long>()
        val totals = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg == context.packageName) continue

            val isForeground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            val isBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED
            } else {
                event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
            }

            when {
                isForeground -> foregroundSince[pkg] = event.timeStamp
                isBackground -> {
                    val foregroundStart = foregroundSince.remove(pkg) ?: continue
                    addClippedDuration(totals, pkg, foregroundStart, event.timeStamp, start, end)
                }
            }
        }

        foregroundSince.forEach { (pkg, foregroundStart) ->
            addClippedDuration(totals, pkg, foregroundStart, end, start, end)
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

    private fun addClippedDuration(
        totals: MutableMap<String, Long>,
        packageName: String,
        foregroundStart: Long,
        foregroundEnd: Long,
        windowStart: Long,
        windowEnd: Long
    ) {
        val clippedStart = maxOf(foregroundStart, windowStart)
        val clippedEnd = minOf(foregroundEnd, windowEnd)
        if (clippedEnd <= clippedStart) return
        val duration = clippedEnd - clippedStart
        totals[packageName] = (totals[packageName] ?: 0L) + duration
    }

    private const val LOOKBACK_MS = 5 * 60 * 1000L
}
