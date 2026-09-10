package com.famyrex.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import java.io.File

object FamyrexAccountDeletion {
    fun delete(context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        FirebaseFunctions.getInstance("europe-west1")
            .getHttpsCallable("deleteMyAccount")
            .call(emptyMap<String, Any>())
            .addOnSuccessListener {
                runCatching { FamyrexLocalDataCleaner.clear(context) }
                    .onFailure {
                        onError("La cuenta se eliminó en el servidor, pero no se pudo limpiar todo el almacenamiento local.")
                        return@addOnSuccessListener
                    }
                FirebaseAuth.getInstance().signOut()
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError(error.message?.takeIf { it.isNotBlank() } ?: "No se pudo eliminar la cuenta. Inténtalo de nuevo.")
            }
    }
}

private object FamyrexLocalDataCleaner {
    fun clear(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            File(appContext.applicationInfo.dataDir, "shared_prefs")
                .listFiles()
                ?.filter { it.extension.equals("xml", ignoreCase = true) }
                ?.forEach { appContext.deleteSharedPreferences(it.nameWithoutExtension) }
        }
        runCatching {
            File(appContext.applicationInfo.dataDir, "databases")
                .listFiles()
                ?.forEach { it.delete() }
        }
        runCatching { androidx.work.WorkManager.getInstance(appContext).cancelAllWork() }
    }
}
