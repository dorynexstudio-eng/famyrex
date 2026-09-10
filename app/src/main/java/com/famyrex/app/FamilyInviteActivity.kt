package com.famyrex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

/** Entry point for famyrex://join links shared by a parent. */
class FamilyInviteActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                JoinFamilyScreen(
                    context = this@FamilyInviteActivity,
                    onJoined = { finish() }
                )
            }
        }
    }
}
