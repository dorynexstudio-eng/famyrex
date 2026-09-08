package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyCloudControlRepositoryValidationTest {
    @Test
    fun runtimePolicySeparatesImplementedAndPlannedActions() {
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.LOCK_DEVICE))
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.SYNC_POLICY))
        assertFalse(RemoteControlActionPolicy.isSupported(FamilyControlAction.REFRESH_LOCATION))
        assertFalse(RemoteControlActionPolicy.isSupported(FamilyControlAction.APPLY_WEB_POLICY))
        assertFalse(RemoteControlActionPolicy.isSupported(FamilyControlAction.REQUEST_APP_APPROVAL))
    }
}
