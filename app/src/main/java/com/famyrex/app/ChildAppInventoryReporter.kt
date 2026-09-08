package com.famyrex.app

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

/** Publishes a bounded, non-sensitive inventory of launchable child-device apps for adult-side controls. */
object ChildAppInventoryReporter {
    private const val PREFS = "famyrex_child_app_inventory"
    private const val KEY_FINGERPRINT = "fingerprint"
    private const val KEY_LAST_PUBLISHED_MS = "last_published_ms"
    private const val MAX_APPS = 100
    private const val REFRESH_INTERVAL_MS = 24L * 60L * 60L * 1000L

    fun report(context: Context) {
        val appContext = context.applicationContext
        if (FirebaseApp.getApps(appContext).isEmpty()) return
        val user = FirebaseAuth.getInstance().currentUser ?: return
        if (!user.isAnonymous) return

        val identity = FamilyDeviceIdentityStore(appContext).current() ?: return
        if (!identity.isSupervised || identity.familyId.isNullOrBlank() || identity.firebaseUid != user.uid) return

        val apps = loadLaunchableApps(appContext)
        val fingerprint = apps.joinToString("|") { "${it.packageName}:${it.label}" }.hashCode().toString()
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val unchanged = prefs.getString(KEY_FINGERPRINT, null) == fingerprint
        val recentlyPublished = now - prefs.getLong(KEY_LAST_PUBLISHED_MS, 0L) < REFRESH_INTERVAL_MS
        if (unchanged && recentlyPublished) return

        val payload = mapOf(
            "uid" to user.uid,
            "memberUid" to user.uid,
            "appInventory" to apps.map { mapOf("packageName" to it.packageName, "label" to it.label) },
            "appInventoryUpdatedAtMs" to now,
            "appInventoryUpdatedAt" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("families").document(identity.familyId!!)
            .collection("devices").document(user.uid)
            .set(payload, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                prefs.edit()
                    .putString(KEY_FINGERPRINT, fingerprint)
                    .putLong(KEY_LAST_PUBLISHED_MS, now)
                    .apply()
            }
    }

    private data class InventoryApp(val packageName: String, val label: String)

    private fun loadLaunchableApps(context: Context): List<InventoryApp> = runCatching {
        val packageManager = context.packageManager
        packageManager.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map { info: ApplicationInfo ->
                InventoryApp(
                    info.packageName,
                    info.loadLabel(packageManager).toString().ifBlank { info.packageName }
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .take(MAX_APPS)
            .toList()
    }.getOrDefault(emptyList())
}
