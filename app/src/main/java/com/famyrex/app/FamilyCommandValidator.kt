package com.famyrex.app

/** Pure validation shared by local and future remote command transports. */
object FamilyCommandValidator {
    fun validate(
        command: FamilyControlCommand,
        identity: FamyrexDeviceIdentity,
        nowMs: Long = System.currentTimeMillis()
    ): String? {
        if (command.commandId.isBlank()) return "Identidad de comando incompleta."
        if (command.familyId.isBlank() || command.memberId.isBlank() || command.deviceId.isBlank()) {
            return "Identidad de comando incompleta."
        }
        if (identity.familyId != null && command.familyId != identity.familyId) {
            return "La familia del comando no coincide con el dispositivo."
        }
        if (command.memberId != identity.famyrexMemberId) {
            return "El miembro del comando no coincide con el dispositivo."
        }
        if (command.deviceId != identity.deviceId) {
            return "El dispositivo del comando no coincide."
        }
        if (command.expiresAtMs <= command.issuedAtMs || nowMs >= command.expiresAtMs) {
            return "El comando ha caducado."
        }
        if (command.issuedAtMs > nowMs + MAX_CLOCK_SKEW_MS) {
            return "El comando tiene una fecha futura no válida."
        }
        if (command.expiresAtMs - command.issuedAtMs > MAX_COMMAND_LIFETIME_MS) {
            return "La duración del comando no es válida."
        }
        if (command.action == FamilyControlAction.SYNC_POLICY && command.value.isNullOrBlank()) {
            return "La sincronización no contiene una política."
        }
        return null
    }

    private const val MAX_CLOCK_SKEW_MS = 5 * 60 * 1000L
    private const val MAX_COMMAND_LIFETIME_MS = 24 * 60 * 60 * 1000L
}
