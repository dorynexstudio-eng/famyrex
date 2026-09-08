package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteControlActionPolicyTest {
    @Test
    fun supportedActionsContainImplementedRuntimeCommands() {
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.LOCK_DEVICE))
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.UNLOCK_DEVICE))
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.GRANT_EXTRA_TIME))
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.SET_DAILY_LIMIT))
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.SET_SCHEDULE))
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.BLOCK_APP))
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.ALLOW_APP))
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.SET_APP_LIMIT))
        assertTrue(RemoteControlActionPolicy.isSupported(FamilyControlAction.SYNC_POLICY))
    }

    @Test
    fun unsupportedActionsStayOutOfRuntimeAllowList() {
        assertFalse(RemoteControlActionPolicy.isSupported(FamilyControlAction.REFRESH_LOCATION))
        assertFalse(RemoteControlActionPolicy.isSupported(FamilyControlAction.APPLY_WEB_POLICY))
        assertFalse(RemoteControlActionPolicy.isSupported(FamilyControlAction.REQUEST_APP_APPROVAL))
    }
}
