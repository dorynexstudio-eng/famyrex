package com.famyrex.app

/** Restores the locally persisted supervised-mode invariant without launching UI. */
object SupervisedStateRestorer {
    fun isRestorable(identity: VerifiedFamilyIdentity?, linkedDevice: Boolean): Boolean {
        if (identity == null || !linkedDevice) return false
        if (identity.familyId.isBlank() || identity.verifiedAtMs <= 0L) return false
        if (identity.secret.length != 32 || !identity.secret.matches(Regex("[0-9a-fA-F]{32}"))) return false
        if (identity.fingerprint.length != 12 || !identity.fingerprint.matches(Regex("[0-9a-fA-F]{12}"))) return false
        return OfflinePairingTokenCodec.fingerprint(identity.secret) == identity.fingerprint.lowercase()
    }

    fun restore(store: FamilyStore): Boolean {
        val identity = store.verifiedFamilyIdentity()
        val linked = store.devices().any { it.linkState == DeviceLinkState.LINKED }
        if (!isRestorable(identity, linked)) {
            store.clearVerifiedFamilyIdentity()
            return false
        }
        store.setAppMode(FamyrexAppMode.SUPERVISED)
        return true
    }
}
