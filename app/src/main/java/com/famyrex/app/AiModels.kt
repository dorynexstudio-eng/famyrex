package com.famyrex.app

data class AiInsight(
    val title: String,
    val summary: String,
    val confidence: Int,
    val supportingSignals: List<String>
)

data class AiDailySummary(
    val headline: String,
    val body: String,
    val insights: List<AiInsight>
)
