package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FamilyDashboardStatusEvaluatorTest {
    @Test
    fun unconfiguredFamilyRemainsPending() {
        assertEquals(
            "⚪ PENDIENTE",
            FamilyDashboardStatusEvaluator.label(true.not(), ParentalStatus.WHITE, 0, 0)
        )
    }

    @Test
    fun redIntelligenceCannotAppearProtected() {
        assertEquals(
            "🔴 PROTECCIÓN EN RIESGO",
            FamilyDashboardStatusEvaluator.label(true, ParentalStatus.RED, 0, 0)
        )
    }

    @Test
    fun degradedComponentCannotAppearProtected() {
        assertEquals(
            "🔴 PROTECCIÓN EN RIESGO",
            FamilyDashboardStatusEvaluator.label(true, ParentalStatus.GREEN, 1, 0)
        )
    }

    @Test
    fun orangeIntelligenceShowsPartialProtection() {
        assertEquals(
            "🟠 PROTECCIÓN PARCIAL",
            FamilyDashboardStatusEvaluator.label(true, ParentalStatus.ORANGE, 0, 0)
        )
    }

    @Test
    fun whiteIntelligenceShowsInsufficientData() {
        assertEquals(
            "⚪ DATOS INSUFICIENTES",
            FamilyDashboardStatusEvaluator.label(true, ParentalStatus.WHITE, 0, 0)
        )
    }

    @Test
    fun missingIntelligenceNeverDefaultsToGreen() {
        assertEquals(
            "⚪ DATOS INSUFICIENTES",
            FamilyDashboardStatusEvaluator.label(true, null, 0, 0)
        )
    }

    @Test
    fun readyGreenIntelligenceCanShowProtected() {
        assertEquals(
            "🟢 PROTEGIDO",
            FamilyDashboardStatusEvaluator.label(true, ParentalStatus.GREEN, 0, 0)
        )
    }
}
