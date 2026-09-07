package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class FamilyAgreement(
    val childProfileId: String,
    val dailyMinutes: Int,
    val goal: String,
    val consequence: String,
    val reviewDate: String,
    val active: Boolean = true
)

data class AgreementStatus(
    val state: AgreementState,
    val usedMinutes: Long,
    val remainingMinutes: Long?,
    val message: String
)

enum class AgreementState { ON_TRACK, ATTENTION, EXCEEDED, INSUFFICIENT_DATA }

internal fun isValidFamilyAgreementDate(value: String): Boolean = runCatching {
    LocalDate.parse(value)
    true
}.getOrDefault(false)

class FamilyAgreementStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_family_agreement", Context.MODE_PRIVATE)

    fun load(childProfileId: String): FamilyAgreement? = loadAll().firstOrNull { it.childProfileId == childProfileId }
    fun load(): FamilyAgreement? = loadAll().firstOrNull()

    fun loadAll(): List<FamilyAgreement> {
        prefs.getString("agreements", null)?.let { raw ->
            runCatching { return parseArray(JSONArray(raw)) }
        }
        val legacy = prefs.getString("agreement", null) ?: return emptyList()
        return runCatching { listOf(parseAgreement(JSONObject(legacy))) }.getOrDefault(emptyList())
    }

    fun save(agreement: FamilyAgreement) {
        require(agreement.childProfileId.isNotBlank())
        require(agreement.dailyMinutes in 1..1440)
        require(isValidFamilyAgreementDate(agreement.reviewDate))
        val array = JSONArray()
        (loadAll().filterNot { it.childProfileId == agreement.childProfileId } + agreement).forEach { array.put(toJson(it)) }
        prefs.edit().putString("agreements", array.toString()).remove("agreement").apply()
    }

    fun clear(childProfileId: String) {
        val array = JSONArray()
        loadAll().filterNot { it.childProfileId == childProfileId }.forEach { array.put(toJson(it)) }
        prefs.edit().putString("agreements", array.toString()).apply()
    }

    fun clear() = prefs.edit().remove("agreement").remove("agreements").apply()

    private fun toJson(a: FamilyAgreement) = JSONObject().apply {
        put("childProfileId", a.childProfileId)
        put("dailyMinutes", a.dailyMinutes)
        put("goal", a.goal.trim())
        put("consequence", a.consequence.trim())
        put("reviewDate", a.reviewDate)
        put("active", a.active)
    }

    private fun parseArray(array: JSONArray): List<FamilyAgreement> = buildList {
        for (i in 0 until array.length()) {
            runCatching { add(parseAgreement(array.getJSONObject(i))) }
        }
    }

    private fun parseAgreement(j: JSONObject) = FamilyAgreement(
        childProfileId = j.getString("childProfileId").trim(),
        dailyMinutes = j.getInt("dailyMinutes"),
        goal = j.optString("goal"),
        consequence = j.optString("consequence"),
        reviewDate = j.optString("reviewDate"),
        active = j.optBoolean("active", true)
    )
}

object FamilyAgreementEngine {
    fun evaluate(agreement: FamilyAgreement?, usageMinutes: Long?): AgreementStatus {
        if (agreement == null || !agreement.active || usageMinutes == null) {
            return AgreementStatus(
                AgreementState.INSUFFICIENT_DATA,
                usageMinutes ?: 0L,
                null,
                "No hay datos suficientes para valorar el cumplimiento del acuerdo."
            )
        }

        val remaining = (agreement.dailyMinutes - usageMinutes).coerceAtLeast(0L)
        return when {
            usageMinutes > agreement.dailyMinutes -> AgreementStatus(
                AgreementState.EXCEEDED,
                usageMinutes,
                0L,
                "Hoy se ha superado el límite acordado por la familia."
            )
            usageMinutes >= (agreement.dailyMinutes * 0.85).toLong() -> AgreementStatus(
                AgreementState.ATTENTION,
                usageMinutes,
                remaining,
                "El uso de hoy se acerca al límite acordado."
            )
            else -> AgreementStatus(
                AgreementState.ON_TRACK,
                usageMinutes,
                remaining,
                "Hoy vas dentro de lo acordado."
            )
        }
    }
}
