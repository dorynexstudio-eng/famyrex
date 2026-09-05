package com.famyrex.app

enum class RecommendationPriority { LOW, MEDIUM, HIGH }

enum class RecommendationAction {
    OBSERVE,
    TALK,
    REVIEW_CONTEXT,
    SUPPORT,
    ESCALATE_IF_NEEDED
}

data class FamilyRecommendation(
    val id: String,
    val title: String,
    val message: String,
    val priority: RecommendationPriority,
    val action: RecommendationAction,
    val alertId: String? = null
)
