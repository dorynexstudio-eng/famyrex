package com.famyrex.app

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Google identity for the adult/parent side of Famyrex.
 *
 * The child side deliberately does not use this class: children are linked by
 * the Famyrex invitation/code flow and receive an internal child profile.
 */
class FamyrexGoogleAuth(context: Context) {
    private val appContext = context.applicationContext

    fun currentUser() : com.google.firebase.auth.FirebaseUser? {
        if (FirebaseApp.getApps(appContext).isEmpty()) return null
        return FirebaseAuth.getInstance().currentUser
    }

    fun isConfigured(): Boolean = FirebaseApp.getApps(appContext).isNotEmpty() && webClientIdOrNull() != null

    fun signInAsParent(
        activity: Activity,
        scope: CoroutineScope,
        onSuccess: (com.google.firebase.auth.FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isConfigured()) {
            onError("Falta configurar Firebase para Famyrex. Añade el google-services.json del proyecto Firebase y habilita Google Sign-In.")
            return
        }

        val serverClientId = webClientIdOrNull()
        if (serverClientId == null) {
            onError("No se ha encontrado el Client ID web de Google. Completa la configuración de Google Sign-In en Firebase.")
            return
        }

        val credentialManager = CredentialManager.create(activity)

        scope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(activity, request)
                authenticateCredential(result.credential, onSuccess, onError)
            } catch (e: GetCredentialException) {
                // If there is no previously authorized account, show the normal
                // account chooser as the second attempt.
                try {
                    val fallbackOption = GetGoogleIdOption.Builder()
                        .setServerClientId(serverClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(fallbackOption)
                        .build()
                    val fallbackResult = credentialManager.getCredential(activity, fallbackRequest)
                    authenticateCredential(fallbackResult.credential, onSuccess, onError)
                } catch (fallback: Exception) {
                    onError(fallback.message?.takeIf { it.isNotBlank() } ?: "No se pudo iniciar sesión con Google.")
                }
            } catch (e: Exception) {
                onError(e.message?.takeIf { it.isNotBlank() } ?: "No se pudo iniciar sesión con Google.")
            }
        }
    }

    fun signOut(activity: Activity, scope: CoroutineScope, onComplete: () -> Unit = {}) {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            onComplete()
            return
        }
        FirebaseAuth.getInstance().signOut()
        scope.launch {
            runCatching {
                credentialManager(activity).clearCredentialState(
                    androidx.credentials.ClearCredentialStateRequest()
                )
            }
            onComplete()
        }
    }

    private fun authenticateCredential(
        credential: androidx.credentials.Credential,
        onSuccess: (com.google.firebase.auth.FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            onError("Google no devolvió una credencial compatible con Famyrex.")
            return
        }

        val googleCredential = try {
            GoogleIdTokenCredential.createFrom(credential.data)
        } catch (e: GoogleIdTokenParsingException) {
            onError("No se pudo interpretar la identidad de Google.")
            return
        }

        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        FirebaseAuth.getInstance()
            .signInWithCredential(firebaseCredential)
            .addOnSuccessListener { result ->
                result.user?.let(onSuccess) ?: onError("Firebase no devolvió el usuario autenticado.")
            }
            .addOnFailureListener { error ->
                onError(error.message?.takeIf { it.isNotBlank() } ?: "Firebase rechazó el inicio de sesión.")
            }
    }

    private fun credentialManager(activity: Activity): CredentialManager = CredentialManager.create(activity)

    private fun webClientIdOrNull(): String? {
        val resourceId = appContext.resources.getIdentifier(
            "default_web_client_id",
            "string",
            appContext.packageName
        )
        if (resourceId == 0) return null
        return runCatching { appContext.getString(resourceId) }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
