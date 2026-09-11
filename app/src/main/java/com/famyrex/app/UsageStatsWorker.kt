package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.ZoneId

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

        // A night window such as 22:00-07:00 spans two calendar dates. After
        // midnight, include the previous day's intervals so the alert can see
        // the complete configured night window without duplicating same-date data.
        val intervalStore = UsageIntervalStore(context)
        val intervals = buildList {
            addAll(intervalStore.load(todayKey))
            if (settings.nightStartMinutes > settings.nightEndMinutes) {
                addAll(intervalStore.load(today.minusDays(1).toString()))
            }
        }
        val history = UsageSnapshotStore(context).loadHistory()
        val usageAlerts = AlertEngine.evaluate(history, intervals, settings)
        AlertStore(context).mergeById(usageAlerts)
        AiAnalysisWorker.enqueue(context)
        Result.success()
    } catch (_: Exception) { Result.retry() }
}
