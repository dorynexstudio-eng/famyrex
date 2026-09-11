package com.famyrex.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class FamyrexApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Restore the locally verified supervised state before scheduling any workers.
        // This closes the boot-time window in which a worker could observe PARENT mode
        // before the BOOT_COMPLETED receiver restores the supervised enrollment.
        SupervisedStateRestorer.restore(FamilyStore(this))

        val firebaseApp = FirebaseApp.initializeApp(this)
        if (firebaseApp != null) {
            FirebaseAppCheck.getInstance(firebaseApp)
                .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())

            // FirebaseAuth can restore its persisted anonymous session asynchronously.
            // Retry token registration when that session becomes available, otherwise a
            // process restart could leave the supervised device temporarily unreachable.
            val auth = FirebaseAuth.getInstance()
            auth.addAuthStateListener { restoredAuth ->
                registerRecoveredMessagingToken(restoredAuth.currentUser?.uid)
            }

            // Re-register on every process start so backend delivery remains recoverable
            // even when FCM did not issue a new token callback.
            registerRecoveredMessagingToken(auth.currentUser?.uid)
        }

        // Local protection scheduling must not depend on Firebase initialization. If
        // Firebase is temporarily unavailable, WorkManager can still restore the local
        // protection/usage/health pipeline and retry network-dependent work later.
        FamyrexWorkScheduler.scheduleProtectionHealth(this)
    }

    private fun registerRecoveredMessagingToken(firebaseUid: String?) {
        if (firebaseUid.isNullOrBlank()) return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (token.isBlank()) return@addOnSuccessListener
                FamilyDeviceTokenRegistrar.rememberToken(this, token)
                FamilyDeviceTokenRegistrar.register(this, token)
            }
    }
}
