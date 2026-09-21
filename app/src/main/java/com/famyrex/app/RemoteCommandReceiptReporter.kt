package com.famyrex.app

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Publishes only execution metadata so the authorized adult can see remote-command status. */
object RemoteCommandReceiptReporter {
    fun report(context: Context, receipt: FamilyControlReceipt, expectedIdentity: FamyrexDeviceIdentity? = null): Task<Void>? {
        if (receipt.commandId.isBlank()) return null
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isEmpty()) return null
        if (!FamilyStore(appContext).isSupervisedEnrollmentActive()) return null
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        if (!user.isAnonymous) return null
        val identity = FamilyDeviceIdentityStore(appContext).current() ?: return null
        if (expectedIdentity != null && (
            identity.familyId != expectedIdentity.familyId ||
            identity.famyrexMemberId != expectedIdentity.famyrexMemberId ||
            identity.deviceId != expectedIdentity.deviceId ||
            identity.firebaseUid != expectedIdentity.firebaseUid
        )) return null
        val familyId = identity.familyId?.takeIf { it.isNotBlank() } ?: return null
        val deviceId = identity.deviceId.takeIf { it.isNotBlank() } ?: return null
        val data = hashMapOf<String, Any?>(
            "deviceId" to deviceId,
            "memberId" to identity.famyrexMemberId,
            "action" to receipt.action.name,
            "acceptedAtMs" to receipt.acceptedAtMs,
            "completedAtMs" to receipt.completedAtMs,
            "success" to receipt.success,
            "reason" to receipt.reason?.take(300)
        )
        return FirebaseFirestore.getInstance()
            .collection("families").document(familyId)
            .collection("commands").document(receipt.commandId)
            .collection("receipts").document(deviceId)
            .set(data)
    }
}
