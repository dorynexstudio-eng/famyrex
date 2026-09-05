package com.famyrex.app

enum class FamilyRole { OWNER, ADULT, CHILD, MEMBER }

enum class DeviceLinkState { UNLINKED, PENDING, LINKED }

enum class FamyrexAppMode { PARENT, SUPERVISED }

data class FamilyProfile(
    val id: String,
    val displayName: String,
    val role: FamilyRole,
    val createdAtMs: Long,
    val guardianProfileIds: List<String> = emptyList()
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
