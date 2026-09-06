package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceSecurityStoreTest {
    private fun validSnapshotJson() = """
        {
          "timestampMs":1725580800000,
          "androidVersion":"15",
          "sdkInt":35,
          "debuggable":false,
          "secureLock":true,
          "developerOptions":null,
          "installedAppCount":42,
          "usageAccess":true,
          "foregroundLocation":true,
          "backgroundLocation":false,
          "securityLevel":"GOOD",
          "reasons":["Configuración correcta"]
        }
    """.trimIndent()

    @Test
    fun malformedRootReturnsNull() {
        assertNull(DeviceSecurityStore.parse("not-json"))
    }

    @Test
    fun missingFieldReturnsNullInsteadOfUsingDefaults() {
        val result = DeviceSecurityStore.parse(validSnapshotJson().replace("\"secureLock\":true,", ""))

        assertNull(result)
    }

    @Test
    fun invalidEnumReturnsNullInsteadOfDowngradingToAttention() {
        val result = DeviceSecurityStore.parse(validSnapshotJson().replace("\"GOOD\"", "\"UNKNOWN\""))

        assertNull(result)
    }

    @Test
    fun invalidNumericRangeReturnsNull() {
        val result = DeviceSecurityStore.parse(validSnapshotJson().replace("\"installedAppCount\":42", "\"installedAppCount\":-1"))

        assertNull(result)
    }

    @Test
    fun validSnapshotIsPreserved() {
        val result = DeviceSecurityStore.parse(validSnapshotJson())

        assertEquals(35, result?.sdkInt)
        assertEquals(42, result?.installedAppCount)
        assertEquals(SecurityLevel.GOOD, result?.securityLevel)
        assertEquals(null, result?.isDeveloperOptionsEnabled)
    }
}
