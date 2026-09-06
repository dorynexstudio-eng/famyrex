package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

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
        return parse(raw, period)
    }

    companion object {
        internal fun parse(raw: String, expectedPeriod: ReportPeriod): UsageReport? = runCatching {
            val o = JSONObject(raw)
            val storedPeriod = ReportPeriod.valueOf(o.getString("period"))
            require(storedPeriod == expectedPeriod)

            val startDate = o.getString("startDate").also { LocalDate.parse(it) }
            val endDate = o.getString("endDate").also { LocalDate.parse(it) }
            require(endDate.compareTo(LocalDate.parse(startDate)) >= 0)

            val totalMinutes = o.getLong("totalMinutes").also { require(it >= 0L) }
            val averageDailyMinutes = o.getLong("averageDailyMinutes").also { require(it >= 0L) }
            val peakMinutes = o.getLong("peakMinutes").also { require(it >= 0L) }
            val alertCount = o.getInt("alertCount").also { require(it >= 0) }
            val importantAlertCount = o.getInt("importantAlertCount").also { require(it >= 0 && it <= alertCount) }
            val peakDate = o.optString("peakDate").ifBlank { null }?.also { LocalDate.parse(it) }
            val narrative = o.getString("narrative")
            val trendPercent = if (o.isNull("trendPercent")) null else o.getInt("trendPercent")

            val arr = o.optJSONArray("topApps") ?: JSONArray()
            val apps = buildList {
                for (i in 0 until arr.length()) {
                    runCatching {
                        val a = arr.getJSONObject(i)
                        val packageName = a.getString("packageName").trim()
                        require(packageName.isNotBlank())
                        val total = a.getLong("totalMinutes").also { require(it >= 0L) }
                        val label = a.optString("label").trim().ifBlank { packageName }
                        add(ReportAppUsage(label, packageName, total))
                    }
                }
            }

            UsageReport(
                period = storedPeriod,
                startDate = startDate,
                endDate = endDate,
                totalMinutes = totalMinutes,
                averageDailyMinutes = averageDailyMinutes,
                peakDate = peakDate,
                peakMinutes = peakMinutes,
                topApps = apps,
                alertCount = alertCount,
                importantAlertCount = importantAlertCount,
                trendPercent = trendPercent,
                narrative = narrative
            )
        }.getOrNull()
    }
}
