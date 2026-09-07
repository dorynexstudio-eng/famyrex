package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FamilyStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("famyrex_family", Context.MODE_PRIVATE)
    private val secretProtector = FamilySecretProtector(appContext)

    fun profiles(): List<FamilyProfile> = parseProfiles(prefs.getString("profiles", null))

    fun devices(): List<FamilyDevice> = parseDevices(prefs.getString("devices", null))

    fun ensureLocalOwner(): FamilyProfile {
        val current = profiles().firstOrNull { it.role == FamilyRole.OWNER }
        if (current != null) return current
        val profile = FamilyProfile("profile-${UUIDHolder.next()}", "Administrador", FamilyRole.OWNER, System.currentTimeMillis())
        saveProfiles(profiles() + profile)
        return profile
    }

    fun addAdult(displayName: String): FamilyProfile {
        val profile = FamilyProfile("profile-${UUIDHolder.next()}", displayName.ifBlank { "Adulto autorizado" }, FamilyRole.ADULT, System.currentTimeMillis())
        saveProfiles(profiles() + profile)
        return profile
    }

    fun addChild(displayName: String, guardianProfileIds: List<String>): FamilyProfile {
        val profile = FamilyProfile("profile-${UUIDHolder.next()}", displayName.ifBlank { "Perfil infantil" }, FamilyRole.CHILD, System.currentTimeMillis(), guardianProfileIds.distinct())
        saveProfiles(profiles() + profile)
        return profile
    }

    /** Creates/updates the local representation of the exact child selected during pairing. */
    fun ensureSupervisedChild(profileId: String, displayName: String): FamilyProfile {
        require(profileId.isNotBlank())
        val existing = profiles().firstOrNull { it.id == profileId }
        if (existing != null) {
            require(existing.role == FamilyRole.CHILD)
            prefs.edit().putString("supervised_child_profile_id", profileId).apply()
            return existing
        }
        val profile = FamilyProfile(profileId, displayName.ifBlank { "Perfil infantil" }, FamilyRole.CHILD, System.currentTimeMillis())
        saveProfiles(profiles().filterNot { it.role == FamilyRole.CHILD } + profile)
        prefs.edit().putString("supervised_child_profile_id", profileId).apply()
        return profile
    }

    /** Returns the child identity explicitly assigned to this supervised installation. */
    fun supervisedChildProfileId(): String? =
        prefs.getString("supervised_child_profile_id", null)?.trim()?.takeIf { it.isNotBlank() }

    fun supervisedChild(): FamilyProfile? =
        supervisedChildProfileId()?.let { id -> profiles().firstOrNull { it.id == id && it.role == FamilyRole.CHILD } }

    fun addDevice(displayName: String, ownerProfileId: String): FamilyDevice {
        val device = FamilyDevice("device-${UUIDHolder.next()}", displayName.ifBlank { "Dispositivo familiar" }, ownerProfileId, DeviceLinkState.PENDING)
        saveDevices(devices() + device)
        return device
    }

    fun setDeviceState(deviceId: String, state: DeviceLinkState) {
        saveDevices(devices().map { if (it.id == deviceId) it.copy(linkState = state, linkedAtMs = if (state == DeviceLinkState.LINKED) System.currentTimeMillis() else it.linkedAtMs) else it })
    }

    fun setAppMode(mode: FamyrexAppMode) {
        prefs.edit().putString("app_mode", mode.name).apply()
    }

    fun appMode(): FamyrexAppMode = parseAppMode(prefs.getString("app_mode", FamyrexAppMode.PARENT.name))

    /** Persists the verified family identity; the binding secret is encrypted with Android Keystore. */
    fun saveVerifiedFamilyIdentity(familyId: String, secret: String, fingerprint: String) {
        require(familyId.isNotBlank())
        require(secret.length == 32)
        require(fingerprint.length == 12)
        prefs.edit()
            .putString("verified_family_id", familyId)
            .putString("verified_family_secret_enc", secretProtector.encrypt(secret.lowercase()))
            .remove("verified_family_secret")
            .putString("verified_family_fingerprint", fingerprint.lowercase())
            .putLong("verified_family_at_ms", System.currentTimeMillis())
            .apply()
    }

    fun verifiedFamilyIdentity(): VerifiedFamilyIdentity? {
        val id = prefs.getString("verified_family_id", null) ?: return null
        val fingerprint = prefs.getString("verified_family_fingerprint", null) ?: return null
        val encrypted = prefs.getString("verified_family_secret_enc", null)
        val secret = encrypted?.let { secretProtector.decrypt(it) }
            ?: prefs.getString("verified_family_secret", null)?.also {
                runCatching {
                    prefs.edit()
                        .putString("verified_family_secret_enc", secretProtector.encrypt(it.lowercase()))
                        .remove("verified_family_secret")
                        .apply()
                }
            }
            ?: return null
        if (secret.length != 32 || fingerprint.length != 12) return null
        return VerifiedFamilyIdentity(id, secret, fingerprint, prefs.getLong("verified_family_at_ms", 0L))
    }

    /** Removes stale supervised binding data and returns this installation to parent mode. */
    fun clearVerifiedFamilyIdentity() {
        prefs.edit()
            .remove("verified_family_id")
            .remove("verified_family_secret")
            .remove("verified_family_secret_enc")
            .remove("verified_family_fingerprint")
            .remove("verified_family_at_ms")
            .remove("supervised_child_profile_id")
            .putString("app_mode", FamyrexAppMode.PARENT.name)
            .apply()
    }

    private fun saveProfiles(items: List<FamilyProfile>) {
        val a = JSONArray()
        items.forEach { a.put(JSONObject().apply { put("id", it.id); put("displayName", it.displayName); put("role", it.role.name); put("createdAtMs", it.createdAtMs); put("guardianProfileIds", JSONArray(it.guardianProfileIds)) }) }
        prefs.edit().putString("profiles", a.toString()).apply()
        syncDashboardFamily()
    }

    private fun saveDevices(items: List<FamilyDevice>) {
        val a = JSONArray()
        items.forEach { a.put(JSONObject().apply { put("id", it.id); put("displayName", it.displayName); put("ownerProfileId", it.ownerProfileId); put("linkState", it.linkState.name); put("linkedAtMs", it.linkedAtMs ?: 0L) }) }
        prefs.edit().putString("devices", a.toString()).apply()
        syncDashboardFamily()
    }

    private fun syncDashboardFamily() {
        val profiles = profiles()
        val owner = profiles.firstOrNull { it.role == FamilyRole.OWNER }
        val child = profiles.firstOrNull { it.role == FamilyRole.CHILD }
        val dashboardPrefs = appContext.getSharedPreferences("famyrex_prefs", Context.MODE_PRIVATE)
        val existingCode = dashboardPrefs.getString("link_code", "") ?: ""
        dashboardPrefs.edit().putString("parent_name", owner?.displayName.orEmpty()).putString("child_name", child?.displayName.orEmpty()).putString("link_code", existingCode).apply()
    }

    companion object {
        internal fun parseProfiles(raw: String?): List<FamilyProfile> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        runCatching {
                            val o = array.getJSONObject(i)
                            val id = o.getString("id").trim()
                            val displayName = o.getString("displayName").trim()
                            val role = FamilyRole.valueOf(o.getString("role"))
                            val createdAtMs = o.getLong("createdAtMs")
                            require(id.isNotBlank() && displayName.isNotBlank() && createdAtMs > 0L)
                            val guardians = o.optJSONArray("guardianProfileIds")?.let { ids ->
                                buildList { for (j in 0 until ids.length()) ids.optString(j).trim().takeIf { it.isNotBlank() }?.let(::add) }
                            } ?: emptyList()
                            add(FamilyProfile(id, displayName, role, createdAtMs, guardians.distinct()))
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }

        internal fun parseDevices(raw: String?): List<FamilyDevice> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (i in 0 until array.length()) {
                        runCatching {
                            val o = array.getJSONObject(i)
                            val id = o.getString("id").trim()
                            val displayName = o.getString("displayName").trim()
                            val ownerProfileId = o.getString("ownerProfileId").trim()
                            val linkState = DeviceLinkState.valueOf(o.getString("linkState"))
                            val linkedAtMs = o.optLong("linkedAtMs", 0L).takeIf { it > 0L }
                            require(id.isNotBlank() && displayName.isNotBlank() && ownerProfileId.isNotBlank())
                            add(FamilyDevice(id, displayName, ownerProfileId, linkState, linkedAtMs))
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }

        internal fun parseAppMode(raw: String?): FamyrexAppMode =
            runCatching { FamyrexAppMode.valueOf(raw ?: FamyrexAppMode.PARENT.name) }.getOrDefault(FamyrexAppMode.PARENT)
    }
}

data class VerifiedFamilyIdentity(val familyId: String, val secret: String, val fingerprint: String, val verifiedAtMs: Long)

private object UUIDHolder {
    fun next(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
}
