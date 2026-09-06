package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HourlyUsageSnapshotStoreTest {
    @Test
    fun malformedRootReturnsEmpty() {
        assertTrue(HourlyUsageSnapshotStore.parseHistory("not-json").isEmpty())
    }

    @Test
    fun invalidSnapshotIsSkippedAndValidSnapshotSurvives() {
        val raw = """
            [
              {"hour":"2026-09-06T10:00:00","totalTimeMs":60000,"topApps":[{"packageName":"com.example","totalTimeMs":30000,"label":"Example"}]},
              {"hour":"bad","totalTimeMs":100}
            ]
        """.trimIndent()

        val result = HourlyUsageSnapshotStore.parseHistory(raw)

        assertEquals(1, result.size)
        assertEquals("2026-09-06T10:00:00", result.single().hour)
        assertEquals(60000, result.single().totalTimeMs)
        assertEquals("com.example", result.single().topApps.single().packageName)
    }

    @Test
    fun invalidAppDoesNotDiscardSnapshot() {
        val raw = """
            [{"hour":"2026-09-06T10:00:00","totalTimeMs":60000,"topApps":[
              {"packageName":"com.good","totalTimeMs":30000},
              {"packageName":"","totalTimeMs":-1}
            ]}]
        """.trimIndent()

        val result = HourlyUsageSnapshotStore.parseHistory(raw)

        assertEquals(1, result.size)
        assertEquals(1, result.single().topApps.size)
        assertEquals("com.good", result.single().topApps.single().packageName)
    }
}
