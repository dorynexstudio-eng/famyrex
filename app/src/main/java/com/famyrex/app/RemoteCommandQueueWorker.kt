package com.famyrex.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RemoteCommandQueueWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val context = applicationContext
        val familyStore = FamilyStore(context)
        if (!familyStore.isSupervisedEnrollmentActive()) return Result.success()
        val identity = FamilyDeviceIdentityStore(context).current() ?: return Result.success()
        if (!identity.isSupervised) return Result.success()
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        if (!currentUser.isAnonymous) return Result.success()
        val firebaseUid = identity.firebaseUid ?: currentUser.uid
        if (firebaseUid != currentUser.uid) return Result.success()
        val familyId = identity.familyId ?: return Result.success()
        val snapshot = Tasks.await(
            FirebaseFirestore.getInstance()
                .collection("families").document(familyId).collection("commands")
                .whereEqualTo("targetDeviceUid", firebaseUid)
                .whereEqualTo("status", "sent")
                .orderBy("issuedAtMs")
                .limit(50)
                .get()
        )
        val executor = FamilyRemoteCommandExecutor(context)
        val receiptStore = RemoteCommandReceiptStore(context)
        val now = System.currentTimeMillis()
        snapshot.documents.asSequence().forEach { document ->
            if (!familyStore.isSupervisedEnrollmentActive()) return@forEach
            val currentIdentity = FamilyDeviceIdentityStore(context).current() ?: return@forEach
            if (currentIdentity.familyId != familyId ||
                currentIdentity.famyrexMemberId != identity.famyrexMemberId ||
                currentIdentity.deviceId != identity.deviceId ||
                currentIdentity.firebaseUid != currentUser.uid
            ) return@forEach
            val command = document.toRemoteCommand() ?: return@forEach
            if (command.familyId != familyId ||
                command.memberId != identity.famyrexMemberId ||
                command.deviceId != identity.deviceId
            ) return@forEach
            val receipt = executor.execute(command, currentIdentity, now)
            receiptStore.save(receipt)
            RemoteCommandReceiptReporter.report(context, receipt, currentIdentity)?.let { task ->
                Tasks.await(task)
            }
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
