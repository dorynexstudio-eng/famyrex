package com.famyrex.app

import android.content.Context
import android.os.Build
import java.util.UUID

/** Bridges the existing local family model to the stable Famyrex identity model. */
object LocalIdentityResolver {
    private const val PREFS = "famyrex_identity"
    private const val KEY_DEVICE_ID = "device_id"

    fun deviceIdentity(context: Context): FamyrexDeviceIdentity? {
        val appContext = context.applicationContext
        val family = FamilyStore(appContext).verifiedFamilyIdentity()?.familyId
        val member = FamilyStore(appContext).supervisedChild()
            ?: FamilyStore(appContext).profiles().firstOrNull { it.role == FamilyRole.OWNER }
            ?: return null

        val deviceId = stableDeviceId(appContext)
        return FamyrexDeviceIdentity(
            deviceId = deviceId,
            famyrexMemberId = member.id,
            familyId = family,
            model = Build.MODEL,
            androidApi = Build.VERSION.SDK_INT,
            isSupervised = member.role == FamilyRole.CHILD
        )
    }

    private fun stableDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() } ?:
            "device-${UUID.randomUUID()}".also { id -> prefs.edit().putString(KEY_DEVICE_ID, id).apply() }
    }
}
