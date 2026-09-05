package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class SupervisedOnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                JoinFamilyScreen(
                    context = applicationContext,
                    onJoined = {
                        startActivity(Intent(this, SupervisedDeviceActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
