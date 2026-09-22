package com.famyrex.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val restored = SupervisedStateRestorer.restore(FamilyStore(appContext))
        if (restored) {
            FamyrexWorkScheduler.scheduleProtectionHealth(appContext)
        } else {
            // Do not recreate family-scoped workers/geofences after a failed or
            // stale restoration. The unlink/reset path has already cleared their data.
            FamyrexWorkScheduler.cancelFamilyScopedWork(appContext)
        }
    }
}
