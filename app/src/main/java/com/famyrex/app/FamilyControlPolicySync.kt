package com.famyrex.app

import android.content.Context

/** Applies a complete device policy snapshot through the existing local policy store. */
object FamilyControlPolicySync {
    fun apply(context: Context, snapshot: DevicePolicySnapshot): FamilyControlReceipt {
        val now = System.currentTimeMillis()
        val validationError = validate(snapshot)
        if (validationError != null) {
            return FamilyControlReceipt(
                commandId = "policy-${snapshot.deviceId}-${snapshot.revision}",
                action = FamilyControlAction.SYNC_POLICY,
                acceptedAtMs = now,
                completedAtMs = now,
                success = false,
                reason = validationError
            )
        }

        ParentalControlStore(context).save(DevicePolicySnapshotAdapter.toLocalConfig(snapshot))
        return FamilyControlReceipt(
            commandId = "policy-${snapshot.deviceId}-${snapshot.revision}",
            action = FamilyControlAction.SYNC_POLICY,
            acceptedAtMs = now,
            completedAtMs = System.currentTimeMillis(),
            success = true,
            reason = "Política sincronizada localmente"
        )
    }

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
}
