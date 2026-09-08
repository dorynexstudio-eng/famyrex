package com.famyrex.app

import android.content.Context

/** Last remote command result, useful to surface transparent execution status in the UI. */
class RemoteCommandReceiptStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(receipt: FamilyControlReceipt) {
        prefs.edit()
            .putString(KEY_COMMAND_ID, receipt.commandId)
            .putString(KEY_ACTION, receipt.action.name)
            .putLong(KEY_ACCEPTED_AT, receipt.acceptedAtMs)
            .putLong(KEY_COMPLETED_AT, receipt.completedAtMs ?: 0L)
            .putBoolean(KEY_SUCCESS, receipt.success)
            .putString(KEY_REASON, receipt.reason)
            .apply()
    }

    fun load(): FamilyControlReceipt? {
        val commandId = prefs.getString(KEY_COMMAND_ID, null) ?: return null
        val action = runCatching { FamilyControlAction.valueOf(prefs.getString(KEY_ACTION, "")!!) }.getOrNull() ?: return null
        return FamilyControlReceipt(
            commandId = commandId,
            action = action,
            acceptedAtMs = prefs.getLong(KEY_ACCEPTED_AT, 0L),
            completedAtMs = prefs.getLong(KEY_COMPLETED_AT, 0L).takeIf { it != 0L },
            success = prefs.getBoolean(KEY_SUCCESS, false),
            reason = prefs.getString(KEY_REASON, null)
        )
    }

    companion object {
        private const val PREFS = "famyrex_last_remote_receipt"
        private const val KEY_COMMAND_ID = "commandId"
        private const val KEY_ACTION = "action"
        private const val KEY_ACCEPTED_AT = "acceptedAt"
        private const val KEY_COMPLETED_AT = "completedAt"
        private const val KEY_SUCCESS = "success"
        private const val KEY_REASON = "reason"
    }
}
