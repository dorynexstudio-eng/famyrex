package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth

/** Periodic recovery for FCM token registration after transient auth/network failures. */
class DeviceTokenRegistrationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val authUser = FirebaseAuth.getInstance().currentUser
            ?: return Result.success()
        if (!authUser.isAnonymous) return Result.success()

        val identity = FamilyDeviceIdentityStore(applicationContext).current()
            ?: return Result.success()
        if (!identity.isSupervised || identity.firebaseUid != authUser.uid) return Result.success()

        val token = FamilyDeviceTokenRegistrar.rememberedToken(applicationContext)
            ?: return Result.success()
        val task = FamilyDeviceTokenRegistrar.register(applicationContext, token)
            ?: return Result.success()
        Tasks.await(task)
        Result.success()
    }.getOrElse {
        Result.retry()
    }
}
