package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Test

class CommunicationRiskIncidentValidationTest {
    @Test
    fun invalidCoreIncidentIsSkippedWithoutHidingValidIncident() {
        val raw = """
            [
              {"id":"valid","createdAtMs":1000,"type":"THREAT","confidence":"HIGH","score":80,"sourcePackage":"com.example","status":"DETECTED"},
              {"id":"","createdAtMs":1000,"type":"THREAT","confidence":"HIGH","score":80,"sourcePackage":"com.example","status":"DETECTED"}
            ]
        """.trimIndent()

        val result = parseRiskIncidentsJson(raw)

        assertEquals(listOf("valid"), result.map { it.id })
    }

    @Test
    fun invalidReasonFieldsAreSkippedButIncidentSurvives() {
        val raw = """
            [{
              "id":"incident-1",
              "createdAtMs":1000,
              "type":"THREAT",
              "confidence":"HIGH",
              "score":80,
              "status":"DETECTED",
              "reasons":[
                {"code":"OK","title":"Señal","detail":"Detalle"},
                {"code":"","title":"","detail":""}
              ]
            }]
        """.trimIndent()

        val result = parseRiskIncidentsJson(raw)

        assertEquals(1, result.size)
        assertEquals(1, result.single().reasons.size)
        assertEquals("OK", result.single().reasons.single().code)
    }

    @Test
    fun invalidHistoryEntryIsSkippedButIncidentSurvives() {
        val raw = """
            [{
              "id":"incident-1",
              "createdAtMs":1000,
              "type":"THREAT",
              "confidence":"HIGH",
              "score":80,
              "status":"DETECTED",
              "statusHistory":[
                {"status":"REVIEWED","timestampMs":2000},
                {"status":"CONFIRMED","timestampMs":0}
              ]
            }]
        """.trimIndent()

        val result = parseRiskIncidentsJson(raw)

        assertEquals(1, result.size)
        assertEquals(1, result.single().statusHistory.size)
        assertEquals(RiskIncidentStatus.REVIEWED, result.single().statusHistory.single().status)
    }
}
