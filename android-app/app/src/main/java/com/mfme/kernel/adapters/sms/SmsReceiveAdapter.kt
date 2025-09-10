package com.mfme.kernel.adapters.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.mfme.kernel.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * BroadcastReceiver that ingests incoming SMS messages into the repository.
 */
class SmsReceiveAdapter : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val repo = ServiceLocator.repository(context.applicationContext)
        val scope = CoroutineScope(Dispatchers.IO)
        messages?.forEach { msg ->
            val sender = msg.displayOriginatingAddress ?: return@forEach
            val body = msg.displayMessageBody ?: return@forEach
            val ts = Instant.ofEpochMilli(msg.timestampMillis)
            scope.launch { repo.ingestSmsIn(sender, body, ts) }
        }
    }
}
