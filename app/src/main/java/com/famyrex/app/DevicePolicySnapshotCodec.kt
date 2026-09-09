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
        if (raw.length > MAX_PAYLOAD_LENGTH) error("Policy snapshot is too large")
        val root = JSONObject(raw)
        val deviceId = root.getString("deviceId")
        if (deviceId.isBlank() || deviceId.length > MAX_DEVICE_ID_LENGTH) error("Invalid deviceId")

        val appsJson = root.optJSONArray("apps") ?: JSONArray()
        if (appsJson.length() > MAX_APPS) error("Too many app policies")
        val apps = buildList(appsJson.length()) {
            for (i in 0 until appsJson.length()) {
                val item = appsJson.optJSONObject(i) ?: error("Invalid app policy")
                val packageName = item.getString("packageName")
                val displayName = item.getString("displayName")
                if (!PACKAGE_NAME_REGEX.matches(packageName)) error("Invalid packageName")
                if (displayName.length > MAX_DISPLAY_NAME_LENGTH) error("Display name is too long")
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
            deviceId = deviceId,
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
        val value = get(name)
        return when (value) {
            is Int -> value
            is Long -> value.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt()
                ?: error("Integer out of range")
            is Number -> value.toLong().takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt()
                ?: error("Integer out of range")
            else -> error("Expected integer")
        }
    }

    private const val MAX_PAYLOAD_LENGTH = 64 * 1024
    private const val MAX_APPS = 100
    private const val MAX_DEVICE_ID_LENGTH = 128
    private const val MAX_DISPLAY_NAME_LENGTH = 100
    private val PACKAGE_NAME_REGEX = Regex("^[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+$")
}
