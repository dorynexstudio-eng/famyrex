package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyIntelligenceModelsTest {
    @Test
    fun missingDataRemainsWhiteAndExplainsWhy() {
        val summary = FamilyIntelligenceAggregator.summarize(
            parentalStatus = ParentalStatus.WHITE,
            totalScreenMinutes = null,
            communicationAlertCount = 0,
            usageAccess = false,
            accessibilityEnabled = false
        )

        assertEquals(ParentalStatus.WHITE, summary.parentalStatus)
        assertFalse(summary.protectionReady)
        assertFalse(summary.actionRequired)
        assertEquals(3, summary.reasons.size)
    }

    @Test
    fun redProtectionRequiresAction() {
        val summary = FamilyIntelligenceAggregator.summarize(
            parentalStatus = ParentalStatus.RED,
            totalScreenMinutes = 180,
            communicationAlertCount = 0,
            usageAccess = true,
            accessibilityEnabled = true
        )

        assertTrue(summary.actionRequired)
        assertTrue(summary.protectionReady)
        assertEquals(1, summary.reasons.size)
    }

    @Test
    fun communicationAlertsRequireReviewEvenWhenProtectionIsGreen() {
        val summary = FamilyIntelligenceAggregator.summarize(
            parentalStatus = ParentalStatus.GREEN,
            totalScreenMinutes = 60,
            communicationAlertCount = 2,
            usageAccess = true,
            accessibilityEnabled = true
        )

        assertTrue(summary.actionRequired)
        assertEquals(1, summary.reasons.size)
    }

    @Test
    fun greenWithoutAlertsNeedsNoAction() {
        val summary = FamilyIntelligenceAggregator.summarize(
            parentalStatus = ParentalStatus.GREEN,
            totalScreenMinutes = 45,
            communicationAlertCount = 0,
            usageAccess = true,
            accessibilityEnabled = true
        )

        assertFalse(summary.actionRequired)
        assertTrue(summary.protectionReady)
        assertTrue(summary.reasons.isEmpty())
    }
}
