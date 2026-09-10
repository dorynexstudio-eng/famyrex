package com.famyrex.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions

class SecureParentInvitationService(context: Context) {
    private val appContext = context.applicationContext

    private fun functions() = FirebaseFunctions.getInstance(FirebaseApp.getInstance(), "europe-west1")

    fun create(
        familyId: String,
        onSuccess: (inviteId: String, expiresAtMs: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado.")
            return
        }
        functions().getHttpsCallable("createSecureParentInvite")
            .call(hashMapOf("familyId" to familyId))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val inviteId = data?.get("inviteId") as? String
                val expiresAtMs = (data?.get("expiresAtMs") as? Number)?.toLong()
                if (inviteId.isNullOrBlank() || expiresAtMs == null) {
                    onError("La invitación de adulto está incompleta.")
                } else onSuccess(inviteId, expiresAtMs)
            }
            .addOnFailureListener { onError(it.message ?: "No se pudo crear la invitación.") }
    }

    fun accept(
        inviteId: String,
        displayName: String,
        onSuccess: (familyId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado.")
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.providerData.none { it.providerId == "google.com" }) {
            onError("Inicia sesión con Google para aceptar esta invitación.")
            return
        }
        functions().getHttpsCallable("acceptSecureParentInvite")
            .call(hashMapOf("inviteId" to inviteId, "displayName" to displayName))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val familyId = data?.get("familyId") as? String
                if (familyId.isNullOrBlank()) onError("La incorporación no ha sido confirmada.")
                else onSuccess(familyId)
            }
            .addOnFailureListener { onError(it.message ?: "No se pudo aceptar la invitación.") }
    }
}
