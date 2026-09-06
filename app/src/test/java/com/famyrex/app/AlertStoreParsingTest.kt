package com.famyrex.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertStoreParsingTest {
    private fun validAlert(id: String, lifecycle: AlertLifecycleStatus = AlertLifecycleStatus.DETECTED): JSONObject =
        JSONObject().apply {
            put("id", id)
            put("type", AlertType.APP_SPIKE.name)
            put("severity", AlertSeverity.ATTENTION.name)
            put("title", "Aviso")
            put("message", "Prueba")
            put("date", "2026-09-06")
            put("packageName", "com.example.app")
            put("lifecycleStatus", lifecycle.name)
        }

    @Test
    fun malformedRecordDoesNotDiscardValidAlerts() {
        val array = JSONArray()
            .put(validAlert("valid-1", AlertLifecycleStatus.DISMISSED))
            .put(JSONObject().put("id", "broken").put("type", "NOT_A_REAL_TYPE"))
            .put(validAlert("valid-2", AlertLifecycleStatus.RESOLVED))

        val result = parseAlertsJson(array.toString())

        assertEquals(listOf("valid-1", "valid-2"), result.map { it.id })
        assertEquals(AlertLifecycleStatus.DISMISSED, result[0].lifecycleStatus)
        assertEquals(AlertLifecycleStatus.RESOLVED, result[1].lifecycleStatus)
    }

    @Test
    fun malformedRootReturnsEmptyList() {
        assertTrue(parseAlertsJson("not-json").isEmpty())
    }

    @Test
    fun invalidLifecycleFallsBackWithoutDiscardingAlert() {
        val alert = validAlert("valid").put("lifecycleStatus", "UNKNOWN_STATUS")

        val result = parseAlertsJson(JSONArray().put(alert).toString())

        assertEquals(1, result.size)
        assertEquals("valid", result.single().id)
        assertEquals(AlertLifecycleStatus.DETECTED, result.single().lifecycleStatus)
    }
}
