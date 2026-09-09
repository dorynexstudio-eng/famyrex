package com.famyrex.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.UUID

/** Builds and sends the complete currently stored parental policy to one supervised device. */
class FamilyPolicySyncService(context: Context) {
    private val appContext = context.applicationContext

    fun syncCurrentPolicy(
        familyId: String,
        child: CloudChildDevice,
        onSuccess: (commandId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (familyId.isBlank() || child.uid.isBlank() || child.memberId.isBlank() || child.deviceId.isBlank()) {
            onError("No se puede sincronizar la política sin una identidad familiar completa.")
            return
        }
        if (!isAdultConfigured()) {
            onError("La cuenta de adulto todavía no está conectada a Firebase.")
            return
        }

        val revision = nextRevision(child.deviceId)
        val snapshot = DevicePolicySnapshotAdapter.fromLocalConfig(
            config = ParentalControlStore(appContext).load(),
            deviceId = child.deviceId,
            revision = revision
        )
        if (snapshot.apps.size > MAX_APPS) {
            onError("La política contiene demasiadas aplicaciones para sincronizar.")
            return
        }

        val encoded = DevicePolicySnapshotCodec.encode(snapshot)
        if (encoded.length > MAX_PAYLOAD_LENGTH) {
            onError("La política es demasiado grande para sincronizarla de forma segura.")
            return
        }

        val commandId = UUID.randomUUID().toString()
        FirebaseFunctions.getInstance(REGION)
            .getHttpsCallable("issueFamilyCommand")
            .call(
                hashMapOf(
                    "familyId" to familyId,
                    "targetDeviceUid" to child.uid,
                    "memberId" to child.memberId,
                    "commandId" to commandId,
                    "action" to FamilyControlAction.SYNC_POLICY.name,
                    "value" to encoded
                )
            )
            .addOnSuccessListener { onSuccess(commandId) }
            .addOnFailureListener { onError(it.toUserMessage()) }
    }

    private fun nextRevision(deviceId: String): Long {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previous = prefs.getLong(revisionKey(deviceId), 0L)
        val next = maxOf(System.currentTimeMillis(), previous + 1L)
        prefs.edit().putLong(revisionKey(deviceId), next).apply()
        return next
    }

    private fun revisionKey(deviceId: String): String = "revision_$deviceId"

    private fun isAdultConfigured(): Boolean =
        FirebaseApp.getApps(appContext).isNotEmpty() &&
            FirebaseAuth.getInstance().currentUser?.isAnonymous == false

    private fun Throwable.toUserMessage(): String {
        val firebase = this as? FirebaseFunctionsException
        return firebase?.message?.takeIf { it.isNotBlank() }
            ?: message?.takeIf { it.isNotBlank() }
            ?: "No se pudo sincronizar la política con el dispositivo infantil."
    }

    companion object {
        private const val REGION = "europe-west1"
        private const val PREFS_NAME = "famyrex_policy_sync_sender"
        private const val MAX_APPS = 100
        private const val MAX_PAYLOAD_LENGTH = 64 * 1024
    }
}
