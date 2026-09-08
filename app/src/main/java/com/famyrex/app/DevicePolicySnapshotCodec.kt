package com.famyrex.app

import org.json.JSONArray
import org.json.JSONObject

/** Stable JSON wire format for complete device policy snapshots. */
object DevicePolicySnapshotCodec {
    fun encode(snapshot: DevicePolicySnapshot): String = JSONObject().apply {
        put("deviceId", snapshot.deviceId)
        putNullableInt("dailyLimitMinutes", snapshot.dailyLimitMinutes)
        putNullableInt("bedtimeStartMinutes", snapshot.bedtimeStartMinutes)
        putNullableInt("bedtimeEndMinutes", snapshot.bedtimeEndMinutes)
        put("webPolicyVersion", snapshot.webPolicyVersion)
        put("revision", snapshot.revision)
        put("apps", JSONArray().apply {
            snapshot.apps.forEach { app ->
                put(JSONObject().apply {
                    put("packageName", app.packageName)
                    put("displayName", app.displayName)
                    put("blocked", app.blocked)
                    putNullableInt("dailyLimitMinutes", app.dailyLimitMinutes)
                    put("approvalRequired", app.approvalRequired)
                    put("installAllowed", app.installAllowed)
                    put("source", app.source.name)
                })
            }
        })
    }.toString()

    fun decode(raw: String): DevicePolicySnapshot? = runCatching {
        val root = JSONObject(raw)
        val appsJson = root.optJSONArray("apps") ?: JSONArray()
        val apps = buildList {
            for (i in 0 until appsJson.length()) {
                val item = appsJson.optJSONObject(i) ?: error("Invalid app policy")
                val packageName = item.getString("packageName")
                val displayName = item.getString("displayName")
                val source = AppInstallSource.valueOf(item.optString("source", AppInstallSource.UNKNOWN.name))
                add(AppPolicy(
                    packageName = packageName,
                    displayName = displayName,
                    blocked = item.optBoolean("blocked", false),
                    dailyLimitMinutes = item.optNullableInt("dailyLimitMinutes"),
                    approvalRequired = item.optBoolean("approvalRequired", false),
                    installAllowed = item.optBoolean("installAllowed", true),
                    source = source
                ))
            }
        }
        DevicePolicySnapshot(
            deviceId = root.getString("deviceId"),
            dailyLimitMinutes = root.optNullableInt("dailyLimitMinutes"),
            bedtimeStartMinutes = root.optNullableInt("bedtimeStartMinutes"),
            bedtimeEndMinutes = root.optNullableInt("bedtimeEndMinutes"),
            apps = apps,
            webPolicyVersion = root.optLong("webPolicyVersion", 0L),
            revision = root.optLong("revision", 0L)
        )
    }.getOrNull()

    private fun JSONObject.putNullableInt(name: String, value: Int?) {
        put(name, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optNullableInt(name: String): Int? {
        if (isNull(name)) return null
        return getInt(name)
    }
}
