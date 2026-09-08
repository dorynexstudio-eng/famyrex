package com.famyrex.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class FamyrexApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val firebaseApp = FirebaseApp.initializeApp(this) ?: return
        FirebaseAppCheck.getInstance(firebaseApp)
            .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())

        // Re-register on every process start so backend delivery remains recoverable
        // even when FCM did not issue a new token callback.
        FamilyDeviceTokenRegistrar.register(this)
        // FCM is the fast path; WorkManager periodically recovers commands missed while offline.
        FamyrexWorkScheduler.scheduleProtectionHealth(this)
    }
}
