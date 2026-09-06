package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme

class SupervisedDeviceActivity : ComponentActivity() {
    private var redirectingToOnboarding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ensureSupervisedBinding()) return
        renderScreen()
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing && ensureSupervisedBinding()) renderScreen()
    }

    private fun renderScreen() {
        val context = applicationContext
        val consent = AccessibilityConsentStore(context)
        setContent {
            MaterialTheme {
                if (!consent.isAccepted()) {
                    AccessibilityConsentScreen(
                        context = context,
                        onAccepted = { openAccessibilitySettings(context) }
                    )
                } else {
                    SupervisedDeviceScreen(context)
                }
            }
        }
    }

    private fun ensureSupervisedBinding(): Boolean {
        val store = FamilyStore(applicationContext)
        if (SupervisedStateRestorer.restore(store)) return true
        if (redirectingToOnboarding) return false

        redirectingToOnboarding = true
        startActivity(Intent(this, SupervisedOnboardingActivity::class.java))
        finish()
        return false
    }
}
