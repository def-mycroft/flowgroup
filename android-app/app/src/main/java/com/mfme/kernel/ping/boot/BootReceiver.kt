package com.mfme.kernel.ping.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mfme.kernel.ping.data.LocationPingPreferences
import com.mfme.kernel.ping.work.SmsLocationPingerWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule based on stored prefs
            val appScope = CoroutineScope(Dispatchers.IO)
            appScope.launch {
                val prefs = LocationPingPreferences(context)
                val model = prefs.flow.first()
                if (model.enabled && model.recipients.isNotEmpty()) {
                    SmsLocationPingerWorkScheduler.schedule(context, model.intervalMin)
                }
            }
        }
    }
}

