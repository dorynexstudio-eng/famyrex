package com.famyrex.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

/** Registers the current device's FCM delivery token through the trusted backend. */
object FamilyDeviceTokenRegistrar {
    fun register(context: Context, token: String? = null) {
        val appContext = context.applicationContext
        val identity = FamilyDeviceIdentityStore(appContext).current() ?: return
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (!identity.isSupervised || identity.firebaseUid != firebaseUid) return

        val resolvedToken = token?.trim()?.takeIf { it.isNotBlank() }
            ?: appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: return

        FirebaseFunctions.getInstance(REGION)
            .getHttpsCallable("registerDeviceToken")
            .call(
                hashMapOf(
                    "familyId" to identity.familyId.orEmpty(),
                    "token" to resolvedToken
                )
            )
    }

    fun rememberToken(context: Context, token: String) {
        if (token.isBlank()) return
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    private const val REGION = "europe-west1"
    private const val PREFS = "famyrex_messaging"
    private const val KEY_FCM_TOKEN = "fcm_token"
}
