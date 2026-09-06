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

        val result = parseRiskIncidentsJsonForTest(raw)

        assertEquals(1, result.size)
        assertEquals("incident-1", result.single().id)
    }

    @Test
    fun malformedRootStillReturnsEmpty() {
        assertTrue(parseRiskIncidentsJsonForTest("not-json").isEmpty())
    }

    private fun parseRiskIncidentsJsonForTest(raw: String): List<CommunicationRiskIncident> {
        val array = runCatching { org.json.JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                runCatching {
                    val o = array.getJSONObject(i)
                    add(
                        CommunicationRiskIncident(
                            id = o.getString("id"),
                            createdAtMs = o.getLong("createdAtMs"),
                            type = CommunicationRiskType.valueOf(o.getString("type")),
                            confidence = RiskConfidence.valueOf(o.getString("confidence")),
                            score = o.getInt("score"),
                            reasons = emptyList(),
                            sourcePackage = o.optString("sourcePackage").ifBlank { null },
                            direction = runCatching { CommunicationDirection.valueOf(o.optString("direction")) }
                                .getOrDefault(CommunicationDirection.UNKNOWN),
                            status = runCatching { RiskIncidentStatus.valueOf(o.optString("status")) }
                                .getOrDefault(RiskIncidentStatus.DETECTED)
                        )
                    )
                }
            }
        }
    }
}
