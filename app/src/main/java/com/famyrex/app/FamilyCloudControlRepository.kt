package com.famyrex.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.UUID

/** Adult-side gateway for discovering supervised devices and issuing trusted commands. */
class FamilyCloudControlRepository(context: Context) {
    private val appContext = context.applicationContext

    fun loadChildren(
        familyId: String,
        onSuccess: (List<CloudChildDevice>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isAdultConfigured()) {
            onError("La cuenta de adulto todavía no está conectada a Firebase.")
            return
        }
        FirebaseFirestore.getInstance()
            .collection("families").document(familyId).collection("members")
            .whereEqualTo("role", "child")
            .get()
            .addOnSuccessListener { members ->
                val children = members.documents.mapNotNull { member ->
                    val uid = member.getString("uid") ?: return@mapNotNull null
                    val memberId = member.getString("memberId") ?: return@mapNotNull null
                    val displayName = member.getString("displayName") ?: "Perfil infantil"
                    val deviceId = member.getString("famyrexDeviceId") ?: return@mapNotNull null
                    CloudChildDevice(uid, memberId, displayName, deviceId)
                }
                onSuccess(children)
            }
            .addOnFailureListener { onError(it.message ?: "No se pudieron cargar los dispositivos infantiles.") }
    }

    fun issueCommand(
        familyId: String,
        child: CloudChildDevice,
        action: FamilyControlAction,
        value: String? = null,
        onSuccess: (commandId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isAdultConfigured()) {
            onError("La cuenta de adulto todavía no está conectada a Firebase.")
            return
        }
        val commandId = UUID.randomUUID().toString()
        FirebaseFunctions.getInstance("europe-west1")
            .getHttpsCallable("issueFamilyCommand")
            .call(hashMapOf(
                "familyId" to familyId,
                "targetDeviceUid" to child.uid,
                "memberId" to child.memberId,
                "commandId" to commandId,
                "action" to action.name,
                "value" to value
            ))
            .addOnSuccessListener { onSuccess(commandId) }
            .addOnFailureListener { onError(it.toUserMessage()) }
    }

    private fun isAdultConfigured(): Boolean =
        FirebaseApp.getApps(appContext).isNotEmpty() &&
            FirebaseAuth.getInstance().currentUser?.isAnonymous == false

    private fun Throwable.toUserMessage(): String {
        val firebase = this as? FirebaseFunctionsException
        return firebase?.message?.takeIf { it.isNotBlank() }
            ?: message?.takeIf { it.isNotBlank() }
            ?: "No se pudo enviar el comando al dispositivo infantil."
    }
}

data class CloudChildDevice(
    val uid: String,
    val memberId: String,
    val displayName: String,
    val deviceId: String
)
