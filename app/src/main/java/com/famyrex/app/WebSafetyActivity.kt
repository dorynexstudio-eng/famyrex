package com.famyrex.app

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class WebSafetyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WebSafetyScreen() }
    }
}

@androidx.compose.runtime.Composable
private fun WebSafetyScreen() {
    val context = LocalContext.current
    val store = remember { WebSafetySettingsStore(context) }
    var settings by remember { mutableStateOf(store.load()) }
    var url by remember { mutableStateOf("https://example.com") }
    var blockedDomain by remember { mutableStateOf("") }
    var allowedDomain by remember { mutableStateOf("") }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun persist(next: WebSafetySettings) {
        settings = next
        store.save(next)
        webView?.let { WebSafetyController.configure(it, next) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Seguridad web", style = MaterialTheme.typography.headlineSmall)
        Text("Navegación aislada en WebView. Famyrex no intercepta el navegador externo ni lee mensajes de otras aplicaciones.")

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Protección web")
                    Switch(checked = settings.enabled, onCheckedChange = { persist(settings.copy(enabled = it)) })
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Safe Browsing para amenazas conocidas")
                    Switch(checked = settings.blockKnownThreats, onCheckedChange = { persist(settings.copy(blockKnownThreats = it)) })
                }
            }
            item {
                OutlinedTextField(
                    value = blockedDomain,
                    onValueChange = { blockedDomain = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dominio a bloquear") },
                    singleLine = true
                )
                Button(
                    enabled = blockedDomain.isNotBlank(),
                    onClick = {
                        persist(settings.copy(blockedDomains = settings.blockedDomains + blockedDomain.trim()))
                        blockedDomain = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Añadir a bloqueados") }
            }
            item {
                OutlinedTextField(
                    value = allowedDomain,
                    onValueChange = { allowedDomain = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dominio permitido") },
                    singleLine = true
                )
                Button(
                    enabled = allowedDomain.isNotBlank(),
                    onClick = {
                        persist(settings.copy(allowedDomains = settings.allowedDomains + allowedDomain.trim()))
                        allowedDomain = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Añadir a permitidos") }
            }
            item { Text("Bloqueados: ${settings.blockedDomains.joinToString().ifBlank { "ninguno" }}") }
            item { Text("Permitidos: ${settings.allowedDomains.joinToString().ifBlank { "ninguno" }}") }
            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dirección web") },
                    singleLine = true
                )
                Button(
                    enabled = url.isNotBlank(),
                    onClick = { webView?.loadUrl(url.trim()) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Abrir") }
            }
            item {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    factory = { viewContext ->
                        WebView(viewContext).also {
                            WebSafetyController.configure(it, settings)
                            webView = it
                            it.loadUrl(url)
                        }
                    },
                    update = { webView = it }
                )
            }
        }
    }
}
