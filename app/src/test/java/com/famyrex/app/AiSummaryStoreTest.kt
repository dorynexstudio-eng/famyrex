package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AiSummaryStoreTest {
    @Test
    fun malformedRootReturnsNull() {
        assertNull(AiSummaryStore.parse("not-json"))
    }

    @Test
    fun malformedInsightDoesNotDiscardValidInsights() {
        val raw = """
            {
              "headline":"Resumen",
              "body":"Actividad estable",
              "insights":[
                {"title":"Válida","summary":"Señal normal","confidence":80,"supportingSignals":["uso estable"]},
                {"title":"Rota","summary":{},"confidence":"broken"},
                {"title":"Válida 2","summary":"Otra señal","confidence":120,"supportingSignals":[]}
              ]
            }
        """.trimIndent()

        val result = AiSummaryStore.parse(raw)

        assertNotNull(result)
        assertEquals(listOf("Válida", "Válida 2"), result!!.insights.map { it.title })
        assertEquals(100, result.insights[1].confidence)
    }

    @Test
    fun missingInsightsStillLoadsSummary() {
        val result = AiSummaryStore.parse("""{"headline":"Resumen","body":"Sin señales"}""")

        assertNotNull(result)
        assertEquals("Resumen", result!!.headline)
        assertEquals(0, result.insights.size)
    }
}
