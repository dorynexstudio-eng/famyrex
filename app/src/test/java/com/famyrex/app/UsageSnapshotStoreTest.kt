package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageSnapshotStoreTest {
    @Test
    fun malformedRootReturnsEmptyHistory() {
        assertTrue(UsageSnapshotStore.parseHistory("not-json").isEmpty())
    }

    @Test
    fun malformedDayDoesNotDiscardValidDays() {
        val raw = """
            [
              {"date":"2026-09-06","totalTimeMs":60000,"topApps":[]},
              {"date":"2026-09-05","totalTimeMs":"broken","topApps":[]},
              {"date":"2026-09-04","totalTimeMs":120000,"topApps":[]}
            ]
        """.trimIndent()

        val result = UsageSnapshotStore.parseHistory(raw)

        assertEquals(listOf("2026-09-06", "2026-09-04"), result.map { it.date })
    }

    @Test
    fun malformedAppDoesNotDiscardValidAppsOrDay() {
        val raw = """
            [
              {
                "date":"2026-09-06",
                "totalTimeMs":180000,
                "topApps":[
                  {"packageName":"com.valid","totalTimeMs":120000,"label":"Valida"},
                  {"packageName":"com.broken","totalTimeMs":"broken","label":"Rota"},
                  {"packageName":"com.valid2","totalTimeMs":60000}
                ]
              }
            ]
        """.trimIndent()

        val result = UsageSnapshotStore.parseHistory(raw)

        assertEquals(1, result.size)
        assertEquals(listOf("com.valid", "com.valid2"), result.single().topApps.map { it.packageName })
        assertEquals("com.valid2", result.single().topApps[1].label)
    }
}
