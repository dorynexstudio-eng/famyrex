package com.famyrex.app

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

        cachedFamilyId()?.let { onSuccess(it); return }

        val db = FirebaseFirestore.getInstance()
        db.collection("families")
            .whereEqualTo("ownerUid", user.uid)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val existing = snapshot.documents.firstOrNull()
                if (existing != null) {
                    bootstrapParentMember(db, existing.id, user.uid, displayName, onSuccess, onError)
                } else {
                    val familyId = "family-${UUID.randomUUID().toString().replace("-", "").take(20)}"
                    val familyRef = db.collection("families").document(familyId)
                    familyRef.set(
                        mapOf(
                            "ownerUid" to user.uid,
                            "name" to "Familia Famyrex",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).addOnSuccessListener {
                        bootstrapParentMember(db, familyId, user.uid, displayName, onSuccess, onError)
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
}
