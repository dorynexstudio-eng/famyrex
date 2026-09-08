package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class ParentFirstSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ParentFirstSetupScreen { name ->
                    val store = FamilyStore(applicationContext)
                    val owner = store.ensureLocalOwner()
                    store.setOwnerDisplayName(name.ifBlank { owner.displayName })
                    getSharedPreferences("famyrex_family", MODE_PRIVATE).edit().putBoolean("parent_setup_completed", true).apply()
                    startActivity(Intent(this, PremiumMainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ParentFirstSetupScreen(onContinue: (String) -> Unit) {
    var name by androidx.compose.runtime.remember { mutableStateOf("") }
    val navy = Color(0xFF071B3A)
    val surface = Color(0xFFF5F8FC)
    Column(
        Modifier.fillMaxSize().background(surface).navigationBarsPadding().padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.FamilyRestroom, null, tint = Color(0xFF0B5CAB), modifier = Modifier.size(58.dp))
        Spacer(Modifier.size(12.dp))
        Text("Crea tu familia", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = navy)
        Text("Empieza configurando el adulto responsable. Después podrás vincular los dispositivos de tu familia.", textAlign = TextAlign.Center, color = Color(0xFF64748B), fontSize = 15.sp)
        Spacer(Modifier.size(20.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tu nombre", fontWeight = FontWeight.Bold, color = navy)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    singleLine = true,
                    placeholder = { Text("Ej. María") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Button(onClick = { onContinue(name.trim()) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Entrar en Famyrex", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.size(14.dp))
        Text("Podrás completar la familia y vincular dispositivos desde Familia.", textAlign = TextAlign.Center, color = Color(0xFF64748B), fontSize = 12.sp)
    }
}
