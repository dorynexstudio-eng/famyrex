package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyIntelligenceModelsTest {
    @Test
    fun missingDataRemainsWhiteAndRequiresAction() {
        val summary = FamilyIntelligenceAggregator.summarize(
            parentalStatus = ParentalStatus.WHITE,
            totalScreenMinutes = null,
            communicationAlertCount = 0,
            usageAccess = false,
            accessibilityEnabled = false
        )

        assertEquals(ParentalStatus.WHITE, summary.parentalStatus)
        assertFalse(summary.protectionReady)
        assertTrue(summary.actionRequired)
        assertEquals(3, summary.reasons.size)
    }

    @Test
    fun missingUsageAccessRequiresActionEvenIfStatusWasNotWhite() {
        val summary = FamilyIntelligenceAggregator.summarize(
            parentalStatus = ParentalStatus.GREEN,
            totalScreenMinutes = null,
            communicationAlertCount = 0,
            usageAccess = false,
            accessibilityEnabled = true
        )

        assertFalse(summary.protectionReady)
        assertTrue(summary.actionRequired)
        assertTrue(summary.reasons.any { it.contains("datos de uso") })
    }

    @Test
    fun missingAccessibilityRequiresAction() {
        val summary = FamilyIntelligenceAggregator.summarize(
            parentalStatus = ParentalStatus.GREEN,
            totalScreenMinutes = 45,
            communicationAlertCount = 0,
            usageAccess = true,
            accessibilityEnabled = false
        )

        assertFalse(summary.protectionReady)
        assertTrue(summary.actionRequired)
        assertTrue(summary.reasons.any { it.contains("guardia parental") })
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
    fun greenWithoutAlertsAndWithProtectionReadyNeedsNoAction() {
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
