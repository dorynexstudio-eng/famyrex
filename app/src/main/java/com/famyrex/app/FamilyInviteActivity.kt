package com.famyrex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

/**
 * Legacy entry point for famyrex://join links.
 * Child secrets are never accepted from an external deep link; pairing is
 * intentionally completed by QR scan or by entering code + key locally.
 */
class FamilyInviteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                JoinFamilyScreen(
                    context = this@FamilyInviteActivity,
                    onJoined = { finish() },
                    allowExternalInvite = false
                )
            }
        }
    }
}
