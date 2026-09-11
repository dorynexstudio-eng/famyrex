package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.ZoneId
import java.time.LocalTime

class UsageStatsWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        val context = applicationContext
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        val todayKey = today.toString()
        val settings = ProtectionSettingsStore(context).load()
        val items = UsageRepository.loadToday(context)
        UsageSnapshotStore(context).save(items)
        val cumulative = UsageCumulativeSnapshot(now, items.associate { it.packageName to it.totalTimeMs })
        val cumulativeStore = UsageCumulativeSnapshotStore(context)
        val delta = cumulativeStore.deltaSincePrevious(todayKey, cumulative)
        cumulativeStore.save(todayKey, cumulative)
        UsageIntervalStore(context).save(todayKey, UsageInterval(now, delta.values.sum()))

        // A night window such as 22:00-07:00 spans two calendar dates. Only
        // after midnight and before the configured end do we need the previous
        // day's late-night segment. Passing the entire previous day's interval
        // list would also include unrelated 00:00-07:00 usage from that day.
        val intervalStore = UsageIntervalStore(context)
        val currentMinutes = LocalTime.now(zone).let { it.hour * 60 + it.minute }
        val intervals = buildList {
            addAll(intervalStore.load(todayKey))
            if (settings.nightStartMinutes > settings.nightEndMinutes && currentMinutes < settings.nightEndMinutes) {
                val previousDate = today.minusDays(1)
                val previousStart = previousDate
                    .atTime(settings.nightStartMinutes / 60, settings.nightStartMinutes % 60)
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
                addAll(intervalStore.load(previousDate.toString()).filter { it.timestampMs >= previousStart })
            }
        }
        val history = UsageSnapshotStore(context).loadHistory()
        val usageAlerts = AlertEngine.evaluate(history, intervals, settings)
        AlertStore(context).mergeById(usageAlerts)
        AiAnalysisWorker.enqueue(context)
        Result.success()
    } catch (_: Exception) { Result.retry() }
}
