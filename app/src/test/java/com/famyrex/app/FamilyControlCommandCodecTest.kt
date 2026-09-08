package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FamilyControlCommandCodecTest {
    @Test fun roundTripPreservesCommand() {
        val command = FamilyControlCommand(
            commandId = "cmd-42",
            familyId = "family-1",
            memberId = "member-1",
            deviceId = "device-1",
            action = FamilyControlAction.SET_DAILY_LIMIT,
            issuedAtMs = 1_000L,
            expiresAtMs = 60_000L,
            value = "90",
            requiresAdultConfirmation = true
        )

        assertEquals(command, FamilyControlCommandCodec.decode(FamilyControlCommandCodec.encode(command)))
    }

    @Test fun malformedPayloadIsRejected() {
        assertNull(FamilyControlCommandCodec.decode("not-json"))
        assertNull(FamilyControlCommandCodec.decode("{}"))
    }

    @Test fun unknownActionIsRejected() {
        val raw = """
            {"commandId":"x","familyId":"f","memberId":"m","deviceId":"d","action":"UNKNOWN","issuedAtMs":1,"expiresAtMs":2}
        """.trimIndent()
        assertNull(FamilyControlCommandCodec.decode(raw))
    }
}
