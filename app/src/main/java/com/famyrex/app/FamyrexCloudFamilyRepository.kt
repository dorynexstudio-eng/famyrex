package com.famyrex.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import java.util.UUID

/** Creates/restores the adult's Famyrex family in Firestore. */
class FamyrexCloudFamilyRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("famyrex_family", Context.MODE_PRIVATE)

    fun cachedFamilyId(): String? = prefs.getString("cloud_family_id", null)?.trim()?.takeIf { it.isNotBlank() }

    fun ensureFamily(
        displayName: String,
        onSuccess: (familyId: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado.")
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) {
            onError("La familia necesita una cuenta de adulto autenticada con Google.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        cachedFamilyId()?.let { cachedId ->
            db.document("families/$cachedId/members/${user.uid}")
                .get()
                .addOnSuccessListener { member ->
                    if (member.exists() && member.data()?.get("role") == "parent") {
                        onSuccess(cachedId)
                    } else {
                        prefs.edit().remove("cloud_family_id").apply()
                        findOwnedFamily(db, user.uid, displayName, onSuccess, onError)
                    }
                }
                .addOnFailureListener { onError(it.message ?: "No se pudo validar la familia guardada.") }
            return
        }

        findOwnedFamily(db, user.uid, displayName, onSuccess, onError)
    }

    /** Recovers the complete safe cloud mirror through the trusted callable backend. */
    fun syncFamilySnapshot(
        familyId: String,
        onSuccess: (FamilySnapshot) -> Unit,
        onError: (String) -> Unit
    ) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onError("Firebase todavía no está configurado.")
            return
        }
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null || user.isAnonymous) {
            onError("Inicia sesión con Google para recuperar la familia.")
            return
        }
        if (familyId.isBlank()) {
            onError("La identidad de la familia está incompleta.")
            return
        }
        FirebaseFunctions.getInstance(FirebaseApp.getInstance(), "europe-west1")
            .getHttpsCallable("getSecureFamilySnapshot")
            .call(hashMapOf("familyId" to familyId))
            .addOnSuccessListener { result ->
                val data = result.data as? Map<*, *>
                val resolvedFamilyId = data?.string("familyId")
                if (resolvedFamilyId.isNullOrBlank()) {
                    onError("Firebase no devolvió una familia válida.")
                    return@addOnSuccessListener
                }
                val adults = data.list("adults").mapNotNull { it as? Map<*, *> }.mapNotNull { it.toAdultSnapshot() }
                val children = data.list("children").mapNotNull { it as? Map<*, *> }.mapNotNull { it.toChildSnapshot() }
                val devices = data.list("devices").mapNotNull { it as? Map<*, *> }.mapNotNull { it.toDeviceSnapshot() }
                onSuccess(
                    FamilySnapshot(
                        familyId = resolvedFamilyId,
                        ownerUid = data.string("ownerUid").orEmpty(),
                        name = data.string("name") ?: "Familia Famyrex",
                        adults = adults,
                        children = children,
                        devices = devices
                    )
                )
            }
            .addOnFailureListener { onError(it.message ?: "No se pudo recuperar la familia desde el servidor.") }
    }

    private fun findOwnedFamily(
        db: FirebaseFirestore,
        uid: String,
        displayName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("families")
            .whereEqualTo("ownerUid", uid)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val existing = snapshot.documents.firstOrNull()
                if (existing != null) {
                    bootstrapParentMember(db, existing.id, uid, displayName, onSuccess, onError)
                } else {
                    val familyId = "family-${UUID.randomUUID().toString().replace("-", "").take(20)}"
                    val familyRef = db.collection("families").document(familyId)
                    familyRef.set(
                        mapOf(
                            "ownerUid" to uid,
                            "name" to "Familia Famyrex",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).addOnSuccessListener {
                        bootstrapParentMember(db, familyId, uid, displayName, onSuccess, onError)
                    }.addOnFailureListener { onError(it.message ?: "No se pudo crear la familia Famyrex.") }
                }
            }
            .addOnFailureListener { onError(it.message ?: "No se pudo recuperar la familia Famyrex.") }
    }

    private fun bootstrapParentMember(
        db: FirebaseFirestore,
        familyId: String,
        uid: String,
        displayName: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        db.document("families/$familyId/members/$uid")
            .set(
                mapOf(
                    "uid" to uid,
                    "role" to "parent",
                    "displayName" to displayName.take(60),
                    "status" to "active",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                prefs.edit().putString("cloud_family_id", familyId).apply()
                onSuccess(familyId)
            }
            .addOnFailureListener { onError(it.message ?: "No se pudo registrar el adulto en la familia.") }
    }

    private fun Map<*, *>.string(key: String): String? = (this[key] as? String)?.trim()?.takeIf { it.isNotBlank() }
    private fun Map<*, *>.list(key: String): List<Any?> = this[key] as? List<Any?> ?: emptyList()

    private fun Map<*, *>.toAdultSnapshot(): FamilyAdultSnapshot? {
        val uid = string("uid") ?: return null
        return FamilyAdultSnapshot(uid, string("displayName") ?: "Adulto autorizado", string("status") ?: "active", number("createdAtMs"))
    }

    private fun Map<*, *>.toChildSnapshot(): FamilyChildSnapshot? {
        val memberId = string("memberId") ?: return null
        return FamilyChildSnapshot(
            memberId = memberId,
            displayName = string("displayName") ?: "Perfil infantil",
            ageRange = string("ageRange").orEmpty(),
            status = string("status") ?: "pending",
            linkedUid = string("linkedUid"),
            linkedDeviceId = string("linkedDeviceId"),
            createdAtMs = number("createdAtMs")
        )
    }

    private fun Map<*, *>.toDeviceSnapshot(): FamilyDeviceSnapshot? {
        val uid = string("uid") ?: return null
        return FamilyDeviceSnapshot(
            uid = uid,
            role = string("role") ?: "child",
            famyrexMemberId = string("famyrexMemberId"),
            famyrexDeviceId = string("famyrexDeviceId"),
            linkedAtMs = number("linkedAtMs")
        )
    }

    private fun Map<*, *>.number(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L
}

data class FamilySnapshot(
    val familyId: String,
    val ownerUid: String,
    val name: String,
    val adults: List<FamilyAdultSnapshot>,
    val children: List<FamilyChildSnapshot>,
    val devices: List<FamilyDeviceSnapshot>
)

data class FamilyAdultSnapshot(val uid: String, val displayName: String, val status: String, val createdAtMs: Long)
data class FamilyChildSnapshot(val memberId: String, val displayName: String, val ageRange: String, val status: String, val linkedUid: String?, val linkedDeviceId: String?, val createdAtMs: Long)
data class FamilyDeviceSnapshot(val uid: String, val role: String, val famyrexMemberId: String?, val famyrexDeviceId: String?, val linkedAtMs: Long)
