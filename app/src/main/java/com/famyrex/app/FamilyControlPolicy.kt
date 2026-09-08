package com.famyrex.app

/**
 * Pure local validation for commands received from any future transport
 * (Firebase, local pairing, or another supported backend).
 */
object FamilyControlPolicy {
    private const val MAX_EXTRA_TIME_MINUTES = 24 * 60
    private const val MAX_DAILY_LIMIT_MINUTES = 24 * 60

    fun validate(command: FamilyControlCommand, nowMs: Long): Boolean {
        if (command.familyId.isBlank() || command.memberId.isBlank() || command.deviceId.isBlank()) return false
        if (command.commandId.isBlank()) return false
        if (command.expiresAtMs <= command.issuedAtMs) return false
        if (command.isExpired(nowMs)) return false

        return when (command.action) {
            FamilyControlAction.GRANT_EXTRA_TIME -> positiveMinutes(command.value, MAX_EXTRA_TIME_MINUTES)
            FamilyControlAction.SET_DAILY_LIMIT -> nonNegativeMinutes(command.value, MAX_DAILY_LIMIT_MINUTES)
            else -> true
        }
    }

    private fun positiveMinutes(value: String?, max: Int): Boolean {
        val minutes = value?.toIntOrNull() ?: return false
        return minutes in 1..max
    }

    private fun nonNegativeMinutes(value: String?, max: Int): Boolean {
        val minutes = value?.toIntOrNull() ?: return false
        return minutes in 0..max
    }
}
