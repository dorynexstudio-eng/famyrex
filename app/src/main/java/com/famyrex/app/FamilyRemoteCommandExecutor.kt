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

        if (command.action == FamilyControlAction.SYNC_POLICY) {
            val snapshot = decodePolicySnapshot(command.value)
                ?: return FamilyControlReceipt(
                    commandId = command.commandId,
                    action = command.action,
                    acceptedAtMs = nowMs,
                    completedAtMs = System.currentTimeMillis(),
                    success = false,
                    reason = "SYNC_POLICY requiere un snapshot válido."
                )
            if (snapshot.deviceId != identity.deviceId) {
                return FamilyControlReceipt(
                    commandId = command.commandId,
                    action = command.action,
                    acceptedAtMs = nowMs,
                    completedAtMs = System.currentTimeMillis(),
                    success = false,
                    reason = "El deviceId del snapshot no coincide con el dispositivo."
                )
            }
            return FamilyControlPolicySync.apply(appContext, snapshot).copy(commandId = command.commandId)
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

    private fun decodePolicySnapshot(value: String?): DevicePolicySnapshot? {
        val raw = value?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        val parts = raw.split('|')
        if (parts.size != 6) return null
        val deviceId = parts[0]
        val dailyLimit = parts[1].toNullableInt() ?: return null
        val start = parts[2].toNullableInt() ?: return null
        val end = parts[3].toNullableInt() ?: return null
        val revision = parts[4].toLongOrNull() ?: return null
        val apps = parts[5].split(';').filter { it.isNotBlank() }.mapNotNull { entry ->
            val fields = entry.split(',', limit = 3)
            if (fields.size != 3) return@mapNotNull null
            val packageName = fields[0].trim()
            val blocked = fields[1].toBooleanStrictOrNull() ?: return@mapNotNull null
            val limit = fields[2].toNullableInt() ?: return@mapNotNull null
            AppPolicy(packageName, packageName, blocked = blocked, dailyLimitMinutes = limit)
        }
        return DevicePolicySnapshot(deviceId, dailyLimit, start, end, apps, revision = revision)
    }

    private fun String.toNullableInt(): Int? = if (this == "null") null else toIntOrNull()
}
