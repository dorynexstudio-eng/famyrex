package com.famyrex.app

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object FamyrexWorkScheduler {
    private const val PROTECTION_WORK = "famyrex_protection_health"
    private const val DEVICE_SECURITY_WORK = "famyrex_device_security"
    private const val REPORT_WORK = "famyrex_reports"

    fun scheduleProtectionHealth(context: Context) {
        val appContext = context.applicationContext
        GeofenceBootstrap.sync(appContext)

        val workManager = WorkManager.getInstance(appContext)

        val protectionRequest = PeriodicWorkRequestBuilder<ProtectionHealthWorker>(
            15,
            TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            PROTECTION_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            protectionRequest
        )

        val deviceSecurityRequest = PeriodicWorkRequestBuilder<DeviceSecurityWorker>(
            30,
            TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            DEVICE_SECURITY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            deviceSecurityRequest
        )

        val reportRequest = PeriodicWorkRequestBuilder<ReportWorker>(
            24,
            TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            REPORT_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            reportRequest
        )
    }
}
