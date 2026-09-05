package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class ModeRouterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = FamilyStore(applicationContext)
        val target = if (store.appMode() == FamyrexAppMode.SUPERVISED) {
            if (store.devices().any { it.linkState == DeviceLinkState.LINKED }) {
                Intent(this, SupervisedDeviceActivity::class.java)
            } else {
                Intent(this, SupervisedOnboardingActivity::class.java)
            }
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(target)
        finish()
    }
}
