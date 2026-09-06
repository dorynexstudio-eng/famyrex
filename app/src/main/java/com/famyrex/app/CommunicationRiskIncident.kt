package com.famyrex.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class RiskIncidentStatus { DETECTED, REVIEWED, CONFIRMED, DISMISSED, AUTO_DISMISSED, RESOLVED }

data class RiskReason(
    val code: String,
    val title: String,
    val detail: String
)

data class RiskIncidentStatusChange(
    val status: RiskIncidentStatus,
    val timestampMs: Long
)

data class CommunicationRiskIncident(
    val id: String,
    val createdAtMs: Long,
    val type: CommunicationRiskType,
    val confidence: RiskConfidence,
    val score: Int,
    val reasons: List<RiskReason>,
    val sourcePackage: String?,
    val direction: CommunicationDirection = CommunicationDirection.UNKNOWN,
    val status: RiskIncidentStatus = RiskIncidentStatus.DETECTED,
    val statusHistory: List<RiskIncidentStatusChange> = emptyList()
)

class CommunicationRiskIncidentStore(context: Context) {
    private val prefs = context.getSharedPreferences("famyrex_risk_incidents", Context.MODE_PRIVATE)

    fun save(incident: CommunicationRiskIncident) {
        val current = load().filterNot { it.id == incident.id }
        val array = JSONArray()
        (listOf(incident) + current).take(100).forEach { item ->
            array.put(JSONObject().apply {
                put("id", item.id)
                put("createdAtMs", item.createdAtMs)
                put("type", item.type.name)
                put("confidence", item.confidence.name)
                put("score", item.score)
                put("sourcePackage", item.sourcePackage ?: "")
                put("direction", item.direction.name)
                put("status", item.status.name)
                put("reasons", JSONArray().apply {
                    item.reasons.forEach { reason ->
                        put(JSONObject().apply {
                            put("code", reason.code)
                            put("title", reason.title)
                            put("detail", reason.detail)
                        })
                    }
                })
                put("statusHistory", JSONArray().apply {
                    item.statusHistory.forEach { change ->
                        put(JSONObject().apply {
                            put("status", change.status.name)
                            put("timestampMs", change.timestampMs)
                        })
                    }
                })
            })
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    fun updateStatus(id: String, status: RiskIncidentStatus): Boolean {
        val item = load().firstOrNull { it.id == id } ?: return false
        if (item.status == status) return true
        if (!isValidTransition(item.status, status)) return false

        val now = System.currentTimeMillis()
        val history = item.statusHistory + RiskIncidentStatusChange(status, now)
        save(item.copy(status = status, statusHistory = history.takeLast(20)))
        return true
    }

    private fun isValidTransition(
        current: RiskIncidentStatus,
        next: RiskIncidentStatus
    ): Boolean = when (current) {
        RiskIncidentStatus.DETECTED -> next == RiskIncidentStatus.REVIEWED ||
            next == RiskIncidentStatus.DISMISSED ||
            next == RiskIncidentStatus.AUTO_DISMISSED
        RiskIncidentStatus.REVIEWED -> next == RiskIncidentStatus.CONFIRMED ||
            next == RiskIncidentStatus.DISMISSED
        RiskIncidentStatus.CONFIRMED -> next == RiskIncidentStatus.RESOLVED
        RiskIncidentStatus.DISMISSED,
        RiskIncidentStatus.AUTO_DISMISSED,
        RiskIncidentStatus.RESOLVED -> false
    }

    fun load(): List<CommunicationRiskIncident> {
        val raw = prefs.getString("items", null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                runCatching {
                    val o = array.getJSONObject(i)
                    val reasonsJson = o.optJSONArray("reasons") ?: JSONArray()
                    val reasons = buildList {
                        for (j in 0 until reasonsJson.length()) {
                            runCatching {
                                val r = reasonsJson.getJSONObject(j)
                                add(RiskReason(r.getString("code"), r.getString("title"), r.getString("detail")))
                            }
                        }
                    }
                    val historyJson = o.optJSONArray("statusHistory") ?: JSONArray()
                    val history = buildList {
                        for (j in 0 until historyJson.length()) {
                            runCatching {
                                val h = historyJson.getJSONObject(j)
                                add(
                                    RiskIncidentStatusChange(
                                        status = RiskIncidentStatus.valueOf(h.getString("status")),
                                        timestampMs = h.getLong("timestampMs")
                                    )
                                )
                            }
                        }
                    }
                    add(
                        CommunicationRiskIncident(
                            id = o.getString("id"),
                            createdAtMs = o.getLong("createdAtMs"),
                            type = CommunicationRiskType.valueOf(o.getString("type")),
                            confidence = RiskConfidence.valueOf(o.getString("confidence")),
                            score = o.getInt("score"),
                            reasons = reasons,
                            sourcePackage = o.optString("sourcePackage").ifBlank { null },
                            direction = runCatching { CommunicationDirection.valueOf(o.optString("direction")) }
                                .getOrDefault(CommunicationDirection.UNKNOWN),
                            status = runCatching { RiskIncidentStatus.valueOf(o.optString("status")) }
                                .getOrDefault(RiskIncidentStatus.DETECTED),
                            statusHistory = history
                        )
                    )
                }
            }
        }
    }
}

object CommunicationRiskReasonCatalog {
    fun fromSignal(signal: CommunicationRiskSignal): RiskReason = when (signal.type) {
        CommunicationRiskType.GROOMING -> RiskReason("GROOMING_SIGNAL", "Posible contacto inapropiado", signal.reason)
        CommunicationRiskType.BULLYING -> RiskReason("BULLYING_SIGNAL", "Posible acoso", signal.reason)
        CommunicationRiskType.THREAT -> RiskReason("THREAT_SIGNAL", "Posible amenaza", signal.reason)
        CommunicationRiskType.SEXUAL_REQUEST -> RiskReason("SEXUAL_REQUEST", "Petición de contenido sexual", signal.reason)
        CommunicationRiskType.SECRET_KEEPING -> RiskReason("SECRET_KEEPING", "Petición de mantener un secreto", signal.reason)
        CommunicationRiskType.SELF_HARM -> RiskReason("SELF_HARM_SIGNAL", "Señal de posible malestar grave", signal.reason)
        CommunicationRiskType.SOCIAL_ISOLATION -> RiskReason("SOCIAL_ISOLATION", "Posible aislamiento social", signal.reason)
        CommunicationRiskType.SOCIAL_CONFLICT -> RiskReason("SOCIAL_CONFLICT", "Posible conflicto entre iguales", signal.reason)
    }
}
