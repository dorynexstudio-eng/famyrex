package com.famyrex.app

import org.json.JSONObject

/** Stable JSON wire format for Firestore/FCM transport without exposing Firebase to the model. */
object FamilyControlCommandCodec {
    fun encode(command: FamilyControlCommand): String = JSONObject().apply {
        put("commandId", command.commandId)
        put("familyId", command.familyId)
        put("memberId", command.memberId)
        put("deviceId", command.deviceId)
        put("action", command.action.name)
        put("issuedAtMs", command.issuedAtMs)
        put("expiresAtMs", command.expiresAtMs)
        command.value?.let { put("value", it) }
        put("requiresAdultConfirmation", command.requiresAdultConfirmation)
    }.toString()

    fun decode(raw: String): FamilyControlCommand? = runCatching {
        val json = JSONObject(raw)
        FamilyControlCommand(
            commandId = json.getString("commandId"),
            familyId = json.getString("familyId"),
            memberId = json.getString("memberId"),
            deviceId = json.getString("deviceId"),
            action = FamilyControlAction.valueOf(json.getString("action")),
            issuedAtMs = json.getLong("issuedAtMs"),
            expiresAtMs = json.getLong("expiresAtMs"),
            value = json.optString("value", null),
            requiresAdultConfirmation = json.optBoolean("requiresAdultConfirmation", true)
        )
    }.getOrNull()
}
