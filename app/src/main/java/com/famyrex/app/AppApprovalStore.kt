package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local state for adult-approved applications.
 * This store does not grant installation privileges by itself; enforcement remains explicit.
 */
class AppApprovalStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun isApproved(packageName: String): Boolean =
        packageName in loadApprovedPackages()

    @Synchronized
    fun approve(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) return false
        val packages = loadApprovedPackages().toMutableSet()
        packages.add(packageName)
        return persist(packages)
    }

    @Synchronized
    fun revoke(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) return false
        val packages = loadApprovedPackages().toMutableSet()
        packages.remove(packageName)
        return persist(packages)
    }

    @Synchronized
    fun approvedPackages(): Set<String> = loadApprovedPackages()

    private fun loadApprovedPackages(): Set<String> {
        val raw = prefs.getString(KEY_PACKAGES, null) ?: return emptySet()
        return runCatching {
            val array = JSONArray(raw)
            buildSet {
                for (index in 0 until array.length()) {
                    val packageName = array.optString(index).trim()
                    if (isValidPackageName(packageName)) add(packageName)
                }
            }
        }.getOrDefault(emptySet())
    }

    private fun persist(packages: Set<String>): Boolean {
        val array = JSONArray()
        packages.asSequence().sorted().take(MAX_PACKAGES).forEach(array::put)
        return prefs.edit().putString(KEY_PACKAGES, array.toString()).commit()
    }

    companion object {
        private const val PREFS_NAME = "famyrex_app_approvals"
        private const val KEY_PACKAGES = "approved_packages"
        private const val MAX_PACKAGES = 100
        private val PACKAGE_NAME_REGEX = Regex("^[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+$")

        private fun isValidPackageName(value: String): Boolean =
            value.length in 1..255 && PACKAGE_NAME_REGEX.matches(value)
    }
}
