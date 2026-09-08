package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class FirstLaunchRoleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                FirstLaunchRoleScreen(
                    onParent = {
                        FamilyStore(applicationContext).setAppMode(FamyrexAppMode.PARENT)
                        getSharedPreferences("famyrex_family", MODE_PRIVATE).edit().putBoolean("role_selected", true).apply()
                        startActivity(Intent(this, ParentFirstSetupActivity::class.java))
                        finish()
                    },
                    onChild = {
                        FamilyStore(applicationContext).setAppMode(FamyrexAppMode.SUPERVISED)
                        getSharedPreferences("famyrex_family", MODE_PRIVATE).edit().putBoolean("role_selected", true).apply()
                        startActivity(Intent(this, SupervisedOnboardingActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun FirstLaunchRoleScreen(onParent: () -> Unit, onChild: () -> Unit) {
    val navy = Color(0xFF071B3A)
    val blue = Color(0xFF0B5CAB)
    val cyan = Color(0xFF12D9E8)
    val surface = Color(0xFFF5F8FC)

    Box(
        Modifier.fillMaxSize().background(surface).navigationBarsPadding().padding(22.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = RoundedCornerShape(28.dp), color = navy, modifier = Modifier.size(92.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_famyrex_logo),
                    contentDescription = "Famyrex",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(9.dp).fillMaxSize()
                )
            }
            Text("Famyrex", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = navy)
            Text("Seguridad familiar inteligente", fontSize = 16.sp, color = Color(0xFF64748B))
            Spacer(Modifier.size(8.dp))
            Text(
                "Antes de empezar, dinos cómo vas a utilizar este dispositivo.",
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                color = Color(0xFF10233F)
            )

            RoleCard(
                title = "Soy padre, madre o adulto responsable",
                description = "Crearé mi familia, vincularé los dispositivos y gestionaré la protección.",
                icon = Icons.Default.FamilyRestroom,
                tint = blue,
                onClick = onParent
            )
            RoleCard(
                title = "Este es el dispositivo de mi hijo/a",
                description = "Vincularé este dispositivo a una familia mediante el código de Famyrex.",
                icon = Icons.Default.ChildCare,
                tint = cyan,
                onClick = onChild,
                outlined = true
            )
            Spacer(Modifier.size(4.dp))
            RowSecurityNote()
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    outlined: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10233F))
            Text(description, fontSize = 13.sp, color = Color(0xFF64748B))
            if (outlined) {
                OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Continuar como hijo/a", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Continuar como adulto", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RowSecurityNote() {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Security, null, tint = Color(0xFF2BE39A), modifier = Modifier.size(20.dp))
        Text("Tú decides qué funciones activar en cada dispositivo.", fontSize = 12.sp, color = Color(0xFF64748B), textAlign = TextAlign.Center)
    }
}
