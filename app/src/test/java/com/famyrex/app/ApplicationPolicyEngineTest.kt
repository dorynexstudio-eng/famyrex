package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApplicationPolicyEngineTest {
    @Test fun unmanagedAppIsNotBlockedByDefault() {
        assertEquals(ApplicationDecision.UNMANAGED, ApplicationPolicyEngine.decision(null, 0))
    }

    @Test fun blockedAppWinsOverOtherRules() {
        val policy = AppPolicy("com.example", "Example", blocked = true, dailyLimitMinutes = 60)
        assertEquals(ApplicationDecision.BLOCK, ApplicationPolicyEngine.decision(policy, 0))
    }

    @Test fun dailyLimitBlocksWhenReached() {
        val policy = AppPolicy("com.example", "Example", dailyLimitMinutes = 60)
        assertEquals(ApplicationDecision.BLOCK, ApplicationPolicyEngine.decision(policy, 60))
    }

    @Test fun approvalIsRequiredBeforeAllowing() {
        val policy = AppPolicy("com.example", "Example", approvalRequired = true)
        assertEquals(ApplicationDecision.APPROVAL_REQUIRED, ApplicationPolicyEngine.decision(policy, 0))
    }

    @Test fun limitValidationRejectsInvalidValues() {
        assertTrue(ApplicationPolicyEngine.validateLimit(null))
        assertTrue(ApplicationPolicyEngine.validateLimit(1))
        assertTrue(ApplicationPolicyEngine.validateLimit(1440))
        assertFalse(ApplicationPolicyEngine.validateLimit(0))
        assertFalse(ApplicationPolicyEngine.validateLimit(1441))
    }
}
