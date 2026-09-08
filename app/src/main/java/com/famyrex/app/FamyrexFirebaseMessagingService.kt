package com.famyrex.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Receives command-only FCM data messages; sensitive data is never placed in notifications. */
class FamyrexFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val raw = message.data[KEY_COMMAND] ?: return
        val command = FamilyControlCommandCodec.decode(raw) ?: return
        val identity = FamilyDeviceIdentityStore(this).current() ?: return
        if (command.familyId != identity.familyId ||
            command.memberId != identity.famyrexMemberId ||
            command.deviceId != identity.deviceId
        ) return

        val receipt = FamilyRemoteCommandExecutor(this).execute(command, identity)
        RemoteCommandReceiptStore(this).save(receipt)
        if (!receipt.success) Log.w(TAG, "Remote command rejected: ${receipt.reason}")
    }

    override fun onNewToken(token: String) {
        if (token.isBlank()) return
        FamilyDeviceTokenRegistrar.rememberToken(this, token)
        FamilyDeviceTokenRegistrar.register(this, token)
    }

    companion object {
        const val KEY_COMMAND = "famyrex_command"
        private const val TAG = "FamyrexFCM"
    }
}
