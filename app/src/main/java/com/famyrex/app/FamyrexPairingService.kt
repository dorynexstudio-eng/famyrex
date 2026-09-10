package com.famyrex.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException

/** Client boundary for the trusted pairing callables. */
class FamyrexPairingService(context: Context) {
    private val appContext = context.applicationContext

    private fun functions(): FirebaseFunctions =
        FirebaseFunctions.getInstance(FirebaseApp.getInstance(), "europe-west1")

    fun createInvite(
        familyId: String,
        childLabel: String,
        famyrexMemberId: String,
        onSuccess: (code: String, token: String, expiresAtMs: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado.")
            return
        }
        if (familyId.isBlank() || famyrexMemberId.isBlank()) {
            onError("La identidad de la familia o del perfil infantil está incompleta.")
            return
        }
        functions().getHttpsCallable("createPairingInvite")
            .call(hashMapOf("familyId" to familyId, "childLabel" to childLabel, "famyrexMemberId" to famyrexMemberId))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val code = data?.get("code") as? String
                val token = data?.get("token") as? String
                val expiresAtMs = (data?.get("expiresAtMs") as? Number)?.toLong()
                if (code.isNullOrBlank() || token.isNullOrBlank() || expiresAtMs == null) {
                    onError("Firebase devolvió una invitación incompleta.")
                } else {
                    showChildInvitationQr(childLabel, code, token)
                    onSuccess(code, token, expiresAtMs)
                }
            }
            .addOnFailureListener { onError(it.toUserMessage()) }
    }

    private fun showChildInvitationQr(childLabel: String, code: String, token: String) {
        val safeLabel = childLabel.trim().ifBlank { "Perfil infantil" }.take(40)
        val link = Uri.Builder()
            .scheme("famyrex")
            .authority("join")
            .appendQueryParameter("code", code)
            .appendQueryParameter("token", token)
            .appendQueryParameter("label", safeLabel)
            .build()
            .toString()

        val qrIntent = Intent(appContext, FamilyInvitationQrActivity::class.java).apply {
            putExtra(FamilyInvitationQrActivity.EXTRA_LINK, link)
            putExtra(FamilyInvitationQrActivity.EXTRA_LABEL, safeLabel)
            putExtra(FamilyInvitationQrActivity.EXTRA_CODE, code)
            putExtra(FamilyInvitationQrActivity.EXTRA_TOKEN, token)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { appContext.startActivity(qrIntent) }
            .onFailure { /* Never share child invitation secrets outside the two devices. */ }
    }

    fun redeemCode(
        code: String,
        token: String,
        childLabel: String,
        famyrexMemberId: String?,
        famyrexDeviceId: String,
        onSuccess: (familyId: String, childUid: String, memberId: String, deviceId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado.")
            return
        }
        if (token.isBlank()) {
            onError("Falta la clave de vinculación.")
            return
        }
        if (!famyrexMemberId.isNullOrBlank() && famyrexMemberId.length !in 8..128) {
            onError("La identidad del perfil infantil no es válida.")
            return
        }
        if (famyrexDeviceId.isBlank()) {
            onError("La identidad local del dispositivo está incompleta.")
            return
        }

        val auth = FirebaseAuth.getInstance()
        fun redeem() {
            val payload = hashMapOf<String, Any>(
                "code" to code,
                "token" to token,
                "childLabel" to childLabel,
                "famyrexDeviceId" to famyrexDeviceId
            )
            if (!famyrexMemberId.isNullOrBlank()) payload["famyrexMemberId"] = famyrexMemberId
            functions().getHttpsCallable("redeemPairingCode")
                .call(payload)
                .addOnSuccessListener { result ->
                    val data = result.data as? Map<*, *>
                    val familyId = data?.get("familyId") as? String
                    val childUid = data?.get("childUid") as? String
                    val memberId = data?.get("famyrexMemberId") as? String
                    val deviceId = data?.get("famyrexDeviceId") as? String
                    if (familyId.isNullOrBlank() || childUid.isNullOrBlank() || memberId.isNullOrBlank() || deviceId.isNullOrBlank()) {
                        onError("Firebase devolvió una vinculación incompleta.")
                    } else onSuccess(familyId, childUid, memberId, deviceId)
                }
                .addOnFailureListener { onError(it.toUserMessage()) }
        }

        val current = auth.currentUser
        if (current != null && current.providerData.none { it.providerId == "anonymous" }) {
            onError("Este dispositivo tiene una sesión de usuario incompatible con el modo infantil.")
            return
        }
        if (current != null) redeem()
        else auth.signInAnonymously()
            .addOnSuccessListener { redeem() }
            .addOnFailureListener { onError(it.toUserMessage()) }
    }

    fun createParentInvite(
        familyId: String,
        onSuccess: (inviteId: String, token: String, expiresAtMs: Long) -> Unit,
        onError: (String) -> Unit
    ) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado.")
            return
        }
        functions().getHttpsCallable("createParentInvite")
            .call(hashMapOf("familyId" to familyId))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val inviteId = data?.get("inviteId") as? String
                val token = data?.get("token") as? String
                val expiresAtMs = (data?.get("expiresAtMs") as? Number)?.toLong()
                if (inviteId.isNullOrBlank() || token.isNullOrBlank() || expiresAtMs == null) {
                    onError("Firebase devolvió una invitación de adulto incompleta.")
                } else onSuccess(inviteId, token, expiresAtMs)
            }
            .addOnFailureListener { onError(it.toUserMessage()) }
    }

    fun acceptParentInvite(
        familyId: String,
        inviteId: String,
        token: String,
        displayName: String,
        onSuccess: (familyId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado.")
            return
        }
        functions().getHttpsCallable("acceptParentInvite")
            .call(hashMapOf("familyId" to familyId, "inviteId" to inviteId, "token" to token, "displayName" to displayName))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val resolvedFamilyId = data?.get("familyId") as? String
                if (resolvedFamilyId.isNullOrBlank()) onError("Firebase no confirmó la incorporación a la familia.")
                else onSuccess(resolvedFamilyId)
            }
            .addOnFailureListener { onError(it.toUserMessage()) }
    }

    private fun Throwable.toUserMessage(): String {
        val firebase = this as? FirebaseFunctionsException
        return firebase?.message?.takeIf { it.isNotBlank() }
            ?: message?.takeIf { it.isNotBlank() }
            ?: "No se pudo completar la operación familiar."
    }
}
