package com.famyrex.app

import android.content.Context

/** Applies a complete device policy snapshot through the existing local policy store. */
object FamilyControlPolicySync {
    fun apply(context: Context, snapshot: DevicePolicySnapshot): FamilyControlReceipt {
        val now = System.currentTimeMillis()
        val validationError = validate(snapshot)
        if (validationError != null) {
            return failure(snapshot, now, validationError)
        }

        val appContext = context.applicationContext
        val state = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastRevision = state.getLong(revisionKey(snapshot.deviceId), -1L)
        if (snapshot.revision <= lastRevision) {
            return failure(
                snapshot,
                now,
                "La política es antigua: revisión ${snapshot.revision}, última aplicada $lastRevision."
            )
        }

        ParentalControlStore(appContext).save(DevicePolicySnapshotAdapter.toLocalConfig(snapshot))
        state.edit().putLong(revisionKey(snapshot.deviceId), snapshot.revision).apply()

        return FamilyControlReceipt(
            commandId = "policy-${snapshot.deviceId}-${snapshot.revision}",
            action = FamilyControlAction.SYNC_POLICY,
            acceptedAtMs = now,
            completedAtMs = System.currentTimeMillis(),
            success = true,
            reason = "Política sincronizada localmente"
        )
    }

    private fun failure(snapshot: DevicePolicySnapshot, now: Long, reason: String) = FamilyControlReceipt(
        commandId = "policy-${snapshot.deviceId}-${snapshot.revision}",
        action = FamilyControlAction.SYNC_POLICY,
        acceptedAtMs = now,
        completedAtMs = now,
        success = false,
        reason = reason
    )

    private fun revisionKey(deviceId: String): String = "revision_$deviceId"

    private fun validate(snapshot: DevicePolicySnapshot): String? {
        if (snapshot.deviceId.isBlank()) return "El deviceId de la política está vacío"
        if (snapshot.revision < 0L) return "La revisión de la política no puede ser negativa"
        if (snapshot.dailyLimitMinutes != null && snapshot.dailyLimitMinutes !in 1..1440) {
            return "El límite diario no es válido"
        }
        val start = snapshot.bedtimeStartMinutes
        val end = snapshot.bedtimeEndMinutes
        if ((start == null) != (end == null)) return "El horario requiere inicio y fin"
        if (start != null && start !in 0..1439) return "La hora de inicio no es válida"
        if (end != null && end !in 0..1439) return "La hora de fin no es válida"
        snapshot.apps.forEach { policy ->
            if (policy.packageName.isBlank()) return "Una aplicación de la política no tiene packageName"
            if (policy.dailyLimitMinutes != null && policy.dailyLimitMinutes !in 1..1440) {
                return "El límite de una aplicación no es válido"
            }
        }
        return null
    }

    private const val PREFS_NAME = "famyrex_policy_sync"
}
