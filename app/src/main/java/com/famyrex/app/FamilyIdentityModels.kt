package com.famyrex.app

/**
 * Stable family identity model independent from Firebase.
 * Google/Famyrex identity and device identity remain separate so the backend
 * can be introduced without coupling local protection logic to Firebase IDs.
 */
enum class FamilyMemberRole {
    ADULT,
    PROTECTED
}

enum class AccountLinkState {
    NOT_LINKED,
    GOOGLE_LINKED,
    INVITED,
    VERIFIED
}

data class FamyrexAccountIdentity(
    val famyrexMemberId: String,
    val familyId: String? = null,
    val role: FamilyMemberRole,
    val googleAccountId: String? = null,
    val googleEmail: String? = null,
    val accountLinkState: AccountLinkState = AccountLinkState.NOT_LINKED
)

data class FamyrexDeviceIdentity(
    val deviceId: String,
    val famyrexMemberId: String,
    val familyId: String? = null,
    val firebaseUid: String? = null,
    val model: String? = null,
    val androidApi: Int? = null,
    val isSupervised: Boolean = false
)

/** Firebase UID is a technical transport identity, never the person identity. */
data class TechnicalDeviceIdentity(
    val firebaseUid: String? = null,
    val deviceId: String,
    val memberId: String,
    val familyId: String? = null
)
