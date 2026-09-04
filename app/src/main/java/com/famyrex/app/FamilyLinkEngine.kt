package com.famyrex.app

object FamilyLinkEngine {
    fun validateDisplayName(name: String): Boolean =
        name.trim().length in 1..40

    fun validatePairingCode(code: String): Boolean =
        code.filter(Char::isDigit).length == 6

    fun canLink(profile: FamilyProfile, device: FamilyDevice): Boolean =
        profile.role == FamilyRole.OWNER && device.linkState != DeviceLinkState.LINKED
}
