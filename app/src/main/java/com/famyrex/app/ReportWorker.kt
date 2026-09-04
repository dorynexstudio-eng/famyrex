package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReportWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        val context = applicationContext
        val history = UsageSnapshotStore(context).loadHistory()
        val alerts = AlertStore(context).load()

        ReportPeriod.entries.forEach { period ->
            ReportStore(context).save(
                ReportEngine.build(history, alerts, period)
            )
        }
        Result.success()
    }.getOrElse {
        Result.retry()
    }
}
