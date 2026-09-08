package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevicePolicySnapshotCodecTest {
    @Test
    fun `round trip preserves nulls apps metadata and special characters`() {
        val snapshot = DevicePolicySnapshot(
            deviceId = "device|1,2",
            dailyLimitMinutes = null,
            bedtimeStartMinutes = null,
            bedtimeEndMinutes = null,
            webPolicyVersion = 12L,
            revision = 7L,
            apps = listOf(
                AppPolicy(
                    packageName = "com.example.one",
                    displayName = "Niños, juegos",
                    blocked = true,
                    dailyLimitMinutes = null,
                    approvalRequired = true,
                    installAllowed = false,
                    source = AppInstallSource.APK
                ),
                AppPolicy(
                    packageName = "com.example.two",
                    displayName = "App \"safe\"",
                    dailyLimitMinutes = 45
                )
            )
        )

        val decoded = DevicePolicySnapshotCodec.decode(DevicePolicySnapshotCodec.encode(snapshot))

        assertNotNull(decoded)
        assertEquals(snapshot, decoded)
    }

    @Test
    fun `invalid json and invalid enum are rejected`() {
        assertNull(DevicePolicySnapshotCodec.decode("{bad"))
        assertNull(DevicePolicySnapshotCodec.decode("{\"deviceId\":\"d\",\"apps\":[{\"packageName\":\"p\",\"source\":\"NOPE\"}]}"))
    }

    @Test
    fun `numeric policy fields remain bounded by model validation`() {
        val raw = "{\"deviceId\":\"d\",\"dailyLimitMinutes\":2147483648,\"revision\":1,\"apps\":[]}"
        assertNull(DevicePolicySnapshotCodec.decode(raw))
    }

    @Test
    fun `empty apps is valid`() {
        val decoded = DevicePolicySnapshotCodec.decode("{\"deviceId\":\"d\",\"apps\":[]}")
        assertTrue(decoded != null)
        assertEquals(emptyList<AppPolicy>(), decoded?.apps)
    }
}
