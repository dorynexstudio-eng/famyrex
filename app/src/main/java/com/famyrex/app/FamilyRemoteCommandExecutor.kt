package com.famyrex.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/**
 * Single entry point for executing a remote family command on this device.
 * Transport code (Firestore/FCM) decodes the payload and delegates here.
 */
class FamilyRemoteCommandExecutor(context: Context) {
    private val appContext = context.applicationContext
    private val gate = FamilyCommandGate(appContext)
    private val policyStore = ParentalControlStore(appContext)
    private val emergencyLockStore = DeviceEmergencyLockStore(appContext)
    private val extraTimeStore = ExtraTimeAllowanceStore(appContext)
    private val auditStore = RemoteCommandAuditStore(appContext)

    fun execute(
        command: FamilyControlCommand,
        identity: FamyrexDeviceIdentity,
        nowMs: Long = System.currentTimeMillis()
    ): FamilyControlReceipt = synchronized(EXECUTION_LOCK) {
        val preflightFailure = supervisedPreflightFailure(identity)
        if (preflightFailure != null) {
            return@synchronized receipt(command, nowMs, preflightFailure)
        }

        val gateResult = gate.check(command, identity, nowMs)
        if (gateResult.status != CommandGateStatus.ACCEPTED) {
            return@synchronized if (gateResult.status == CommandGateStatus.REPLAY) {
                // A replay means this exact command already completed successfully. Report it
                // as successful so recovery after a lost receipt cannot overwrite a real success
                // with a misleading failure when the queue retries the same command.
                receiptSuccess(command, nowMs, gateResult.reason)
            } else {
                receipt(command, nowMs, gateResult.reason)
            }
        }

        val receipt = when (command.action) {
            FamilyControlAction.LOCK_DEVICE -> {
                emergencyLockStore.setLocked(identity.deviceId, true)
                receiptSuccess(command, nowMs)
            }
            FamilyControlAction.UNLOCK_DEVICE -> {
                emergencyLockStore.setLocked(identity.deviceId, false)
                receiptSuccess(command, nowMs)
            }
            FamilyControlAction.GRANT_EXTRA_TIME -> {
                val minutes = command.value?.trim()?.toIntOrNull()
                if (minutes == null || minutes !in 1..1440) {
                    receipt(command, nowMs, "El tiempo extra debe estar entre 1 y 1440 minutos.")
                } else if (extraTimeStore.grantMinutes(minutes, command.commandId)) {
                    receiptSuccess(command, nowMs)
                } else {
                    receipt(command, nowMs, "No se pudo guardar el tiempo extra.")
                }
            }
            FamilyControlAction.SYNC_POLICY -> {
                val snapshot = DevicePolicySnapshotCodec.decode(command.value.orEmpty())
                if (snapshot == null) {
                    receipt(command, nowMs, "SYNC_POLICY requiere un snapshot JSON válido.")
                } else if (snapshot.deviceId != identity.deviceId) {
                    receipt(command, nowMs, "El deviceId del snapshot no coincide con el dispositivo.")
                } else {
                    FamilyControlPolicySync.apply(appContext, snapshot).copy(commandId = command.commandId)
                }
            }
            else -> {
                when (val result = FamilyControlCommandApplier.apply(command, policyStore.load())) {
                    is CommandApplicationResult.Applied -> {
                        policyStore.save(result.config)
                        receiptSuccess(command, nowMs)
                    }
                    is CommandApplicationResult.Rejected -> receipt(command, nowMs, result.reason)
                    is CommandApplicationResult.Unsupported -> receipt(command, nowMs, result.reason)
                }
            }
        }

        if (receipt.success) gate.complete(command.commandId, receipt.completedAtMs ?: nowMs)
        auditStore.record(receipt)
        receipt
    }

    /**
     * Defense in depth: command execution must only happen from the currently
     * enrolled supervised context, even if a transport layer supplies a forged
     * or stale identity object.
     */
    private fun supervisedPreflightFailure(identity: FamyrexDeviceIdentity): String? {
        if (!identity.isSupervised) return "El dispositivo no está en modo supervisado."
        if (identity.familyId.isNullOrBlank() || identity.famyrexMemberId.isBlank() || identity.deviceId.isBlank()) {
            return "La identidad supervisada está incompleta."
        }
        if (FamilyStore(appContext).appMode() != FamyrexAppMode.SUPERVISED) {
            return "El dispositivo ya no está en modo supervisado."
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: return "La sesión del dispositivo no está disponible."
        if (!currentUser.isAnonymous) return "La sesión del dispositivo no es anónima."
        if (identity.firebaseUid != currentUser.uid) return "La identidad Firebase no coincide con el dispositivo."

        val enrolledIdentity = FamilyDeviceIdentityStore(appContext).current()
            ?: return "No existe una identidad supervisada válida."
        if (enrolledIdentity.familyId != identity.familyId ||
            enrolledIdentity.famyrexMemberId != identity.famyrexMemberId ||
            enrolledIdentity.deviceId != identity.deviceId ||
            enrolledIdentity.firebaseUid != currentUser.uid
        ) {
            return "La identidad del comando no coincide con el dispositivo inscrito."
        }
        return null
    }

    private fun receiptSuccess(
        command: FamilyControlCommand,
        nowMs: Long,
        reason: String? = null
    ): FamilyControlReceipt =
        FamilyControlReceipt(
            commandId = command.commandId,
            action = command.action,
            acceptedAtMs = nowMs,
            completedAtMs = System.currentTimeMillis(),
            success = true,
            reason = reason
        )

    private fun receipt(command: FamilyControlCommand, nowMs: Long, reason: String?): FamilyControlReceipt =
        FamilyControlReceipt(
            commandId = command.commandId,
            action = command.action,
            acceptedAtMs = nowMs,
            completedAtMs = System.currentTimeMillis(),
            success = false,
            reason = reason
        )

    companion object {
        private val EXECUTION_LOCK = Any()
    }
}