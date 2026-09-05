package com.famyrex.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class ModeRouterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = if (FamilyStore(applicationContext).appMode() == FamyrexAppMode.SUPERVISED) {
            Intent(this, SupervisedDeviceActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(target)
        finish()
    }
}
