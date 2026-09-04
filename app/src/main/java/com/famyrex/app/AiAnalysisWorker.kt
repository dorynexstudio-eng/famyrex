package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AiAnalysisWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        fun enqueue(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<AiAnalysisWorker>().build()
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork("famyrex_ai_analysis", androidx.work.ExistingWorkPolicy.REPLACE, request)
        }
    }

    override suspend fun doWork(): Result = runCatching {
        val context = applicationContext
        val historyStore = UsageSnapshotStore(context)
        val intervals = UsageIntervalStore(context).load(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        val settings = WellbeingSettingsStore(context).load()
        val history = historyStore.loadHistory()
        val alerts = AlertStore(context).load()
        val today = history.maxByOrNull { it.date }
        val wellbeing = today?.let {
            WellbeingEngine.evaluate(it.totalTimeMs / 60_000L, intervals, settings)
        }

        AiSummaryStore(context).save(
            AiUsageAnalyzer.summarize(history, alerts, wellbeing)
        )
        Result.success()
    }.getOrElse {
        Result.retry()
    }
}
