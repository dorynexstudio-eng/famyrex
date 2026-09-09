package com.famyrex.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.UUID

/** Adult-side gateway for discovering supervised devices, app inventory, device status, issuing commands and reading execution status. */
class FamilyCloudControlRepository(context: Context) {
    private val appContext = context.applicationContext

    fun loadChildren(
        familyId: String,
        onSuccess: (List<CloudChildDevice>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (familyId.isBlank()) {
            onError("La familia no tiene un identificador válido.")
            return
        }
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

    fun loadChildAppInventory(
        familyId: String,
        child: CloudChildDevice,
        onSuccess: (ChildAppInventoryState) -> Unit,
        onError: (String) -> Unit
    ) {
        if (familyId.isBlank() || child.uid.isBlank() || child.deviceId.isBlank()) {
            onError("No se puede consultar el inventario sin una identidad infantil completa.")
            return
        }
        if (!isAdultConfigured()) {
            onError("La cuenta de adulto todavía no está conectada a Firebase.")
            return
        }
        FirebaseFirestore.getInstance()
            .collection("families").document(familyId)
            .collection("devices").document(child.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val rawApps = snapshot.get("appInventory") as? List<*> ?: emptyList<Any?>()
                val apps = rawApps.mapNotNull { raw ->
                    val map = raw as? Map<*, *> ?: return@mapNotNull null
                    val packageName = map["packageName"] as? String ?: return@mapNotNull null
                    if (!PACKAGE_NAME_REGEX.matches(packageName)) return@mapNotNull null
                    val label = (map["label"] as? String).orEmpty().trim().take(100).ifBlank { packageName }
                    ChildAppInventoryItem(packageName, label)
                }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
                val rawUpdatedAtMs = snapshot.getLong("appInventoryUpdatedAtMs") ?: 0L
                val updatedAtMs = rawUpdatedAtMs.takeIf { isPlausibleInventoryTimestamp(it) } ?: 0L
                onSuccess(ChildAppInventoryState(apps, updatedAtMs))
            }
            .addOnFailureListener { onError(it.message ?: "No se pudo consultar el inventario de aplicaciones.") }
    }

    fun loadChildDeviceStatus(
        familyId: String,
        child: CloudChildDevice,
        onSuccess: (ChildDeviceStatus) -> Unit,
        onError: (String) -> Unit
    ) {
        if (familyId.isBlank() || child.uid.isBlank() || child.deviceId.isBlank()) {
            onError("No se puede consultar el estado sin una identidad infantil completa.")
            return
        }
        if (!isAdultConfigured()) {
            onError("La cuenta de adulto todavía no está conectada a Firebase.")
            return
        }
        FirebaseFirestore.getInstance()
            .collection("families").document(familyId)
            .collection("devices").document(child.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val updatedAtMs = snapshot.getLong("deviceStatusUpdatedAtMs") ?: 0L
                val protectionCheckedAtMs = snapshot.getLong("protectionCheckedAtMs") ?: 0L
                val protectionReasons = (snapshot.get("protectionReasons") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.map { it.trim().take(200) }
                    ?.filter { it.isNotBlank() }
                    ?.take(8)
                    ?: emptyList()
                onSuccess(
                    ChildDeviceStatus(
                        updatedAtMs = updatedAtMs,
                        protectionActive = snapshot.getBoolean("protectionActive"),
                        protectionCheckedAtMs = protectionCheckedAtMs,
                        protectionReasons = protectionReasons
                    )
                )
            }
            .addOnFailureListener { onError(it.message ?: "No se pudo consultar el estado del dispositivo infantil.") }
    }

    fun issueCommand(
        familyId: String,
        child: CloudChildDevice,
        action: FamilyControlAction,
        value: String? = null,
        onSuccess: (commandId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (familyId.isBlank()) {
            onError("La familia no tiene un identificador válido.")
            return
        }
        if (!RemoteControlActionPolicy.isSupported(action)) {
            onError("Esta acción todavía no está disponible en el dispositivo supervisado.")
            return
        }
        if (child.uid.isBlank() || child.memberId.isBlank() || child.deviceId.isBlank()) {
            onError("El dispositivo infantil no tiene una identidad cloud completa.")
            return
        }
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

    fun loadCommandReceipt(
        familyId: String,
        commandId: String,
        deviceId: String,
        onSuccess: (CloudCommandReceipt?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (familyId.isBlank() || commandId.isBlank() || deviceId.isBlank()) {
            onError("No se puede consultar el recibo sin una identidad completa.")
            return
        }
        if (!isAdultConfigured()) {
            onError("La cuenta de adulto todavía no está conectada a Firebase.")
            return
        }
        FirebaseFirestore.getInstance()
            .collection("families").document(familyId)
            .collection("commands").document(commandId)
            .collection("receipts").document(deviceId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }
                val action = runCatching { FamilyControlAction.valueOf(snapshot.getString("action").orEmpty()) }.getOrNull()
                if (action == null) {
                    onError("El recibo remoto tiene una acción no reconocida.")
                    return@addOnSuccessListener
                }
                onSuccess(
                    CloudCommandReceipt(
                        commandId = commandId,
                        deviceId = snapshot.getString("deviceId").orEmpty(),
                        action = action,
                        success = snapshot.getBoolean("success") == true,
                        acceptedAtMs = snapshot.getLong("acceptedAtMs") ?: 0L,
                        completedAtMs = snapshot.getLong("completedAtMs"),
                        reason = snapshot.getString("reason")
                    )
                )
            }
            .addOnFailureListener { onError(it.message ?: "No se pudo consultar el estado de la orden.") }
    }

    private fun isAdultConfigured(): Boolean =
        FirebaseApp.getApps(appContext).isNotEmpty() &&
            FirebaseAuth.getInstance().currentUser?.isAnonymous == false

    private fun isPlausibleInventoryTimestamp(timestampMs: Long): Boolean {
        if (timestampMs <= 0L) return false
        val now = System.currentTimeMillis()
        return timestampMs <= now + FUTURE_TIMESTAMP_SKEW_MS
    }

    private fun Throwable.toUserMessage(): String {
        val firebase = this as? FirebaseFunctionsException
        return firebase?.message?.takeIf { it.isNotBlank() }
            ?: message?.takeIf { it.isNotBlank() }
            ?: "No se pudo enviar el comando al dispositivo infantil."
    }

    companion object {
        private val PACKAGE_NAME_REGEX = Regex("^[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+$")
        private const val FUTURE_TIMESTAMP_SKEW_MS = 10L * 60L * 1000L
    }
}

data class CloudChildDevice(
    val uid: String,
    val memberId: String,
    val displayName: String,
    val deviceId: String
)

data class ChildAppInventoryItem(
    val packageName: String,
    val label: String
)

data class ChildAppInventoryState(
    val apps: List<ChildAppInventoryItem>,
    val updatedAtMs: Long
)

data class ChildDeviceStatus(
    val updatedAtMs: Long,
    val protectionActive: Boolean?,
    val protectionCheckedAtMs: Long,
    val protectionReasons: List<String>
) {
    fun liveness(nowMs: Long = System.currentTimeMillis()): ChildDeviceLiveness = when {
        updatedAtMs <= 0L -> ChildDeviceLiveness.UNKNOWN
        nowMs - updatedAtMs <= ONLINE_WINDOW_MS -> ChildDeviceLiveness.ONLINE
        nowMs - updatedAtMs <= UNKNOWN_WINDOW_MS -> ChildDeviceLiveness.OFFLINE
        else -> ChildDeviceLiveness.UNKNOWN
    }

    companion object {
        private const val ONLINE_WINDOW_MS = 45L * 60L * 1000L
        private const val UNKNOWN_WINDOW_MS = 24L * 60L * 60L * 1000L
    }
}

enum class ChildDeviceLiveness { ONLINE, OFFLINE, UNKNOWN }

data class CloudCommandReceipt(
    val commandId: String,
    val deviceId: String,
    val action: FamilyControlAction,
    val success: Boolean,
    val acceptedAtMs: Long,
    val completedAtMs: Long?,
    val reason: String?
)
