package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class FamilyStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("famyrex_family", Context.MODE_PRIVATE)

    fun profiles(): List<FamilyProfile> {
        val raw = prefs.getString("profiles", null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(raw)
            buildList {
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    val guardians = o.optJSONArray("guardianProfileIds")?.let { array ->
                        buildList { for (j in 0 until array.length()) add(array.optString(j)) }
                    } ?: emptyList()
                    add(FamilyProfile(o.optString("id"), o.optString("displayName"),
                        runCatching { FamilyRole.valueOf(o.optString("role")) }.getOrDefault(FamilyRole.MEMBER),
                        o.optLong("createdAtMs"), guardians.filter { it.isNotBlank() }))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun devices(): List<FamilyDevice> {
        val raw = prefs.getString("devices", null) ?: return emptyList()
        return runCatching {
            val a = JSONArray(raw)
            buildList {
                for (i in 0 until a.length()) {
                    val o = a.getJSONObject(i)
                    add(FamilyDevice(o.optString("id"), o.optString("displayName"), o.optString("ownerProfileId"),
                        runCatching { DeviceLinkState.valueOf(o.optString("linkState")) }.getOrDefault(DeviceLinkState.UNLINKED),
                        o.optLong("linkedAtMs").takeIf { it > 0 }))
                }
            }
        }.getOrDefault(emptyList())
    }

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

    fun appMode(): FamyrexAppMode = runCatching { FamyrexAppMode.valueOf(prefs.getString("app_mode", FamyrexAppMode.PARENT.name)!!) }.getOrDefault(FamyrexAppMode.PARENT)

    /** Persists the verified family identity on this installation. */
    fun saveVerifiedFamilyIdentity(familyId: String, secret: String, fingerprint: String) {
        require(familyId.isNotBlank())
        require(secret.length == 32)
        require(fingerprint.length == 12)
        prefs.edit()
            .putString("verified_family_id", familyId)
            .putString("verified_family_secret", secret.lowercase())
            .putString("verified_family_fingerprint", fingerprint.lowercase())
            .putLong("verified_family_at_ms", System.currentTimeMillis())
            .apply()
    }

    fun verifiedFamilyIdentity(): VerifiedFamilyIdentity? {
        val id = prefs.getString("verified_family_id", null) ?: return null
        val secret = prefs.getString("verified_family_secret", null) ?: return null
        val fingerprint = prefs.getString("verified_family_fingerprint", null) ?: return null
        if (secret.length != 32 || fingerprint.length != 12) return null
        return VerifiedFamilyIdentity(id, secret, fingerprint, prefs.getLong("verified_family_at_ms", 0L))
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
}

data class VerifiedFamilyIdentity(val familyId: String, val secret: String, val fingerprint: String, val verifiedAtMs: Long)

private object UUIDHolder {
    fun next(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(16)
}
