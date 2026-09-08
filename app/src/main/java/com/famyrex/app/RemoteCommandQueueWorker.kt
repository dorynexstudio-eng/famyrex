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
        val identity = FamilyDeviceIdentityStore(context).current()
            ?: return Result.success()
        if (!identity.isSupervised) return Result.success()

        val currentUser = FirebaseAuth.getInstance().currentUser
            ?: return Result.success()
        // Recovery is only valid for the anonymous child transport identity.
        if (!currentUser.isAnonymous) return Result.success()
        val firebaseUid = identity.firebaseUid ?: currentUser.uid
        if (firebaseUid != currentUser.uid) return Result.success()

        val familyId = identity.familyId ?: return Result.success()

        // Query only pending commands so an old history cannot crowd out a live command.
        // The composite index is checked in firebase/firestore.indexes.json.
        val snapshot = Tasks.await(
            FirebaseFirestore.getInstance()
                .collection("families").document(familyId).collection("commands")
                .whereEqualTo("targetDeviceUid", firebaseUid)
                .whereEqualTo("status", "sent")
                .limit(50)
                .get()
        )

        val executor = FamilyRemoteCommandExecutor(context)
        val receiptStore = RemoteCommandReceiptStore(context)
        val now = System.currentTimeMillis()
        snapshot.documents
            .asSequence()
            .sortedBy { it.getLong("issuedAtMs") ?: Long.MAX_VALUE }
            .forEach { document ->
                val command = document.toRemoteCommand() ?: return@forEach
                if (command.familyId != familyId ||
                    command.memberId != identity.famyrexMemberId ||
                    command.deviceId != identity.deviceId
                ) return@forEach

                // Let the canonical executor decide whether the command is expired or
                // otherwise invalid, so recovery produces the same receipt and audit
                // trail as FCM instead of silently leaving stale commands in "sent".
                val receipt = executor.execute(command, identity, now)
                receiptStore.save(receipt)
                RemoteCommandReceiptReporter.report(context, receipt)
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
