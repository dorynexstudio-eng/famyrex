package com.famyrex.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Publishes only execution metadata so the authorized adult can see remote-command status. */
object RemoteCommandReceiptReporter {
    fun report(context: Context, receipt: FamilyControlReceipt) {
        if (receipt.commandId.isBlank()) return
        if (FirebaseApp.getApps(context.applicationContext).isEmpty()) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (!user.isAnonymous) return
        val identity = FamilyDeviceIdentityStore(context.applicationContext).current() ?: return
        val familyId = identity.familyId?.takeIf { it.isNotBlank() } ?: return
        val deviceId = identity.deviceId.takeIf { it.isNotBlank() } ?: return

        val data = hashMapOf<String, Any?>(
            "deviceId" to deviceId,
            "memberId" to identity.famyrexMemberId,
            "action" to receipt.action.name,
            "acceptedAtMs" to receipt.acceptedAtMs,
            "completedAtMs" to receipt.completedAtMs,
            "success" to receipt.success,
            "reason" to receipt.reason?.take(300)
        )

        FirebaseFirestore.getInstance()
            .collection("families").document(familyId)
            .collection("commands").document(receipt.commandId)
            .collection("receipts").document(deviceId)
            .set(data)
    }
}
