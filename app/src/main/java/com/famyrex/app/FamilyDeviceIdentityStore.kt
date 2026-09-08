package com.famyrex.app

import android.content.Context
import android.os.Build
import com.google.firebase.auth.FirebaseAuth

/** Resolves the supervised device identity from the local enrollment state. */
class FamilyDeviceIdentityStore(context: Context) {
    private val appContext = context.applicationContext

    fun current(): FamyrexDeviceIdentity? {
        val familyStore = FamilyStore(appContext)
        val family = familyStore.verifiedFamilyIdentity() ?: return null
        val child = familyStore.supervisedChild() ?: return null
        val device = familyStore.devices().firstOrNull { it.ownerProfileId == child.id && it.linkState == DeviceLinkState.LINKED }
            ?: return null

        return FamyrexDeviceIdentity(
            deviceId = device.id,
            famyrexMemberId = child.id,
            familyId = family.familyId,
            firebaseUid = FirebaseAuth.getInstance().currentUser?.uid,
            model = Build.MODEL,
            androidApi = Build.VERSION.SDK_INT,
            isSupervised = true
        )
    }
}
