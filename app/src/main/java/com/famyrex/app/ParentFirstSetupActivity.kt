package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.lifecycleScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ParentFirstSetupActivity : ComponentActivity() {
    private lateinit var googleAuth: FamyrexGoogleAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleAuth = FamyrexGoogleAuth(applicationContext)

        val currentUser = googleAuth.currentUser()
        if (currentUser != null) {
            finishParentSetup(currentUser.uid, currentUser.displayName, currentUser.email)
            return
        }

        render(configured = googleAuth.isConfigured())
    }

    private fun render(configured: Boolean, loading: Boolean = false, errorMessage: String? = null) {
        setContent {
            MaterialTheme {
                ParentGoogleSignInScreen(
                    configured = configured,
                    loading = loading,
                    errorMessage = errorMessage,
                    onGoogle = { signInWithGoogle() },
                    onLater = { continueLocally() }
                )
            }
        }
    }

    private fun signInWithGoogle() {
        render(configured = googleAuth.isConfigured(), loading = true)
        googleAuth.signInAsParent(
            activity = this,
            scope = lifecycleScope,
            onSuccess = { user ->
                runOnUiThread { finishParentSetup(user.uid, user.displayName, user.email) }
            },
            onError = { message ->
                runOnUiThread { render(googleAuth.isConfigured(), errorMessage = message) }
            }
        )
    }

    private fun continueLocally() {
        val store = FamilyStore(applicationContext)
        val owner = store.ensureLocalOwner()
        store.setOwnerDisplayName(owner.displayName)
        getSharedPreferences("famyrex_family", MODE_PRIVATE)
            .edit().putBoolean("parent_setup_completed", true).apply()
        startActivity(Intent(this, PremiumMainActivity::class.java))
        finish()
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
        startActivity(Intent(this, PremiumMainActivity::class.java))
        finish()
    }
}

@Composable
private fun ParentGoogleSignInScreen(
    configured: Boolean,
    loading: Boolean = false,
    errorMessage: String? = null,
    onGoogle: () -> Unit,
    onLater: () -> Unit
) {
    val navy = Color(0xFF071B3A)
    val surface = Color(0xFFF5F8FC)

    Column(
        Modifier.fillMaxSize().background(surface).navigationBarsPadding().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.FamilyRestroom, null, tint = Color(0xFF0B5CAB), modifier = Modifier.size(58.dp))
        Spacer(Modifier.size(12.dp))
        Text("Tu cuenta de adulto", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = navy)
        Text(
            "Inicia sesión con Google para crear y administrar tu familia Famyrex.",
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
                Button(
                    onClick = onGoogle,
                    enabled = configured && !loading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (loading) "Conectando con Google…" else "Continuar con Google", fontWeight = FontWeight.Bold)
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
            "Más adelante podrás invitar a otro adulto a la misma familia con su propia cuenta de Google.",
            textAlign = TextAlign.Center,
            color = Color(0xFF64748B),
            fontSize = 12.sp
        )
    }
}
