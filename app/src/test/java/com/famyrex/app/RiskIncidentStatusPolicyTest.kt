package com.famyrex.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskIncidentStatusPolicyTest {
    @Test
    fun detectedAllowsReviewOrDismissalOnly() {
        assertTrue(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.DETECTED, RiskIncidentStatus.REVIEWED))
        assertTrue(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.DETECTED, RiskIncidentStatus.DISMISSED))
        assertTrue(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.DETECTED, RiskIncidentStatus.AUTO_DISMISSED))
        assertFalse(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.DETECTED, RiskIncidentStatus.CONFIRMED))
        assertFalse(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.DETECTED, RiskIncidentStatus.RESOLVED))
    }

    @Test
    fun reviewedMustBeConfirmedOrDismissed() {
        assertTrue(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.REVIEWED, RiskIncidentStatus.CONFIRMED))
        assertTrue(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.REVIEWED, RiskIncidentStatus.DISMISSED))
        assertFalse(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.REVIEWED, RiskIncidentStatus.RESOLVED))
        assertFalse(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.REVIEWED, RiskIncidentStatus.DETECTED))
    }

    @Test
    fun confirmedCanOnlyBeResolved() {
        assertTrue(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.CONFIRMED, RiskIncidentStatus.RESOLVED))
        assertFalse(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.CONFIRMED, RiskIncidentStatus.DETECTED))
        assertFalse(RiskIncidentStatusPolicy.canTransition(RiskIncidentStatus.CONFIRMED, RiskIncidentStatus.DISMISSED))
    }

    @Test
    fun terminalStatesCannotBeReopenedOrChanged() {
        for (terminal in listOf(
            RiskIncidentStatus.DISMISSED,
            RiskIncidentStatus.AUTO_DISMISSED,
            RiskIncidentStatus.RESOLVED
        )) {
            for (next in RiskIncidentStatus.entries) {
                assertFalse(RiskIncidentStatusPolicy.canTransition(terminal, next))
            }
        }
    }

    @Test
    fun everyStateRejectsSelfTransitionAtPolicyLevel() {
        for (status in RiskIncidentStatus.entries) {
            assertFalse(RiskIncidentStatusPolicy.canTransition(status, status))
        }
    }
}
