package com.famyrex.app

/** Restores the locally persisted supervised-mode invariant without launching UI. */
object SupervisedStateRestorer {
    fun isRestorable(identity: VerifiedFamilyIdentity?, linkedDevice: Boolean): Boolean =
        identity != null &&
            identity.familyId.isNotBlank() &&
            identity.secret.length == 32 &&
            identity.fingerprint.length == 12 &&
            linkedDevice

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
