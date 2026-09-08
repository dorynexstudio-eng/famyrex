package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local safety gate for commands that may arrive from the future online layer.
 * It authenticates by identity binding, expiry and replay protection before any
 * executor is allowed to perform an action on the device.
 */
class FamilyCommandGate(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun accept(
        command: FamilyControlCommand,
        identity: FamyrexDeviceIdentity,
        nowMs: Long = System.currentTimeMillis()
    ): CommandGateResult {
        if (command.familyId.isBlank() || command.memberId.isBlank() || command.deviceId.isBlank()) {
            return CommandGateResult.REJECTED("Identidad de comando incompleta.")
        }
        if (identity.familyId != null && command.familyId != identity.familyId) {
            return CommandGateResult.REJECTED("La familia del comando no coincide con el dispositivo.")
        }
        if (command.memberId != identity.famyrexMemberId) {
            return CommandGateResult.REJECTED("El miembro del comando no coincide con el dispositivo.")
        }
        if (command.deviceId != identity.deviceId) {
            return CommandGateResult.REJECTED("El dispositivo del comando no coincide.")
        }
        if (command.expiresAtMs <= command.issuedAtMs || nowMs >= command.expiresAtMs) {
            return CommandGateResult.REJECTED("El comando ha caducado.")
        }
        if (command.issuedAtMs > nowMs + MAX_CLOCK_SKEW_MS) {
            return CommandGateResult.REJECTED("El comando tiene una fecha futura no válida.")
        }
        if (command.expiresAtMs - command.issuedAtMs > MAX_COMMAND_LIFETIME_MS) {
            return CommandGateResult.REJECTED("La duración del comando no es válida.")
        }
        if (command.action == FamilyControlAction.SYNC_POLICY && command.value.isNullOrBlank()) {
            return CommandGateResult.REJECTED("La sincronización no contiene una política.")
        }
        if (isConsumed(command.commandId)) {
            return CommandGateResult.REPLAY("El comando ya fue procesado.")
        }

        markConsumed(command.commandId, nowMs)
        return CommandGateResult.ACCEPTED
    }

    private fun isConsumed(commandId: String): Boolean = consumedCommands().any { it.first == commandId }

    private fun markConsumed(commandId: String, acceptedAtMs: Long) {
        val items = consumedCommands()
            .filter { acceptedAtMs - it.second <= RECEIPT_RETENTION_MS }
            .toMutableList()
        items += commandId to acceptedAtMs

        val json = JSONArray()
        items.takeLast(MAX_RECEIPTS).forEach { (id, timestamp) ->
            json.put(JSONObject().apply {
                put("id", id)
                put("acceptedAtMs", timestamp)
            })
        }
        prefs.edit().putString(KEY_CONSUMED, json.toString()).apply()
    }

    private fun consumedCommands(): List<Pair<String, Long>> = runCatching {
        val array = JSONArray(prefs.getString(KEY_CONSUMED, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(item.getString("id") to item.getLong("acceptedAtMs"))
            }
        }
    }.getOrDefault(emptyList())

    companion object {
        private const val PREFS_NAME = "famyrex_command_gate"
        private const val KEY_CONSUMED = "consumed_commands"
        private const val MAX_RECEIPTS = 200
        private const val MAX_CLOCK_SKEW_MS = 5 * 60 * 1000L
        private const val MAX_COMMAND_LIFETIME_MS = 24 * 60 * 60 * 1000L
        private const val RECEIPT_RETENTION_MS = 7 * 24 * 60 * 60 * 1000L
    }
}

enum class CommandGateStatus {
    ACCEPTED,
    REJECTED,
    REPLAY
}

data class CommandGateResult(
    val status: CommandGateStatus,
    val reason: String? = null
) {
    companion object {
        val ACCEPTED = CommandGateResult(CommandGateStatus.ACCEPTED)
        fun REJECTED(reason: String) = CommandGateResult(CommandGateStatus.REJECTED, reason)
        fun REPLAY(reason: String) = CommandGateResult(CommandGateStatus.REPLAY, reason)
    }
}
