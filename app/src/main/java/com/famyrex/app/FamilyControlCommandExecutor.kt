package com.famyrex.app

import android.content.Context

/**
 * Single entry point for applying a remote command to the local protection model.
 * Transport code must never bypass this gate.
 */
class FamilyControlCommandExecutor(context: Context) {
    private val appContext = context.applicationContext

    fun execute(
        command: FamilyControlCommand,
        identity: FamyrexDeviceIdentity,
        nowMs: Long = System.currentTimeMillis()
    ): FamilyControlReceipt {
        val gate = FamilyCommandGate(appContext)
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

        val receipt = when (val result = FamilyControlCommandApplier.apply(command, ParentalControlStore(appContext).load())) {
            is CommandApplicationResult.Applied -> {
                ParentalControlStore(appContext).save(result.config)
                FamilyControlReceipt(command.commandId, command.action, nowMs, System.currentTimeMillis(), true)
            }
            is CommandApplicationResult.Rejected -> FamilyControlReceipt(command.commandId, command.action, nowMs, System.currentTimeMillis(), false, result.reason)
            is CommandApplicationResult.Unsupported -> FamilyControlReceipt(command.commandId, command.action, nowMs, System.currentTimeMillis(), false, result.reason)
        }

        if (receipt.success) {
            gate.complete(command.commandId, receipt.completedAtMs ?: nowMs)
        }
        return receipt
    }
}
