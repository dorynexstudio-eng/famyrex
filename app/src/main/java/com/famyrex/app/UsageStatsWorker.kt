package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.ZoneId

class UsageStatsWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        val context = applicationContext
        val items = UsageRepository.loadToday(context)
        UsageSnapshotStore(context).save(items)
        val date = LocalDate.now(ZoneId.systemDefault()).toString()
        val cumulative = UsageCumulativeSnapshot(System.currentTimeMillis(), items.associate { it.packageName to it.totalTimeMs })
        val cumulativeStore = UsageCumulativeSnapshotStore(context)
        val delta = cumulativeStore.deltaSincePrevious(date, cumulative)
        cumulativeStore.save(date, cumulative)
        UsageIntervalStore(context).save(date, UsageInterval(System.currentTimeMillis(), delta.values.sum()))
        val history = UsageSnapshotStore(context).loadHistory()
        val intervals = UsageIntervalStore(context).load(date)
        AlertStore(context).save(AlertEngine.evaluate(history, intervals, ProtectionSettingsStore(context).load()))
        AiAnalysisWorker.enqueue(context)
        Result.success()
    } catch (_: Exception) { Result.retry() }
}
