package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local safety gate for commands that may arrive from the future online layer.
 * Validation is kept pure; this class adds persistent replay protection.
 */
class FamilyCommandGate(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun accept(
        command: FamilyControlCommand,
        identity: FamyrexDeviceIdentity,
        nowMs: Long = System.currentTimeMillis()
    ): CommandGateResult {
        FamilyCommandValidator.validate(command, identity, nowMs)?.let {
            return CommandGateResult.REJECTED(it)
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
