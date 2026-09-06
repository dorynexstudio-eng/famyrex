package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParentalControlStoreTest {
    @Test
    fun invalidRootFallsBackToEmptyConfig() {
        val result = parseParentalControlConfig("not-json")
        assertNull(result.screenTimeLimit)
        assertEquals(0, result.pauseSchedules.size)
        assertEquals(0, result.appRestrictions.size)
    }

    @Test
    fun malformedScheduleDoesNotDiscardValidSchedules() {
        val result = parseParentalControlConfig("""
            {
              "pauseSchedules":[
                {"startMinuteOfDay":120,"endMinuteOfDay":300,"enabled":true},
                {"startMinuteOfDay":"broken","endMinuteOfDay":500},
                {"startMinuteOfDay":600,"endMinuteOfDay":900,"enabled":false}
              ]
            }
        """.trimIndent())

        assertEquals(listOf(120, 600), result.pauseSchedules.map { it.startMinuteOfDay })
    }

    @Test
    fun malformedRestrictionDoesNotDiscardValidRestrictions() {
        val result = parseParentalControlConfig("""
            {
              "appRestrictions":[
                {"packageName":"com.valid","dailyMinutes":60,"blocked":true},
                {"packageName":"","dailyMinutes":30},
                {"packageName":"com.valid2","dailyMinutes":null,"blocked":false}
              ]
            }
        """.trimIndent())

        assertEquals(listOf("com.valid", "com.valid2"), result.appRestrictions.map { it.packageName })
    }

    @Test
    fun invalidScreenTimeDoesNotDiscardOtherValidConfiguration() {
        val result = parseParentalControlConfig("""
            {
              "screenTime":{"dailyMinutes":0,"enabled":true},
              "pauseSchedules":[{"startMinuteOfDay":60,"endMinuteOfDay":120}],
              "appRestrictions":[{"packageName":"com.valid","dailyMinutes":30}]
            }
        """.trimIndent())

        assertNull(result.screenTimeLimit)
        assertEquals(listOf(60), result.pauseSchedules.map { it.startMinuteOfDay })
        assertEquals(listOf("com.valid"), result.appRestrictions.map { it.packageName })
    }

    @Test
    fun nullDailyMinutesIsPreservedAsUnlimited() {
        val result = parseParentalControlConfig("""
            {
              "appRestrictions":[{"packageName":"com.valid","dailyMinutes":null,"blocked":false}]
            }
        """.trimIndent())

        assertNull(result.appRestrictions.single().dailyMinutes)
    }
}
