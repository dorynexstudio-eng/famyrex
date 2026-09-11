package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ParentFirstSetupActivity : ComponentActivity() {
    private lateinit var googleAuth: FamyrexGoogleAuth
    private val pairingService by lazy { FamyrexPairingService(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleAuth = FamyrexGoogleAuth(applicationContext)

        val inviteFromIntent = intent.getStringExtra(EXTRA_PARENT_INVITE).orEmpty().trim()
        val currentUser = googleAuth.currentUser()
        if (currentUser != null) {
            if (inviteFromIntent.isNotBlank()) {
                acceptInvitation(inviteFromIntent, currentUser.displayName, currentUser.email)
            } else {
                finishParentSetup(currentUser.uid, currentUser.displayName, currentUser.email)
            }
            return
        }

        render(configured = googleAuth.isConfigured(), initialInviteId = inviteFromIntent)
    }

    private fun render(configured: Boolean, loading: Boolean = false, errorMessage: String? = null, initialInviteId: String = "") {
        setContent {
            MaterialTheme {
                ParentGoogleSignInScreen(
                    configured = configured,
                    loading = loading,
                    errorMessage = errorMessage,
                    initialInviteId = initialInviteId,
                    onGoogle = { inviteId -> signInWithGoogle(inviteId) },
                    onLater = { continueLocally() }
                )
            }
        }
    }

    private fun signInWithGoogle(inviteId: String) {
        render(configured = googleAuth.isConfigured(), loading = true, initialInviteId = inviteId)
        googleAuth.signInAsParent(
            activity = this,
            scope = lifecycleScope,
            onSuccess = { user ->
                runOnUiThread {
                    if (inviteId.isNotBlank()) {
                        acceptInvitation(inviteId, user.displayName, user.email)
                    } else {
                        finishParentSetup(user.uid, user.displayName, user.email)
                    }
                }
            },
            onError = { message ->
                runOnUiThread { render(googleAuth.isConfigured(), errorMessage = message, initialInviteId = inviteId) }
            }
        )
    }

    private fun acceptInvitation(inviteId: String, displayName: String?, email: String?) {
        render(configured = googleAuth.isConfigured(), loading = true, initialInviteId = inviteId)
        val resolvedName = displayName?.trim().orEmpty()
            .ifBlank { email?.substringBefore('@').orEmpty().ifBlank { "Adulto autorizado" } }
        pairingService.acceptParentInvite(
            inviteId = inviteId,
            displayName = resolvedName,
            onSuccess = { familyId ->
                getSharedPreferences("famyrex_family", MODE_PRIVATE).edit()
                    .putBoolean("parent_setup_completed", true)
                    .putString("cloud_family_id", familyId)
                    .putString("google_parent_uid", googleAuth.currentUser()?.uid.orEmpty())
                    .putString("google_parent_email", email.orEmpty())
                    .apply()
                runOnUiThread { openPremium() }
            },
            onError = { message ->
                runOnUiThread { render(googleAuth.isConfigured(), errorMessage = message, initialInviteId = inviteId) }
            }
        )
    }

    private fun continueLocally() {
        val store = FamilyStore(applicationContext)
        val owner = store.ensureLocalOwner()
        store.setOwnerDisplayName(owner.displayName)
        getSharedPreferences("famyrex_family", MODE_PRIVATE)
            .edit().putBoolean("parent_setup_completed", true).apply()
        openPremium()
    }

    private fun finishParentSetup(uid: String, displayName: String?, email: String?) {
        val store = FamilyStore(applicationContext)
        val owner = store.ensureLocalOwner()
        val resolvedName = displayName?.trim().orEmpty()
            .ifBlank { email?.substringBefore('@').orEmpty().ifBlank { owner.displayName } }
        store.setOwnerDisplayName(resolvedName)

        getSharedPreferences("famyrex_family", MODE_PRIVATE).edit()
            .putBoolean("parent_setup_completed", true)
            .putString("google_parent_uid", uid)
            .putString("google_parent_email", email.orEmpty())
            .apply()

        FamyrexCloudFamilyRepository(applicationContext).ensureFamily(
            displayName = resolvedName,
            onSuccess = { familyId ->
                getSharedPreferences("famyrex_family", MODE_PRIVATE)
                    .edit().putString("cloud_family_id", familyId).apply()
                runOnUiThread { openPremium() }
            },
            onError = { message ->
                runOnUiThread { render(googleAuth.isConfigured(), errorMessage = message) }
            }
        )
    }

    private fun openPremium() {
        startActivity(Intent(this, PremiumMainActivity::class.java))
        finish()
    }

    companion object {
        const val EXTRA_PARENT_INVITE = "parent_invite_id"
    }
}

@Composable
private fun ParentGoogleSignInScreen(
    configured: Boolean,
    loading: Boolean = false,
    errorMessage: String? = null,
    initialInviteId: String = "",
    onGoogle: (inviteId: String) -> Unit,
    onLater: () -> Unit
) {
    val navy = Color(0xFF071B3A)
    val surface = Color(0xFFF5F8FC)
    var inviteId by remember(initialInviteId) { mutableStateOf(initialInviteId) }

    Column(
        Modifier.fillMaxSize().background(surface).navigationBarsPadding().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.FamilyRestroom, null, tint = Color(0xFF0B5CAB), modifier = Modifier.size(58.dp))
        Spacer(Modifier.size(12.dp))
        Text("Tu cuenta de adulto", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = navy)
        Text(
            "Inicia sesión con Google para crear o unirte a una familia Famyrex.",
            textAlign = TextAlign.Center,
            color = Color(0xFF64748B),
            fontSize = 15.sp
        )
        Spacer(Modifier.size(20.dp))

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.AccountCircle, null, tint = Color(0xFF0B5CAB), modifier = Modifier.size(42.dp))
                Text("Google", fontWeight = FontWeight.Bold, color = navy, fontSize = 19.sp)
                Text(
                    "Tu cuenta identifica al adulto dentro de Famyrex. Los dispositivos infantiles seguirán vinculándose mediante código y no necesitan iniciar sesión con Google.",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = inviteId,
                    onValueChange = { inviteId = it.filter { char -> char.isDigit() || char.lowercaseChar() in 'a'..'f' }.take(64) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Identificador de invitación (opcional)") },
                    supportingText = { Text("Si te ha invitado otro adulto, pega aquí el identificador que te ha entregado.") },
                    singleLine = true,
                    enabled = !loading
                )
                Button(
                    onClick = { onGoogle(inviteId.trim()) },
                    enabled = configured && !loading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (loading) "Conectando…" else if (inviteId.isBlank()) "Crear mi familia con Google" else "Aceptar invitación con Google", fontWeight = FontWeight.Bold)
                }

                if (!configured) {
                    Text(
                        "La conexión Firebase todavía no está configurada. Cuando añadamos el proyecto Firebase y su configuración, este botón quedará operativo.",
                        color = Color(0xFFB45309),
                        fontSize = 12.sp
                    )
                    OutlinedButton(onClick = onLater, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Text("Continuar con configuración local")
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(errorMessage, color = Color(0xFFB91C1C), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.size(14.dp))
        Text(
            "Una invitación de adulto es temporal y solo puede utilizarse una vez.",
            textAlign = TextAlign.Center,
            color = Color(0xFF64748B),
            fontSize = 12.sp
        )
    }
}
