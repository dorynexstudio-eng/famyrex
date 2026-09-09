package com.famyrex.app

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Small cloud bridge for the child's latest location. Raw location is kept to one latest point. */
class FamilyLocationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = FirebaseFirestore.getInstance()

    fun publishChildLocation(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        capturedAtMs: Long,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val task = publishChildLocationTask(latitude, longitude, accuracyMeters, capturedAtMs)
        if (task == null) {
            onComplete(false)
            return
        }
        task.addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun publishChildLocationTask(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        capturedAtMs: Long
    ): Task<Void>? {
        val identity = FamilyStore(appContext).verifiedFamilyIdentity() ?: return null
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        if (!user.isAnonymous) return null

        return db.document("families/${identity.familyId}/members/${user.uid}/location/latest")
            .set(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "accuracyMeters" to accuracyMeters.toDouble().coerceAtLeast(0.0),
                    "capturedAtMs" to capturedAtMs
                )
            )
    }

    fun loadChildLocation(
        childMemberId: String,
        onSuccess: (FamilyChildLocation?) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        val familyId = FamilyStore(appContext).verifiedFamilyIdentity()?.familyId
            ?: run { onSuccess(null); return }

        db.collection("families/$familyId/members")
            .whereEqualTo("memberId", childMemberId)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val childUid = snapshot.documents.firstOrNull()?.id
                if (childUid == null) {
                    onSuccess(null)
                    return@addOnSuccessListener
                }
                db.document("families/$familyId/members/$childUid/location/latest")
                    .get()
                    .addOnSuccessListener { doc ->
                        if (!doc.exists()) {
                            onSuccess(null)
                            return@addOnSuccessListener
                        }
                        val latitude = doc.getDouble("latitude")
                        val longitude = doc.getDouble("longitude")
                        val accuracy = doc.getDouble("accuracyMeters")
                        val capturedAt = doc.getLong("capturedAtMs")
                        if (latitude == null || longitude == null || capturedAt == null) {
                            onSuccess(null)
                        } else {
                            onSuccess(FamilyChildLocation(latitude, longitude, accuracy?.toFloat() ?: 0f, capturedAt))
                        }
                    }
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }
}

data class FamilyChildLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val capturedAtMs: Long
)
