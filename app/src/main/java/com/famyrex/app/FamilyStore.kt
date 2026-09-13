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

    fun setOwnerDisplayName(displayName: String): FamilyProfile {
        val owner = ensureLocalOwner()
        val updated = owner.copy(displayName = displayName.ifBlank { owner.displayName })
        saveProfiles(profiles().map { if (it.id == owner.id) updated else it })
        return updated
    }

    fun addAdult(displayName: String): FamilyProfile {
        val profile = FamilyProfile("profile-${UUIDHolder.next()}", displayName.ifBlank { "Adulto autorizado" }, FamilyRole.ADULT, System.currentTimeMillis())
        saveProfiles(profiles() + profile)
        return profile
    }

    fun addChild(displayName: String, guardianProfileIds: List<String>): FamilyProfile {
        return addChildWithId("profile-${UUIDHolder.next()}", displayName, guardianProfileIds)
    }

    /** Adds the local mirror of a child profile created by the trusted cloud API. */
    fun addChildWithId(profileId: String, displayName: String, guardianProfileIds: List<String>): FamilyProfile {
        require(profileId.isNotBlank())
        val existing = profiles().firstOrNull { it.id == profileId }
        if (existing != null) {
            require(existing.role == FamilyRole.CHILD)
            return existing
        }
        val profile = FamilyProfile(profileId, displayName.ifBlank { "Perfil infantil" }, FamilyRole.CHILD, System.currentTimeMillis(), guardianProfileIds.distinct())
        saveProfiles(profiles() + profile)
        return profile
    }

    /**
     * Rebuilds only the cloud-family mirror. Server identifiers are retained and any
     * previous/legacy local mirror entries are removed; supervised secrets and other prefs remain untouched.
     */
    fun applyCloudFamilySnapshot(snapshot: FamilySnapshot) {
        val previousProfileIds = parseStringSet(prefs.getString(KEY_CLOUD_PROFILE_IDS, null))
        val previousDeviceIds = parseStringSet(prefs.getString(KEY_CLOUD_DEVICE_IDS, null))
        val currentProfiles = profiles().filterNot { it.id in previousProfileIds || it.id.startsWith("profile-") }.toMutableList()
        val currentDevices = devices().filterNot { it.id in previousDeviceIds || it.id.startsWith("device-") }.toMutableList()

        val adultProfiles = snapshot.adults.map { adult ->
            FamilyProfile(
                id = adult.uid,
                displayName = adult.displayName,
                role = if (adult.uid == snapshot.ownerUid) FamilyRole.OWNER else FamilyRole.ADULT,
                createdAtMs = adult.createdAtMs.takeIf { it > 0L } ?: System.currentTimeMillis()
            )
        }
        val parentIds = adultProfiles.map { it.id }
        val childProfiles = snapshot.children.map { child ->
            FamilyProfile(
                id = child.memberId,
                displayName = child.displayName,
                role = FamilyRole.CHILD,
                createdAtMs = child.createdAtMs.takeIf { it > 0L } ?: System.currentTimeMillis(),
                guardianProfileIds = parentIds
            )
        }
        val mirroredProfiles = (currentProfiles + adultProfiles + childProfiles).distinctBy { it.id }
        val mirroredDevices = snapshot.devices.mapNotNull { device ->
            val memberId = device.famyrexMemberId ?: return@mapNotNull null
            val deviceId = device.famyrexDeviceId ?: device.uid
            if (deviceId.isBlank()) return@mapNotNull null
            FamilyDevice(
                id = deviceId,
                displayName = "Dispositivo infantil",
                ownerProfileId = memberId,
                linkState = if (device.linkedAtMs > 0L) DeviceLinkState.LINKED else DeviceLinkState.PENDING,
                linkedAtMs = device.linkedAtMs.takeIf { it > 0L }
            )
        }
        val allDevices = (currentDevices + mirroredDevices).distinctBy { it.id }

        prefs.edit()
            .putString(KEY_CLOUD_PROFILE_IDS, JSONArray(adultProfiles.map { it.id } + childProfiles.map { it.id }).toString())
            .putString(KEY_CLOUD_DEVICE_IDS, JSONArray(mirroredDevices.map { it.id }).toString())
            .apply()
        saveProfiles(mirroredProfiles)
        saveDevices(allDevices)
    }

    fun ensureSupervisedChild(profileId: String, displayName: String): FamilyProfile {
        require(profileId.isNotBlank())
        val existing = profiles().firstOrNull { it.id == profileId }
        if (existing != null) {
            require(existing.role == FamilyRole.CHILD)
            prefs.edit().putString("supervised_child_profile_id", profileId).apply()
            return existing
        }
        val profile = FamilyProfile(profileId, displayName.ifBlank { "Perfil infantil" }, FamilyRole.CHILD, System.currentTimeMillis())
        saveProfiles(profiles() + profile)
        prefs.edit().putString("supervised_child_profile_id", profileId).apply()
        return profile
    }

    fun supervisedChildProfileId(): String? = prefs.getString("supervised_child_profile_id", null)?.trim()?.takeIf { it.isNotBlank() }
    fun supervisedChild(): FamilyProfile? = supervisedChildProfileId()?.let { id -> profiles().firstOrNull { it.id == id && it.role == FamilyRole.CHILD } }

    fun addDevice(displayName: String, ownerProfileId: String): FamilyDevice {
        return addDeviceWithId("device-${UUIDHolder.next()}", displayName, ownerProfileId)
    }

    fun addDeviceWithId(deviceId: String, displayName: String, ownerProfileId: String): FamilyDevice {
        require(deviceId.isNotBlank())
        require(ownerProfileId.isNotBlank())
        val existing = devices().firstOrNull { it.id == deviceId }
        if (existing != null) {
            require(existing.ownerProfileId == ownerProfileId)
            return existing
        }
        val device = FamilyDevice(deviceId, displayName.ifBlank { "Dispositivo familiar" }, ownerProfileId, DeviceLinkState.PENDING)
        saveDevices(devices() + device)
        return device
    }

    fun setDeviceState(deviceId: String, state: DeviceLinkState) {
        saveDevices(devices().map { if (it.id == deviceId) it.copy(linkState = state, linkedAtMs = if (state == DeviceLinkState.LINKED) System.currentTimeMillis() else it.linkedAtMs) else it })
    }

    fun setAppMode(mode: FamyrexAppMode) { prefs.edit().putString("app_mode", mode.name).apply() }
    fun appMode(): FamyrexAppMode = parseAppMode(prefs.getString("app_mode", FamyrexAppMode.PARENT.name))

    fun saveVerifiedFamilyIdentity(familyId: String, secret: String, fingerprint: String) {
        require(familyId.isNotBlank()); require(secret.length == 32); require(fingerprint.length == 12)
        prefs.edit().putString("verified_family_id", familyId).putString("verified_family_secret_enc", secretProtector.encrypt(secret.lowercase())).remove("verified_family_secret").putString("verified_family_fingerprint", fingerprint.lowercase()).putLong("verified_family_at_ms", System.currentTimeMillis()).apply()
    }

    fun verifiedFamilyIdentity(): VerifiedFamilyIdentity? {
        val id = prefs.getString("verified_family_id", null) ?: return null
        val fingerprint = prefs.getString("verified_family_fingerprint", null) ?: return null
        val encrypted = prefs.getString("verified_family_secret_enc", null)
        val secret = encrypted?.let { secretProtector.decrypt(it) }
            ?: prefs.getString("verified_family_secret", null)?.also {
                runCatching { prefs.edit().putString("verified_family_secret_enc", secretProtector.encrypt(it.lowercase())).remove("verified_family_secret").apply() }
            } ?: return null
        if (secret.length != 32 || fingerprint.length != 12) return null
        return VerifiedFamilyIdentity(id, secret, fingerprint, prefs.getLong("verified_family_at_ms", 0L))
    }

    fun clearVerifiedFamilyIdentity() {
        // This is a security-critical boundary: wait until the identity/mode reset is
        // durably written before returning, so a process death cannot leave the old
        // verified enrollment temporarily restorable.
        val cloudProfileIds = parseStringSet(prefs.getString(KEY_CLOUD_PROFILE_IDS, null))
        val cloudDeviceIds = parseStringSet(prefs.getString(KEY_CLOUD_DEVICE_IDS, null))
        val remainingProfiles = profiles().filterNot { it.id in cloudProfileIds }
        val remainingDevices = devices().filterNot { it.id in cloudDeviceIds }
        prefs.edit()
            .remove("verified_family_id")
            .remove("verified_family_secret")
            .remove("verified_family_secret_enc")
            .remove("verified_family_fingerprint")
            .remove("verified_family_at_ms")
            .remove("supervised_child_profile_id")
            .remove(KEY_CLOUD_PROFILE_IDS)
            .remove(KEY_CLOUD_DEVICE_IDS)
            .putString("profiles", profilesJson(remainingProfiles).toString())
            .putString("devices", devicesJson(remainingDevices).toString())
            .putString("app_mode", FamyrexAppMode.PARENT.name)
            .commit()
        syncDashboardFamily()
        ExtraTimeAllowanceStore(appContext).clear()
        AppApprovalStore(appContext).clear()
        AccessibilityConsentStore(appContext).clear()
        DeviceEmergencyLockStore(appContext).clear()
        FamilyCommandGate(appContext).clear()
    }

    private fun saveProfiles(items: List<FamilyProfile>) {
        prefs.edit().putString("profiles", profilesJson(items).toString()).apply(); syncDashboardFamily()
    }

    private fun saveDevices(items: List<FamilyDevice>) {
        prefs.edit().putString("devices", devicesJson(items).toString()).apply(); syncDashboardFamily()
    }

    private fun profilesJson(items: List<FamilyProfile>): JSONArray {
        val a = JSONArray()
        items.forEach { a.put(JSONObject().apply { put("id", it.id); put("displayName", it.displayName); put("role", it.role.name); put("createdAtMs", it.createdAtMs); put("guardianProfileIds", JSONArray(it.guardianProfileIds)) }) }
        return a
    }

    private fun devicesJson(items: List<FamilyDevice>): JSONArray {
        val a = JSONArray()
        items.forEach { a.put(JSONObject().apply { put("id", it.id); put("displayName", it.displayName); put("ownerProfileId", it.ownerProfileId); put("linkState", it.linkState.name); put("linkedAtMs", it.linkedAtMs ?: 0L) }) }
        return a
    }

    private fun syncDashboardFamily() {
        val profiles = profiles(); val owner = profiles.firstOrNull { it.role == FamilyRole.OWNER }; val child = profiles.firstOrNull { it.role == FamilyRole.CHILD }
        val dashboardPrefs = appContext.getSharedPreferences("famyrex_prefs", Context.MODE_PRIVATE); val existingCode = dashboardPrefs.getString("link_code", "") ?: ""
        dashboardPrefs.edit().putString("parent_name", owner?.displayName.orEmpty()).putString("child_name", child?.displayName.orEmpty()).putString("link_code", existingCode).apply()
    }

    companion object {
        private const val KEY_CLOUD_PROFILE_IDS = "cloud_mirror_profile_ids"
        private const val KEY_CLOUD_DEVICE_IDS = "cloud_mirror_device_ids"

        private fun parseStringSet(raw: String?): Set<String> {
            if (raw.isNullOrBlank()) return emptySet()
            return runCatching {
                val array = JSONArray(raw)
                buildSet { for (i in 0 until array.length()) array.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add) }
            }.getOrDefault(emptySet())
        }

        internal fun parseProfiles(raw: String?): List<FamilyProfile> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching { val array = JSONArray(raw); buildList { for (i in 0 until array.length()) runCatching { val o = array.getJSONObject(i); val id = o.getString("id").trim(); val displayName = o.getString("displayName").trim(); val role = FamilyRole.valueOf(o.getString("role")); val createdAtMs = o.getLong("createdAtMs"); require(id.isNotBlank() && displayName.isNotBlank() && createdAtMs > 0L); val guardians = o.optJSONArray("guardianProfileIds")?.let { ids -> buildList { for (j in 0 until ids.length()) ids.optString(j).trim().takeIf { it.isNotBlank() }?.let(::add) } } ?: emptyList(); add(FamilyProfile(id, displayName, role, createdAtMs, guardians.distinct())) } } }.getOrDefault(emptyList())
        }
        internal fun parseDevices(raw: String?): List<FamilyDevice> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching { val array = JSONArray(raw); buildList { for (i in 0 until array.length()) runCatching { val o = array.getJSONObject(i); val id = o.getString("id").trim(); val displayName = o.getString("displayName").trim(); val ownerProfileId = o.getString("ownerProfileId").trim(); val linkState = DeviceLinkState.valueOf(o.getString("linkState")); val linkedAtMs = o.optLong("linkedAtMs", 0L).takeIf { it > 0L }; require(id.isNotBlank() && displayName.isNotBlank() && ownerProfileId.isNotBlank()); add(FamilyDevice(id, displayName, ownerProfileId, linkState, linkedAtMs)) } } }.getOrDefault(emptyList())
        }
        internal fun parseAppMode(raw: String?): FamyrexAppMode = runCatching { FamyrexAppMode.valueOf(raw ?: FamyrexAppMode.PARENT.name) }.getOrDefault(FamyrexAppMode.PARENT)
    }
}

data class VerifiedFamilyIdentity(val familyId: String, val secret: String, val fingerprint: String, val verifiedAtMs: Long)
private object UUIDHolder { fun next(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(16) }
