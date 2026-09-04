package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ReportStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_reports", Context.MODE_PRIVATE)

    fun save(report: UsageReport) {
        val obj = JSONObject().apply {
            put("period", report.period.name)
            put("startDate", report.startDate)
            put("endDate", report.endDate)
            put("totalMinutes", report.totalMinutes)
            put("averageDailyMinutes", report.averageDailyMinutes)
            put("peakDate", report.peakDate ?: "")
            put("peakMinutes", report.peakMinutes)
            put("alertCount", report.alertCount)
            put("importantAlertCount", report.importantAlertCount)
            put("trendPercent", report.trendPercent ?: JSONObject.NULL)
            put("narrative", report.narrative)
            put("topApps", JSONArray().apply {
                report.topApps.forEach {
                    put(JSONObject().apply {
                        put("label", it.label)
                        put("packageName", it.packageName)
                        put("totalMinutes", it.totalMinutes)
                    })
                }
            })
        }
        prefs.edit().putString(report.period.name, obj.toString()).apply()
    }

    fun load(period: ReportPeriod): UsageReport? {
        val raw = prefs.getString(period.name, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val arr = o.optJSONArray("topApps") ?: JSONArray()
            val apps = buildList {
                for (i in 0 until arr.length()) {
                    val a = arr.getJSONObject(i)
                    add(ReportAppUsage(
                        a.optString("label"),
                        a.optString("packageName"),
                        a.optLong("totalMinutes")
                    ))
                }
            }
            UsageReport(
                period = ReportPeriod.valueOf(o.getString("period")),
                startDate = o.getString("startDate"),
                endDate = o.getString("endDate"),
                totalMinutes = o.getLong("totalMinutes"),
                averageDailyMinutes = o.getLong("averageDailyMinutes"),
                peakDate = o.optString("peakDate").ifBlank { null },
                peakMinutes = o.optLong("peakMinutes"),
                topApps = apps,
                alertCount = o.optInt("alertCount"),
                importantAlertCount = o.optInt("importantAlertCount"),
                trendPercent = if (o.isNull("trendPercent")) null else o.optInt("trendPercent"),
                narrative = o.optString("narrative")
            )
        }.getOrNull()
    }
}
