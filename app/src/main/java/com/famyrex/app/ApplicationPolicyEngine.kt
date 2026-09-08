package com.famyrex.app

import java.util.Calendar

/**
 * Pure local policy evaluation. It deliberately does not assume intent or guilt:
 * it only answers whether a package is currently allowed by the configured policy.
 */
object ApplicationPolicyEngine {
    fun decision(
        policy: AppPolicy?,
        usedMinutesToday: Int,
        nowMs: Long = System.currentTimeMillis()
    ): ApplicationDecision {
        if (policy == null) return ApplicationDecision.UNMANAGED
        if (policy.blocked) return ApplicationDecision.BLOCK
        if (!policy.installAllowed && policy.source != AppInstallSource.UNKNOWN) {
            return ApplicationDecision.BLOCK
        }
        if (policy.dailyLimitMinutes != null && usedMinutesToday >= policy.dailyLimitMinutes) {
            return ApplicationDecision.BLOCK
        }
        if (policy.approvalRequired) return ApplicationDecision.APPROVAL_REQUIRED
        return ApplicationDecision.ALLOW
    }

    fun isWithinSchedule(
        startMinute: Int,
        endMinute: Int,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (startMinute !in 0..1439 || endMinute !in 0..1439) return false
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMs }
        val minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        return if (startMinute <= endMinute) {
            minute in startMinute..endMinute
        } else {
            minute >= startMinute || minute <= endMinute
        }
    }

    fun validateLimit(minutes: Int?): Boolean = minutes == null || minutes in 1..1440
}
