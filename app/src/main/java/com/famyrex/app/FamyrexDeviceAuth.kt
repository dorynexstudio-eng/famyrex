package com.famyrex.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Invisible Firebase identity for a supervised device.
 *
 * This is deliberately not a Google account and is never presented as a
 * child login. It gives Firestore Security Rules a stable authenticated UID
 * while the child remains linked to the parent family only through Famyrex.
 */
class FamyrexDeviceAuth(context: Context) {
    private val appContext = context.applicationContext

    fun currentUser(): FirebaseUser? {
        if (FirebaseApp.getApps(appContext).isEmpty()) return null
        return FirebaseAuth.getInstance().currentUser
    }

    fun ensureAnonymous(onSuccess: (FirebaseUser) -> Unit, onError: (String) -> Unit) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado en Famyrex.")
            return
        }

        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { user ->
            onSuccess(user)
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                result.user?.let(onSuccess)
                    ?: onError("Firebase no devolvió una identidad de dispositivo.")
            }
            .addOnFailureListener { error ->
                onError(error.message?.takeIf { it.isNotBlank() }
                    ?: "No se pudo registrar temporalmente el dispositivo.")
            }
    }
}
