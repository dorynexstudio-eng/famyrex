package com.famyrex.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks

class FamilyLocationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val store = FamilyStore(applicationContext)
        if (store.appMode() != FamyrexAppMode.SUPERVISED || store.verifiedFamilyIdentity() == null) return Result.success()

        val fine = applicationContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = applicationContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return Result.success()

        return runCatching {
            val client = LocationServices.getFusedLocationProviderClient(applicationContext)
            val location = Tasks.await(client.lastLocation)
                ?: Tasks.await(client.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                ))
                ?: return Result.retry()

            Tasks.await(
                com.google.android.gms.tasks.Tasks.forResult(
                    FamilyLocationRepository(applicationContext).publishChildLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = location.accuracy,
                        capturedAtMs = location.time
                    )
                )
            )
            Result.success()
        }.getOrElse { Result.retry() }
    }
}
