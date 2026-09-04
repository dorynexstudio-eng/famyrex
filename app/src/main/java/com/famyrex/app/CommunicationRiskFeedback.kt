package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class RiskFeedback { RELEVANT, FALSE_POSITIVE, UNSURE }

data class CommunicationRiskFeedbackEntry(
    val incidentId: String,
    val feedback: RiskFeedback,
    val createdAtMs: Long = System.currentTimeMillis()
)

class CommunicationRiskFeedbackStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_risk_feedback", Context.MODE_PRIVATE)

    fun save(entry: CommunicationRiskFeedbackEntry) {
        val current = load().filterNot { it.incidentId == entry.incidentId }
        val array = JSONArray()
        (listOf(entry) + current).take(200).forEach {
            array.put(JSONObject().apply {
                put("incidentId", it.incidentId)
                put("feedback", it.feedback.name)
                put("createdAtMs", it.createdAtMs)
            })
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    fun load(): List<CommunicationRiskFeedbackEntry> {
        val raw = prefs.getString("items", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        CommunicationRiskFeedbackEntry(
                            incidentId = o.getString("incidentId"),
                            feedback = RiskFeedback.valueOf(o.getString("feedback")),
                            createdAtMs = o.optLong("createdAtMs", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
