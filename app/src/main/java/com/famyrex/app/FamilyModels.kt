package com.famyrex.app

enum class FamilyRole { OWNER, MEMBER }

enum class DeviceLinkState { UNLINKED, PENDING, LINKED }

data class FamilyProfile(
    val id: String,
    val displayName: String,
    val role: FamilyRole,
    val createdAtMs: Long
)

data class FamilyDevice(
    val id: String,
    val displayName: String,
    val ownerProfileId: String,
    val linkState: DeviceLinkState,
    val linkedAtMs: Long? = null
)

data class PairingCode(
    val code: String,
    val createdAtMs: Long,
    val expiresAtMs: Long
)
