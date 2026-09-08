package com.famyrex.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/** Client boundary for the trusted pairing callables. */
class FamyrexPairingService(context: Context) {
    private val appContext = context.applicationContext

    fun createInvite(
        familyId: String,
        childLabel: String,
        onSuccess: (code: String, token: String, expiresAtMs: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado.")
            return
        }
        FirebaseFunctions.getInstance()
            .getHttpsCallable("createPairingInvite")
            .call(hashMapOf("familyId" to familyId, "childLabel" to childLabel))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val code = data?.get("code") as? String
                val token = data?.get("token") as? String
                val expiresAtMs = (data?.get("expiresAtMs") as? Number)?.toLong()
                if (code.isNullOrBlank() || token.isNullOrBlank() || expiresAtMs == null) {
                    onError("Firebase devolvió una invitación incompleta.")
                } else {
                    onSuccess(code, token, expiresAtMs)
                }
            }
            .addOnFailureListener { error -> onError(error.toUserMessage()) }
    }

    fun redeemCode(
        code: String,
        childLabel: String,
        onSuccess: (familyId: String, childUid: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado.")
            return
        }
        FirebaseFunctions.getInstance()
            .getHttpsCallable("redeemPairingCode")
            .call(hashMapOf("code" to code, "childLabel" to childLabel))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val familyId = data?.get("familyId") as? String
                val childUid = data?.get("childUid") as? String
                if (familyId.isNullOrBlank() || childUid.isNullOrBlank()) {
                    onError("Firebase devolvió una vinculación incompleta.")
                } else {
                    onSuccess(familyId, childUid)
                }
            }
            .addOnFailureListener { error -> onError(error.toUserMessage()) }
    }

    private fun Throwable.toUserMessage(): String {
        val firebase = this as? FirebaseFunctionsException
        return firebase?.message?.takeIf { it.isNotBlank() }
            ?: message?.takeIf { it.isNotBlank() }
            ?: "No se pudo completar la vinculación."
    }
}
