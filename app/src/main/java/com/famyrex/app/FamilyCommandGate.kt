package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local safety gate for commands that may arrive from the online layer.
 * Validation is performed before a command can be consumed. A command is
 * recorded only after its local execution succeeds, so malformed or unsupported
 * commands can never poison the replay ledger.
 */
class FamilyCommandGate(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun check(
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
        return CommandGateResult.ACCEPTED
    }

    /** Marks a successfully executed command as consumed. */
    fun complete(commandId: String, completedAtMs: Long = System.currentTimeMillis()): Boolean {
        require(commandId.isNotBlank())
        if (isConsumed(commandId)) return true
        val items = consumedCommands()
            .filter { completedAtMs - it.second <= RECEIPT_RETENTION_MS }
            .toMutableList()
        items += commandId to completedAtMs

        val json = JSONArray()
        items.takeLast(MAX_RECEIPTS).forEach { (id, timestamp) ->
            json.put(JSONObject().apply {
                put("id", id)
                put("acceptedAtMs", timestamp)
            })
        }
        // A successful command must not be considered consumed only in memory. Use commit()
        // so a process death immediately after execution cannot normally reopen the replay window.
        return prefs.edit().putString(KEY_CONSUMED, json.toString()).commit()
    }

    /** Removes replay state when the supervised enrollment is reset. */
    fun clear() {
        prefs.edit().remove(KEY_CONSUMED).commit()
    }

    /** Backwards-compatible acceptance API; callers should prefer check + complete. */
    fun accept(
        command: FamilyControlCommand,
        identity: FamyrexDeviceIdentity,
        nowMs: Long = System.currentTimeMillis()
    ): CommandGateResult = check(command, identity, nowMs).also {
        if (it.status == CommandGateStatus.ACCEPTED) complete(command.commandId, nowMs)
    }

    private fun isConsumed(commandId: String): Boolean = consumedCommands().any { it.first == commandId }

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
