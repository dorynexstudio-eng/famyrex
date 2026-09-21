package com.famyrex.app

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FamyrexFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val appContext = applicationContext
        if (!FamilyStore(appContext).isSupervisedEnrollmentActive()) return
        val raw = message.data[KEY_COMMAND] ?: return
        val command = FamilyControlCommandCodec.decode(raw) ?: return
        val identity = FamilyDeviceIdentityStore(appContext).current() ?: return
        if (command.familyId != identity.familyId ||
            command.memberId != identity.famyrexMemberId ||
            command.deviceId != identity.deviceId
        ) return
        val receipt = FamilyRemoteCommandExecutor(appContext).execute(command, identity)
        RemoteCommandReceiptStore(appContext).save(receipt)
        RemoteCommandReceiptReporter.report(appContext, receipt, identity)
        if (!receipt.success) Log.w(TAG, "Remote command rejected: " + receipt.reason)
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
