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
        /** Parses the persisted summary without allowing one bad insight to hide valid data. */
        internal fun parse(raw: String): AiDailySummary? = runCatching {
            val o = JSONObject(raw)
            val arr = o.optJSONArray("insights")
            val insights = buildList {
                if (arr != null) for (i in 0 until arr.length()) {
                    runCatching {
                        val x = arr.getJSONObject(i)
                        val sig = x.optJSONArray("supportingSignals")
                        val signals = buildList {
                            if (sig != null) for (j in 0 until sig.length()) {
                                sig.optString(j).takeIf { it.isNotBlank() }?.let(::add)
                            }
                        }
                        add(AiInsight(
                            title = x.optString("title"),
                            summary = x.optString("summary"),
                            confidence = x.optInt("confidence").coerceIn(0, 100),
                            supportingSignals = signals
                        ))
                    }
                }
            }
            AiDailySummary(
                headline = o.optString("headline"),
                body = o.optString("body"),
                insights = insights
            )
        }.getOrNull()
    }
}
