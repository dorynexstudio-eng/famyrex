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

        val firebaseApp = FirebaseApp.initializeApp(this) ?: return
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

        // FCM is the fast path; WorkManager periodically recovers commands missed while offline.
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
