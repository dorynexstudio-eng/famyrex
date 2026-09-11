package com.famyrex.app

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

/** Registers the current device's FCM delivery token through the trusted backend. */
object FamilyDeviceTokenRegistrar {
    /**
     * Returns the callable task so WorkManager can retry a transient registration failure.
     * UI/FCM callers may safely ignore the returned task.
     */
    fun register(context: Context, token: String? = null): Task<*>? {
        val appContext = context.applicationContext
        val identity = FamilyDeviceIdentityStore(appContext).current() ?: return null
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        if (!identity.isSupervised || identity.firebaseUid != firebaseUid) return null

        val resolvedToken = token?.trim()?.takeIf { it.isNotBlank() }
            ?: appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, null)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: return null

        return FirebaseFunctions.getInstance(REGION)
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

    fun rememberedToken(context: Context): String? =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    private const val REGION = "europe-west1"
    private const val PREFS = "famyrex_messaging"
    private const val KEY_FCM_TOKEN = "fcm_token"
}
