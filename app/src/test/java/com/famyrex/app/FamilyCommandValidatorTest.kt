package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilyCommandValidatorTest {
    private val identity = FamyrexDeviceIdentity(
        deviceId = "device-1",
        famyrexMemberId = "member-1",
        familyId = "family-1"
    )

    private fun command(
        commandId: String = "cmd-1",
        familyId: String = "family-1",
        memberId: String = "member-1",
        deviceId: String = "device-1",
        issuedAtMs: Long = 1_000L,
        expiresAtMs: Long = 60_000L,
        action: FamilyControlAction = FamilyControlAction.LOCK_DEVICE,
        value: String? = null
    ) = FamilyControlCommand(
        commandId = commandId,
        familyId = familyId,
        memberId = memberId,
        deviceId = deviceId,
        action = action,
        issuedAtMs = issuedAtMs,
        expiresAtMs = expiresAtMs,
        value = value
    )

    @Test fun acceptsValidCommand() {
        assertNull(FamilyCommandValidator.validate(command(), identity, nowMs = 2_000L))
    }

    @Test fun rejectsWrongDevice() {
        assertEquals(
            "El dispositivo del comando no coincide.",
            FamilyCommandValidator.validate(command(deviceId = "other"), identity, nowMs = 2_000L)
        )
    }

    @Test fun rejectsExpiredCommand() {
        assertEquals(
            "El comando ha caducado.",
            FamilyCommandValidator.validate(command(expiresAtMs = 2_000L), identity, nowMs = 2_000L)
        )
    }

    @Test fun rejectsFutureIssuedCommand() {
        assertEquals(
            "El comando tiene una fecha futura no válida.",
            FamilyCommandValidator.validate(command(issuedAtMs = 400_000L, expiresAtMs = 460_000L), identity, nowMs = 1_000L)
        )
    }

    @Test fun requiresValueForPolicySync() {
        assertEquals(
            "La sincronización no contiene una política.",
            FamilyCommandValidator.validate(
                command(action = FamilyControlAction.SYNC_POLICY),
                identity,
                nowMs = 2_000L
            )
        )
    }
}
