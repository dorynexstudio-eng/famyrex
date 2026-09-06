package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunicationRiskIncidentStoreTest {
    @Test
    fun malformedIncidentDoesNotDiscardValidIncidents() {
        val valid = """
            {"id":"incident-1","createdAtMs":1,"type":"BULLYING","confidence":"HIGH","score":80,"sourcePackage":"com.example","direction":"INCOMING","status":"DETECTED","reasons":[],"statusHistory":[]}
        """.trimIndent()
        val raw = "[$valid,{\"id\":\"broken\",\"type\":\"NOT_A_REAL_TYPE\"}]"

        val result = parseRiskIncidentsJson(raw)

        assertEquals(1, result.size)
        assertEquals("incident-1", result.single().id)
    }

    @Test
    fun malformedRootStillReturnsEmpty() {
        assertTrue(parseRiskIncidentsJson("not-json").isEmpty())
    }
}
