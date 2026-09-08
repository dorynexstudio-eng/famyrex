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

    fun execute(
        command: FamilyControlCommand,
        identity: FamyrexDeviceIdentity,
        nowMs: Long = System.currentTimeMillis()
    ): FamilyControlReceipt {
        val gateResult = gate.check(command, identity, nowMs)
        if (gateResult.status != CommandGateStatus.ACCEPTED) {
            return FamilyControlReceipt(
                commandId = command.commandId,
                action = command.action,
                acceptedAtMs = nowMs,
                completedAtMs = nowMs,
                success = false,
                reason = gateResult.reason
            )
        }

        val receipt = if (command.action == FamilyControlAction.SYNC_POLICY) {
            val snapshot = DevicePolicySnapshotCodec.decode(command.value.orEmpty())
            if (snapshot == null) {
                receipt(command, nowMs, "SYNC_POLICY requiere un snapshot JSON válido.")
            } else if (snapshot.deviceId != identity.deviceId) {
                receipt(command, nowMs, "El deviceId del snapshot no coincide con el dispositivo.")
            } else {
                FamilyControlPolicySync.apply(appContext, snapshot).copy(commandId = command.commandId)
            }
        } else {
            when (val result = FamilyControlCommandApplier.apply(command, policyStore.load())) {
                is CommandApplicationResult.Applied -> {
                    policyStore.save(result.config)
                    FamilyControlReceipt(
                        commandId = command.commandId,
                        action = command.action,
                        acceptedAtMs = nowMs,
                        completedAtMs = System.currentTimeMillis(),
                        success = true
                    )
                }
                is CommandApplicationResult.Rejected -> receipt(command, nowMs, result.reason)
                is CommandApplicationResult.Unsupported -> receipt(command, nowMs, result.reason)
            }
        }

        if (receipt.success) gate.complete(command.commandId, receipt.completedAtMs ?: nowMs)
        return receipt
    }

    private fun receipt(command: FamilyControlCommand, nowMs: Long, reason: String): FamilyControlReceipt =
        FamilyControlReceipt(
            commandId = command.commandId,
            action = command.action,
            acceptedAtMs = nowMs,
            completedAtMs = System.currentTimeMillis(),
            success = false,
            reason = reason
        )
}
