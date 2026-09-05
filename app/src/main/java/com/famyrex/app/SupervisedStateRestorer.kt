package com.famyrex.app

/** Restores the locally persisted supervised-mode invariant without launching UI. */
object SupervisedStateRestorer {
    fun restore(store: FamilyStore): Boolean {
        val identity = store.verifiedFamilyIdentity() ?: return false
        if (identity.familyId.isBlank() || identity.secret.length != 32 || identity.fingerprint.length != 12) return false
        val linked = store.devices().any { it.linkState == DeviceLinkState.LINKED }
        if (!linked) return false
        store.setAppMode(FamyrexAppMode.SUPERVISED)
        return true
    }
}
