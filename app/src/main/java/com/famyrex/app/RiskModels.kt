package com.famyrex.app

enum class RiskLevel {
    NORMAL, ATTENTION, ELEVATED, IMPORTANT
}

data class RiskAssessment(
    val score: Int,
    val level: RiskLevel,
    val reasons: List<String>
)
