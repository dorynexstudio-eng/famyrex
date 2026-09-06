package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DeviceSecurityStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_device_security", Context.MODE_PRIVATE)

    fun save(snapshot: DeviceSecuritySnapshot) {
        val obj = JSONObject()
            .put("timestampMs", snapshot.timestampMs)
            .put("androidVersion", snapshot.androidVersion)
            .put("sdkInt", snapshot.sdkInt)
            .put("debuggable", snapshot.isDebuggable)
            .put("secureLock", snapshot.hasSecureLockScreen)
            .put("developerOptions", snapshot.isDeveloperOptionsEnabled)
            .put("installedAppCount", snapshot.installedAppCount)
            .put("usageAccess", snapshot.usageAccessGranted)
            .put("foregroundLocation", snapshot.foregroundLocationGranted)
            .put("backgroundLocation", snapshot.backgroundLocationGranted)
            .put("securityLevel", snapshot.securityLevel.name)
            .put("reasons", JSONArray(snapshot.reasons))
        prefs.edit().putString("latest", obj.toString()).apply()
    }

    fun load(): DeviceSecuritySnapshot? {
        val raw = prefs.getString("latest", null) ?: return null
        return parse(raw)
    }

    companion object {
        private val REQUIRED_KEYS = listOf(
            "timestampMs", "androidVersion", "sdkInt", "debuggable", "secureLock",
            "developerOptions", "installedAppCount", "usageAccess", "foregroundLocation",
            "backgroundLocation", "securityLevel", "reasons"
        )

        internal fun parse(raw: String): DeviceSecuritySnapshot? = runCatching {
            val o = JSONObject(raw)
            require(REQUIRED_KEYS.all(o::has))

            val timestampMs = o.getLong("timestampMs")
            val androidVersion = o.getString("androidVersion")
            val sdkInt = o.getInt("sdkInt")
            val installedAppCount = o.getInt("installedAppCount")
            require(timestampMs > 0L)
            require(androidVersion.isNotBlank())
            require(sdkInt > 0)
            require(installedAppCount >= 0)

            val reasonsArray = o.getJSONArray("reasons")
            val reasons = buildList {
                for (i in 0 until reasonsArray.length()) add(reasonsArray.getString(i))
            }

            DeviceSecuritySnapshot(
                timestampMs = timestampMs,
                androidVersion = androidVersion,
                sdkInt = sdkInt,
                isDebuggable = o.getBoolean("debuggable"),
                hasSecureLockScreen = o.getBoolean("secureLock"),
                isDeveloperOptionsEnabled = if (o.isNull("developerOptions")) null else o.getBoolean("developerOptions"),
                installedAppCount = installedAppCount,
                usageAccessGranted = o.getBoolean("usageAccess"),
                foregroundLocationGranted = o.getBoolean("foregroundLocation"),
                backgroundLocationGranted = o.getBoolean("backgroundLocation"),
                securityLevel = SecurityLevel.valueOf(o.getString("securityLevel")),
                reasons = reasons
            )
        }.getOrNull()
    }
}
