package com.famyrex.app

enum class WellbeingGoal {
    REDUCE_SCREEN_TIME,
    BETTER_NIGHT_ROUTINE,
    MORE_BREAKS,
    BALANCED_USE
}

data class WellbeingSettings(
    val dailyGoalMinutes: Long = 180,
    val breakAfterMinutes: Long = 60,
    val breakDurationMinutes: Long = 10,
    val nightStartMinutes: Int = 0,
    val nightEndMinutes: Int = 360,
    val goal: WellbeingGoal = WellbeingGoal.BALANCED_USE
)

data class WellbeingAssessment(
    val todayMinutes: Long,
    val goalMinutes: Long,
    val goalProgress: Int,
    val breakCount: Int,
    val nightMinutes: Long,
    val recommendation: String
)
