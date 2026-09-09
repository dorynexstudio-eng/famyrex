package com.famyrex.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/** Publishes minimal supervised-device liveness and protection-health metadata for the adult dashboard. */
object ChildDeviceStatusReporter {
    private const val MAX_REASONS = 8
    private const val MAX_REASON_LENGTH = 200

    fun report(context: Context, health: ProtectionHealth) {
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isEmpty()) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (!user.isAnonymous) return

        val identity = FamilyDeviceIdentityStore(appContext).current() ?: return
        val familyId = identity.familyId?.takeIf { it.isNotBlank() } ?: return
        if (!identity.isSupervised || identity.firebaseUid != user.uid) return

        val now = System.currentTimeMillis()
        val reasons = health.reasons
            .asSequence()
            .map { it.trim().take(MAX_REASON_LENGTH) }
            .filter { it.isNotBlank() }
            .take(MAX_REASONS)
            .toList()

        FirebaseFirestore.getInstance()
            .collection("families").document(familyId)
            .collection("devices").document(user.uid)
            .set(
                mapOf(
                    "uid" to user.uid,
                    "memberUid" to user.uid,
                    "deviceStatusUpdatedAtMs" to now,
                    "deviceStatusUpdatedAt" to FieldValue.serverTimestamp(),
                    "protectionActive" to health.active,
                    "protectionReasons" to reasons,
                    "protectionCheckedAtMs" to health.checkedAtMs
                ),
                SetOptions.merge()
            )
    }
}
