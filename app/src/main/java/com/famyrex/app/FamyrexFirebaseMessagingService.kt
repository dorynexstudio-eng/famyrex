package com.famyrex.app

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Receives command-only FCM data messages; sensitive data is never placed in notifications. */
class FamyrexFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val raw = message.data[KEY_COMMAND] ?: return
        val command = FamilyControlCommandCodec.decode(raw) ?: return
        val identity = FamilyDeviceIdentityStore(this).current() ?: return
        FamilyControlCommandExecutor(this).execute(command, identity)
    }

    override fun onNewToken(token: String) {
        if (token.isBlank()) return
        getSharedPreferences(PREFS, MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token)
            .apply()
    }

    companion object {
        const val KEY_COMMAND = "famyrex_command"
        private const val PREFS = "famyrex_messaging"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}
