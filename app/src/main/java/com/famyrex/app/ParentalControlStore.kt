package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persistencia local de la configuración parental. */
class ParentalControlStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_parental_controls", Context.MODE_PRIVATE)

    fun load(): ParentalControlConfig {
        val raw = prefs.getString(KEY_CONFIG, null) ?: return ParentalControlConfig()
        return parseParentalControlConfig(raw)
    }

    fun save(config: ParentalControlConfig) {
        val root = JSONObject().apply {
            config.screenTimeLimit?.let { limit ->
                put("screenTime", JSONObject().apply {
                    put("dailyMinutes", limit.dailyMinutes)
                    put("enabled", limit.enabled)
                })
            }
            put("pauseSchedules", JSONArray().apply {
                config.pauseSchedules.forEach { schedule ->
                    put(JSONObject().apply {
                        put("startMinuteOfDay", schedule.startMinuteOfDay)
                        put("endMinuteOfDay", schedule.endMinuteOfDay)
                        put("enabled", schedule.enabled)
                    })
                }
            })
            put("appRestrictions", JSONArray().apply {
                config.appRestrictions.forEach { restriction ->
                    put(JSONObject().apply {
                        put("packageName", restriction.packageName)
                        if (restriction.dailyMinutes != null) put("dailyMinutes", restriction.dailyMinutes) else put("dailyMinutes", JSONObject.NULL)
                        put("blocked", restriction.blocked)
                    })
                }
            })
        }
        prefs.edit().putString(KEY_CONFIG, root.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_CONFIG).apply()
    }

    companion object {
        private const val KEY_CONFIG = "config"
    }
}

internal fun parseParentalControlConfig(raw: String): ParentalControlConfig = runCatching {
    val root = JSONObject(raw)

    val screenTime = root.optJSONObject("screenTime")?.let { item ->
        runCatching {
            ScreenTimeLimit(
                dailyMinutes = item.getInt("dailyMinutes"),
                enabled = item.optBoolean("enabled", true)
            )
        }.getOrNull()
    }

    val schedules = buildList {
        val array = root.optJSONArray("pauseSchedules") ?: JSONArray()
        for (i in 0 until array.length()) {
            runCatching {
                val item = array.getJSONObject(i)
                add(
                    PauseSchedule(
                        startMinuteOfDay = item.getInt("startMinuteOfDay"),
                        endMinuteOfDay = item.getInt("endMinuteOfDay"),
                        enabled = item.optBoolean("enabled", true)
                    )
                )
            }
        }
    }

    val restrictions = buildList {
        val array = root.optJSONArray("appRestrictions") ?: JSONArray()
        for (i in 0 until array.length()) {
            runCatching {
                val item = array.getJSONObject(i)
                add(
                    AppRestriction(
                        packageName = item.getString("packageName"),
                        dailyMinutes = if (item.has("dailyMinutes") && !item.isNull("dailyMinutes")) item.getInt("dailyMinutes") else null,
                        blocked = item.optBoolean("blocked", false)
                    )
                )
            }
        }
    }

    ParentalControlConfig(
        screenTimeLimit = screenTime,
        pauseSchedules = schedules,
        appRestrictions = restrictions
    )
}.getOrDefault(ParentalControlConfig())
