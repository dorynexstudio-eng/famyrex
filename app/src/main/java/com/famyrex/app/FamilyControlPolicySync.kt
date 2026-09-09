package com.famyrex.app

import android.content.Context

/** Applies a complete device policy snapshot through the existing local policy store. */
object FamilyControlPolicySync {
    @Synchronized
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

        // Persist the complete policy synchronously before advancing the revision. This prevents
        // the revision marker from becoming durable while the actual policy is still only queued
        // in SharedPreferences.apply(). If the process dies afterwards, replaying the same
        // revision remains safe and idempotent.
        val persisted = runCatching {
            ParentalControlStore(appContext)
                .saveBlocking(DevicePolicySnapshotAdapter.toLocalConfig(snapshot))
        }.getOrDefault(false)
        if (!persisted) {
            return failure(snapshot, now, "No se pudo guardar la política localmente")
        }

        val revisionPersisted = state.edit()
            .putLong(revisionKey(snapshot.deviceId), snapshot.revision)
            .commit()
        if (!revisionPersisted) {
            // The policy is already durable but the revision marker is not. A later delivery may
            // replay this revision; because applying a complete snapshot is idempotent, that is
            // preferable to reporting a successful revision that was never durably recorded.
            return failure(snapshot, now, "No se pudo guardar la revisión de la política")
        }

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
        if (snapshot.deviceId.length > MAX_DEVICE_ID_LENGTH) return "El deviceId de la política es demasiado largo"
        if (snapshot.revision < 0L) return "La revisión de la política no puede ser negativa"
        if (snapshot.webPolicyVersion < 0L) return "La versión de política web no puede ser negativa"
        if (snapshot.dailyLimitMinutes != null && snapshot.dailyLimitMinutes !in 1..1440) {
            return "El límite diario no es válido"
        }
        val start = snapshot.bedtimeStartMinutes
        val end = snapshot.bedtimeEndMinutes
        if ((start == null) != (end == null)) return "El horario requiere inicio y fin"
        if (start != null && start !in 0..1439) return "La hora de inicio no es válida"
        if (end != null && end !in 0..1439) return "La hora de fin no es válida"
        if (snapshot.apps.size > MAX_APPS) return "La política contiene demasiadas aplicaciones"
        val seenPackages = HashSet<String>(snapshot.apps.size)
        snapshot.apps.forEach { policy ->
            if (!PACKAGE_NAME_REGEX.matches(policy.packageName)) return "El packageName de una aplicación no es válido"
            if (!seenPackages.add(policy.packageName)) return "La política contiene aplicaciones duplicadas"
            if (policy.displayName.length > MAX_DISPLAY_NAME_LENGTH) return "El nombre de una aplicación es demasiado largo"
            if (policy.dailyLimitMinutes != null && policy.dailyLimitMinutes !in 1..1440) {
                return "El límite de una aplicación no es válido"
            }
        }
        return null
    }

    private const val PREFS_NAME = "famyrex_policy_sync"
    private const val MAX_APPS = 100
    private const val MAX_DEVICE_ID_LENGTH = 128
    private const val MAX_DISPLAY_NAME_LENGTH = 100
    private val PACKAGE_NAME_REGEX = Regex("^[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+$")
}
