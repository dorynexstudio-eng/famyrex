package com.famyrex.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        SupervisedStateRestorer.restore(FamilyStore(appContext))
        FamyrexWorkScheduler.scheduleProtectionHealth(appContext)
    }
}
