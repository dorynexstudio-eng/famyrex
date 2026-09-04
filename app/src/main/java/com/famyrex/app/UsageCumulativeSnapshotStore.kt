package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class UsageCumulativeSnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_usage_cumulative", Context.MODE_PRIVATE)

    fun load(dateKey: String): List<UsageCumulativeSnapshot> {
        val raw = prefs.getString(dateKey, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val totalsObj = obj.optJSONObject("totals") ?: JSONObject()
                    val totals = mutableMapOf<String, Long>()
                    totalsObj.keys().forEach { key ->
                        totals[key] = totalsObj.optLong(key, 0L)
                    }
                    add(UsageCumulativeSnapshot(obj.optLong("timestampMs"), totals))
                }
            }.sortedBy { it.timestampMs }.takeLast(96)
        }.getOrDefault(emptyList())
    }

    fun save(dateKey: String, snapshot: UsageCumulativeSnapshot) {
        val updated = (load(dateKey) + snapshot)
            .distinctBy { it.timestampMs }
            .sortedBy { it.timestampMs }
            .takeLast(96)

        val arr = JSONArray()
        updated.forEach { item ->
            val obj = JSONObject()
            obj.put("timestampMs", item.timestampMs)
            val totals = JSONObject()
            item.totalsByPackageMs.forEach { (pkg, ms) -> totals.put(pkg, ms) }
            obj.put("totals", totals)
            arr.put(obj)
        }
        prefs.edit().putString(dateKey, arr.toString()).apply()
    }

    fun deltaSincePrevious(dateKey: String, current: UsageCumulativeSnapshot): Map<String, Long> {
        val previous = load(dateKey).lastOrNull { it.timestampMs < current.timestampMs } ?: return emptyMap()
        val packages = previous.totalsByPackageMs.keys + current.totalsByPackageMs.keys
        return packages.associateWith { pkg ->
            val now = current.totalsByPackageMs[pkg] ?: 0L
            val before = previous.totalsByPackageMs[pkg] ?: 0L
            (now - before).coerceAtLeast(0L)
        }.filterValues { it > 0L }
    }
}
