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
                emergencyLockStore.setLocked(true)
                receiptSuccess(command, nowMs)
            }
            FamilyControlAction.UNLOCK_DEVICE -> {
                emergencyLockStore.setLocked(false)
                receiptSuccess(command, nowMs)
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
        // check + execution + complete must be atomic within this process;
        // otherwise two FCM callbacks could both pass the replay check.
        private val EXECUTION_LOCK = Any()
    }
}
