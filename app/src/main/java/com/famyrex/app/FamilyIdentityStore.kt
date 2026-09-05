package com.famyrex.app

import android.content.Context
import java.security.SecureRandom

/** Local family identity used to bind installations without a server. */
class FamilyIdentityStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_family_identity", Context.MODE_PRIVATE)

    fun identity(): FamilyIdentity {
        val existing = prefs.getString("family_id", null)
        val id = existing ?: randomHex(16).also { prefs.edit().putString("family_id", it).apply() }
        return FamilyIdentity(id)
    }

    fun hasIdentity(): Boolean = prefs.contains("family_id")

    private fun randomHex(bytes: Int): String {
        val data = ByteArray(bytes)
        SecureRandom().nextBytes(data)
        return data.joinToString("") { "%02x".format(it) }
    }
}

data class FamilyIdentity(val familyId: String)
