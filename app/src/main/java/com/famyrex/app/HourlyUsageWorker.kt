package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDateTime
import java.time.ZoneId

class HourlyUsageWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val context = applicationContext
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val completedHour = now.withMinute(0).withSecond(0).withNano(0).minusHours(1)
        HourlyUsageSnapshotStore(context).save(
            UsageRepository.loadPreviousCompletedHour(context, now),
            completedHour
        )
        Result.success()
    }.getOrElse { Result.retry() }
}
