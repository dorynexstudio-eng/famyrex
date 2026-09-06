package com.famyrex.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParentalControlStoreTest {
    @Test
    fun invalidRootFallsBackToEmptyConfig() {
        val result = parse("not-json")
        assertNull(result.screenTimeLimit)
        assertEquals(0, result.pauseSchedules.size)
        assertEquals(0, result.appRestrictions.size)
    }

    @Test
    fun malformedScheduleDoesNotDiscardValidSchedules() {
        val result = parse("""
            {
              "pauseSchedules":[
                {"startMinuteOfDay":120,"endMinuteOfDay":300,"enabled":true},
                {"startMinuteOfDay":"broken","endMinuteOfDay":500},
                {"startMinuteOfDay":600,"endMinuteOfDay":900,"enabled":false}
              ]
            }
        """.trimIndent())

        assertEquals(listOf(120, 600), result.pauseSchedules.map { it.startMinuteOfDay })
    }

    @Test
    fun malformedRestrictionDoesNotDiscardValidRestrictions() {
        val result = parse("""
            {
              "appRestrictions":[
                {"packageName":"com.valid","dailyMinutes":60,"blocked":true},
                {"packageName":"","dailyMinutes":30},
                {"packageName":"com.valid2","dailyMinutes":null,"blocked":false}
              ]
            }
        """.trimIndent())

        assertEquals(listOf("com.valid", "com.valid2"), result.appRestrictions.map { it.packageName })
    }

    private fun parse(raw: String): ParentalControlConfig =
        runCatching {
            val root = org.json.JSONObject(raw)
            val screenTime = root.optJSONObject("screenTime")?.let { item ->
                runCatching {
                    ScreenTimeLimit(item.getInt("dailyMinutes"), item.optBoolean("enabled", true))
                }.getOrNull()
            }
            val schedules = buildList {
                val array = root.optJSONArray("pauseSchedules") ?: org.json.JSONArray()
                for (i in 0 until array.length()) runCatching {
                    val item = array.getJSONObject(i)
                    add(PauseSchedule(item.getInt("startMinuteOfDay"), item.getInt("endMinuteOfDay"), item.optBoolean("enabled", true)))
                }
            }
            val restrictions = buildList {
                val array = root.optJSONArray("appRestrictions") ?: org.json.JSONArray()
                for (i in 0 until array.length()) runCatching {
                    val item = array.getJSONObject(i)
                    add(AppRestriction(item.getString("packageName"), if (item.has("dailyMinutes") && !item.isNull("dailyMinutes")) item.getInt("dailyMinutes") else null, item.optBoolean("blocked", false)))
                }
            }
            ParentalControlConfig(screenTime, schedules, restrictions)
        }.getOrDefault(ParentalControlConfig())
}
