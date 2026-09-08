package com.famyrex.app

/** Actions that can be transported to a supervised device once the online layer is active. */
enum class FamilyControlAction {
    LOCK_DEVICE,
    UNLOCK_DEVICE,
    GRANT_EXTRA_TIME,
    SET_DAILY_LIMIT,
    SET_SCHEDULE,
    BLOCK_APP,
    ALLOW_APP,
    SET_APP_LIMIT,
    REQUEST_APP_APPROVAL,
    REFRESH_LOCATION,
    APPLY_WEB_POLICY,
    SYNC_POLICY
}

enum class AppInstallSource {
    GOOGLE_PLAY,
    APK,
    UNKNOWN
}

data class FamilyControlCommand(
    val commandId: String,
    val familyId: String,
    val memberId: String,
    val deviceId: String,
    val action: FamilyControlAction,
    val issuedAtMs: Long,
    val expiresAtMs: Long,
    val value: String? = null,
    val requiresAdultConfirmation: Boolean = true
) {
    fun isExpired(nowMs: Long): Boolean = nowMs >= expiresAtMs
}

data class AppPolicy(
    val packageName: String,
    val displayName: String,
    val blocked: Boolean = false,
    val dailyLimitMinutes: Int? = null,
    val approvalRequired: Boolean = false,
    val installAllowed: Boolean = true,
    val source: AppInstallSource = AppInstallSource.UNKNOWN
)

data class DevicePolicySnapshot(
    val deviceId: String,
    val dailyLimitMinutes: Int? = null,
    val bedtimeStartMinutes: Int? = null,
    val bedtimeEndMinutes: Int? = null,
    val apps: List<AppPolicy> = emptyList(),
    val webPolicyVersion: Long = 0L,
    val revision: Long = 0L
)

/** A locally generated request shown to the adult before a managed app is allowed. */
data class AppApprovalRequest(
    val requestId: String,
    val familyId: String,
    val memberId: String,
    val deviceId: String,
    val packageName: String,
    val displayName: String,
    val source: AppInstallSource,
    val createdAtMs: Long,
    val expiresAtMs: Long,
    val approved: Boolean? = null
)

/** Receipt kept locally so remote commands remain auditable and idempotent. */
data class FamilyControlReceipt(
    val commandId: String,
    val action: FamilyControlAction,
    val acceptedAtMs: Long,
    val completedAtMs: Long? = null,
    val success: Boolean,
    val reason: String? = null
)
