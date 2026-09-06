package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectionSettingsStoreTest {
    @Test
    fun normalizationPreservesValidSettings() {
        val settings = ProtectionSettings(
            nightStartMinutes = 1320,
            nightEndMinutes = 360,
            nightMinutesThreshold = 45,
            dailyMinutesThreshold = 180,
            appSpikePercent = 75,
            sensitivity = 3
        )

        assertEquals(settings, ProtectionSettingsStore.normalize(settings))
    }

    @Test
    fun normalizationPreventsInvalidPersistedValues() {
        val settings = ProtectionSettings(
            nightStartMinutes = -1,
            nightEndMinutes = 2000,
            nightMinutesThreshold = 0,
            dailyMinutesThreshold = -10,
            appSpikePercent = 0,
            sensitivity = 99
        )

        assertEquals(
            ProtectionSettings(
                nightStartMinutes = 0,
                nightEndMinutes = 1439,
                nightMinutesThreshold = 1,
                dailyMinutesThreshold = 1,
                appSpikePercent = 1,
                sensitivity = 3
            ),
            ProtectionSettingsStore.normalize(settings)
        )
    }
}
