package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DeviceSecurityWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = runCatching {
        if (!FamilyStore(applicationContext).isSupervisedEnrollmentActive()) return Result.success()
        val snapshot = DeviceSecurityChecker(applicationContext).check()
        DeviceSecurityStore(applicationContext).save(snapshot)
        Result.success()
    }.getOrElse {
        Result.retry()
    }
}
