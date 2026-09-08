package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class ModeRouterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = FamilyStore(applicationContext)
        val prefs = getSharedPreferences("famyrex_family", MODE_PRIVATE)
        val roleSelected = prefs.getBoolean("role_selected", false)
        val restoredSupervised = SupervisedStateRestorer.restore(store)

        val target = when {
            !roleSelected -> Intent(this, FirstLaunchRoleActivity::class.java)
            restoredSupervised -> Intent(this, SupervisedDeviceActivity::class.java)
            store.appMode() == FamyrexAppMode.SUPERVISED -> Intent(this, SupervisedOnboardingActivity::class.java)
            !prefs.getBoolean("parent_setup_completed", false) -> Intent(this, ParentFirstSetupActivity::class.java)
            else -> Intent(this, PremiumMainActivity::class.java)
        }
        startActivity(target)
        finish()
    }
}
