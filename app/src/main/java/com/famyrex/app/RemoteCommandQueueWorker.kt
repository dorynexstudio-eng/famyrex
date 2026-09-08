package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Recovery path for remote commands missed by FCM while the device was offline. */
class RemoteCommandQueueWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val context = applicationContext
        val identity = FamilyDeviceIdentityStore(context).current() ?: return Result.success()
        val firebaseUid = identity.firebaseUid ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.success()
        val familyId = identity.familyId ?: return Result.success()

        // Filter status locally to avoid requiring a composite Firestore index.
        val snapshot = Tasks.await(
            FirebaseFirestore.getInstance()
                .collection("families").document(familyId).collection("commands")
                .whereEqualTo("targetDeviceUid", firebaseUid)
                .limit(50)
                .get()
        )

        val executor = FamilyRemoteCommandExecutor(context)
        val now = System.currentTimeMillis()
        snapshot.documents
            .asSequence()
            .filter { it.getString("status") == "sent" }
            .sortedBy { it.getLong("issuedAtMs") ?: Long.MAX_VALUE }
            .forEach { document ->
                val command = document.toRemoteCommand() ?: return@forEach
                if (command.familyId != familyId || command.memberId != identity.famyrexMemberId || command.deviceId != identity.deviceId) return@forEach
                if (command.expiresAtMs <= now) return@forEach
                executor.execute(command, identity, now)
            }
        Result.success()
    }.getOrElse { Result.retry() }

    private fun com.google.firebase.firestore.DocumentSnapshot.toRemoteCommand(): FamilyControlCommand? = runCatching {
        FamilyControlCommand(
            commandId = getString("commandId") ?: id,
            familyId = getString("familyId") ?: return null,
            memberId = getString("memberId") ?: return null,
            deviceId = getString("deviceId") ?: return null,
            action = FamilyControlAction.valueOf(getString("action") ?: return null),
            issuedAtMs = getLong("issuedAtMs") ?: return null,
            expiresAtMs = getLong("expiresAtMs") ?: return null,
            value = getString("value"),
            requiresAdultConfirmation = getBoolean("requiresAdultConfirmation") ?: true
        )
    }.getOrNull()
}
