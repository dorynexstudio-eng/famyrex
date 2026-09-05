package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ParentalStatusEvaluatorTest {
    @Test
    fun `missing usage access is insufficient data`() {
        assertEquals(
            ParentalStatus.WHITE,
            ParentalStatusEvaluator.overall(true.not(), true, 30, ScreenTimeLimit(60))
        )
    }

    @Test
    fun `missing accessibility is insufficient data`() {
        assertEquals(
            ParentalStatus.WHITE,
            ParentalStatusEvaluator.overall(true, false, 30, ScreenTimeLimit(60))
        )
    }

    @Test
    fun `missing usage value is insufficient data`() {
        assertEquals(
            ParentalStatus.WHITE,
            ParentalStatusEvaluator.overall(true, true, null, ScreenTimeLimit(60))
        )
    }

    @Test
    fun `no active global limit is green when data is available`() {
        assertEquals(
            ParentalStatus.GREEN,
            ParentalStatusEvaluator.overall(true, true, 500, null)
        )
    }

    @Test
    fun `global usage at limit is red`() {
        assertEquals(
            ParentalStatus.RED,
            ParentalStatusEvaluator.overall(true, true, 60, ScreenTimeLimit(60))
        )
    }

    @Test
    fun `global usage near limit is orange`() {
        assertEquals(
            ParentalStatus.ORANGE,
            ParentalStatusEvaluator.overall(true, true, 48, ScreenTimeLimit(60))
        )
    }

    @Test
    fun `app blocked is red`() {
        assertEquals(
            ParentalStatus.RED,
            ParentalStatusEvaluator.app(true, 5, AppRestriction("com.example", blocked = true))
        )
    }

    @Test
    fun `app usage near limit is orange`() {
        assertEquals(
            ParentalStatus.ORANGE,
            ParentalStatusEvaluator.app(true, 48, AppRestriction("com.example", dailyMinutes = 60))
        )
    }

    @Test
    fun `app without restriction is green`() {
        assertEquals(
            ParentalStatus.GREEN,
            ParentalStatusEvaluator.app(true, 180, null)
        )
    }

    @Test
    fun `app without usage data is insufficient`() {
        assertEquals(
            ParentalStatus.WHITE,
            ParentalStatusEvaluator.app(false, null, AppRestriction("com.example", dailyMinutes = 60))
        )
    }
}
