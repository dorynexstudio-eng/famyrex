package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AiSummaryStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_ai_summary", Context.MODE_PRIVATE)

    fun save(summary: AiDailySummary) {
        val obj = JSONObject()
            .put("headline", summary.headline)
            .put("body", summary.body)
        val arr = JSONArray()
        summary.insights.forEach {
            arr.put(
                JSONObject()
                    .put("title", it.title)
                    .put("summary", it.summary)
                    .put("confidence", it.confidence)
                    .put("supportingSignals", JSONArray(it.supportingSignals))
            )
        }
        obj.put("insights", arr)
        prefs.edit().putString("latest", obj.toString()).apply()
    }

    fun load(): AiDailySummary? {
        val raw = prefs.getString("latest", null) ?: return null
        return parse(raw)
    }

    companion object {
        internal fun parse(raw: String): AiDailySummary? {
            val root = try {
                JSONObject(raw)
            } catch (_: Exception) {
                return null
            }

            val insights = mutableListOf<AiInsight>()
            val array = root.optJSONArray("insights")
            if (array != null) {
                for (i in 0 until array.length()) {
                    val insight = try {
                        val item = array.getJSONObject(i)
                        val title = item.optString("title")
                        val summary = item.optString("summary")
                        val confidenceValue = item.opt("confidence")
                        val confidence = when (confidenceValue) {
                            is Number -> confidenceValue.toInt().coerceIn(0, 100)
                            else -> throw IllegalArgumentException("invalid confidence")
                        }
                        val signals = mutableListOf<String>()
                        val signalArray = item.optJSONArray("supportingSignals")
                        if (signalArray != null) {
                            for (j in 0 until signalArray.length()) {
                                signalArray.optString(j).takeIf { it.isNotBlank() }?.let(signals::add)
                            }
                        }
                        if (title.isBlank() || summary.isBlank()) {
                            throw IllegalArgumentException("invalid insight")
                        }
                        AiInsight(title, summary, confidence, signals)
                    } catch (_: Exception) {
                        null
                    }
                    if (insight != null) insights.add(insight)
                }
            }

            return AiDailySummary(
                headline = root.optString("headline"),
                body = root.optString("body"),
                insights = insights
            )
        }
    }
}
