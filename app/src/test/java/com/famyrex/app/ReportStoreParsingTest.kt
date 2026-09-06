package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportStoreParsingTest {
    @Test
    fun malformedRootReturnsNull() {
        assertNull(ReportStore.parse("not-json", ReportPeriod.WEEKLY))
    }

    @Test
    fun validReportRoundTripsThroughParser() {
        val raw = """
            {
              "period":"WEEKLY",
              "startDate":"2026-08-31",
              "endDate":"2026-09-06",
              "totalMinutes":840,
              "averageDailyMinutes":120,
              "peakDate":"2026-09-05",
              "peakMinutes":180,
              "alertCount":3,
              "importantAlertCount":1,
              "trendPercent":12,
              "narrative":"Resumen semanal",
              "topApps":[
                {"label":"YouTube","packageName":"com.youtube","totalMinutes":300}
              ]
            }
        """.trimIndent()

        val result = ReportStore.parse(raw, ReportPeriod.WEEKLY)

        assertEquals(840L, result?.totalMinutes)
        assertEquals(1, result?.topApps?.size)
        assertEquals("com.youtube", result?.topApps?.single()?.packageName)
    }

    @Test
    fun malformedAppDoesNotDiscardValidReportOrApps() {
        val raw = """
            {
              "period":"DAILY",
              "startDate":"2026-09-06",
              "endDate":"2026-09-06",
              "totalMinutes":60,
              "averageDailyMinutes":60,
              "peakDate":"",
              "peakMinutes":60,
              "alertCount":0,
              "importantAlertCount":0,
              "trendPercent":null,
              "narrative":"Resumen",
              "topApps":[
                {"packageName":"com.valid","totalMinutes":30,"label":"Valida"},
                {"packageName":"com.broken","totalMinutes":"broken","label":"Rota"},
                {"packageName":"com.valid2","totalMinutes":20}
              ]
            }
        """.trimIndent()

        val result = ReportStore.parse(raw, ReportPeriod.DAILY)

        assertEquals(listOf("com.valid", "com.valid2"), result?.topApps?.map { it.packageName })
        assertEquals("com.valid2", result?.topApps?.get(1)?.label)
    }

    @Test
    fun invalidCoreValuesReturnNull() {
        val raw = """
            {
              "period":"WEEKLY",
              "startDate":"2026-08-31",
              "endDate":"2026-09-06",
              "totalMinutes":-1,
              "averageDailyMinutes":120,
              "peakDate":"",
              "peakMinutes":180,
              "alertCount":3,
              "importantAlertCount":1,
              "trendPercent":null,
              "narrative":"Resumen",
              "topApps":[]
            }
        """.trimIndent()

        assertNull(ReportStore.parse(raw, ReportPeriod.WEEKLY))
    }

    @Test
    fun periodMismatchReturnsNull() {
        val raw = """
            {
              "period":"DAILY",
              "startDate":"2026-09-06",
              "endDate":"2026-09-06",
              "totalMinutes":60,
              "averageDailyMinutes":60,
              "peakDate":"",
              "peakMinutes":60,
              "alertCount":0,
              "importantAlertCount":0,
              "trendPercent":null,
              "narrative":"Resumen",
              "topApps":[]
            }
        """.trimIndent()

        assertNull(ReportStore.parse(raw, ReportPeriod.WEEKLY))
    }
}
