package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class ModeRouterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = FamilyStore(applicationContext)
        val restoredSupervised = SupervisedStateRestorer.restore(store)
        val target = if (restoredSupervised || store.appMode() == FamyrexAppMode.SUPERVISED) {
            if (restoredSupervised) {
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
