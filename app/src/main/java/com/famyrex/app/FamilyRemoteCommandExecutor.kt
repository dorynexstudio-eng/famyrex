package com.famyrex.app

import android.content.Context

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
        val gateResult = gate.check(command, identity, nowMs)
        if (gateResult.status != CommandGateStatus.ACCEPTED) {
            return@synchronized receipt(command, nowMs, gateResult.reason)
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
                } else if (extraTimeStore.grantMinutes(minutes)) {
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

    private fun receiptSuccess(command: FamilyControlCommand, nowMs: Long): FamilyControlReceipt =
        FamilyControlReceipt(
            commandId = command.commandId,
            action = command.action,
            acceptedAtMs = nowMs,
            completedAtMs = System.currentTimeMillis(),
            success = true
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
