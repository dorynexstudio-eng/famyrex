package com.famyrex.app

import android.content.Context

/**
 * Single entry point for executing a remote family command on this device.
 * Transport code (Firestore/FCM) should decode the payload and delegate here.
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
        val gateResult = gate.accept(command, identity, nowMs)
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

        return when (val result = FamilyControlCommandApplier.apply(command, policyStore.load())) {
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
            is CommandApplicationResult.Rejected -> FamilyControlReceipt(
                commandId = command.commandId,
                action = command.action,
                acceptedAtMs = nowMs,
                completedAtMs = System.currentTimeMillis(),
                success = false,
                reason = result.reason
            )
            is CommandApplicationResult.Unsupported -> FamilyControlReceipt(
                commandId = command.commandId,
                action = command.action,
                acceptedAtMs = nowMs,
                completedAtMs = System.currentTimeMillis(),
                success = false,
                reason = result.reason
            )
        }
    }
}
