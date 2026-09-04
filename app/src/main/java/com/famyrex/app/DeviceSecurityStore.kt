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
        return runCatching {
            val o = JSONObject(raw)
            val reasonsArray = o.optJSONArray("reasons")
            val reasons = buildList {
                if (reasonsArray != null) {
                    for (i in 0 until reasonsArray.length()) add(reasonsArray.optString(i))
                }
            }
            DeviceSecuritySnapshot(
                timestampMs = o.optLong("timestampMs"),
                androidVersion = o.optString("androidVersion"),
                sdkInt = o.optInt("sdkInt"),
                isDebuggable = o.optBoolean("debuggable"),
                hasSecureLockScreen = o.optBoolean("secureLock"),
                isDeveloperOptionsEnabled = if (o.isNull("developerOptions")) null else o.optBoolean("developerOptions"),
                installedAppCount = o.optInt("installedAppCount"),
                usageAccessGranted = o.optBoolean("usageAccess"),
                foregroundLocationGranted = o.optBoolean("foregroundLocation"),
                backgroundLocationGranted = o.optBoolean("backgroundLocation"),
                securityLevel = runCatching {
                    SecurityLevel.valueOf(o.optString("securityLevel"))
                }.getOrDefault(SecurityLevel.ATTENTION),
                reasons = reasons
            )
        }.getOrNull()
    }
}
