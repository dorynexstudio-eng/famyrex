package com.famyrex.app

enum class ReportPeriod { DAILY, WEEKLY, MONTHLY }

data class UsageReport(
    val period: ReportPeriod,
    val startDate: String,
    val endDate: String,
    val totalMinutes: Long,
    val averageDailyMinutes: Long,
    val peakDate: String?,
    val peakMinutes: Long,
    val topApps: List<ReportAppUsage>,
    val alertCount: Int,
    val importantAlertCount: Int,
    val trendPercent: Int?,
    val narrative: String
)

data class ReportAppUsage(
    val label: String,
    val packageName: String,
    val totalMinutes: Long
)
