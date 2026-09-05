package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class SupervisedDeviceActivity : ComponentActivity() {
    private var redirectingToOnboarding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!ensureSupervisedBinding()) return
        setContent { SupervisedDeviceScreen(applicationContext) }
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) ensureSupervisedBinding()
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
