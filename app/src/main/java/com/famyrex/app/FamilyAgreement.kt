package com.famyrex.app

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

/**
 * Local family agreement. It describes what the family decided together;
 * Famyrex observes and explains compliance but never applies the consequence.
 */
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

class FamilyAgreementStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_family_agreement", Context.MODE_PRIVATE)

    fun load(): FamilyAgreement? {
        val raw = prefs.getString("agreement", null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            FamilyAgreement(
                childProfileId = json.getString("childProfileId"),
                dailyMinutes = json.getInt("dailyMinutes"),
                goal = json.optString("goal"),
                consequence = json.optString("consequence"),
                reviewDate = json.optString("reviewDate"),
                active = json.optBoolean("active", true)
            )
        }.getOrNull()
    }

    fun save(agreement: FamilyAgreement) {
        require(agreement.dailyMinutes in 1..1440)
        JSONObject().apply {
            put("childProfileId", agreement.childProfileId)
            put("dailyMinutes", agreement.dailyMinutes)
            put("goal", agreement.goal.trim())
            put("consequence", agreement.consequence.trim())
            put("reviewDate", agreement.reviewDate)
            put("active", agreement.active)
        }.also { prefs.edit().putString("agreement", it.toString()).apply() }
    }

    fun clear() = prefs.edit().remove("agreement").apply()
}

object FamilyAgreementEngine {
    fun evaluate(
        agreement: FamilyAgreement?,
        usageMinutes: Long?,
        today: LocalDate = LocalDate.now()
    ): AgreementStatus {
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
                AgreementState.EXCEEDED, usageMinutes, 0L,
                "Hoy se ha superado el límite acordado por la familia."
            )
            usageMinutes >= (agreement.dailyMinutes * 0.85).toLong() -> AgreementStatus(
                AgreementState.ATTENTION, usageMinutes, remaining,
                "El uso de hoy se acerca al límite acordado."
            )
            else -> AgreementStatus(
                AgreementState.ON_TRACK, usageMinutes, remaining,
                "Hoy vas dentro de lo acordado."
            )
        }
    }
}
