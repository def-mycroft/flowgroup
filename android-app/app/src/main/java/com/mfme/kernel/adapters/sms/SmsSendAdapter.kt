package com.mfme.kernel.adapters.sms

import android.content.Context
import android.telephony.SmsManager
import com.mfme.kernel.data.KernelRepository
import com.mfme.kernel.data.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Simple SMS sending adapter that delegates persistence to the repository
 * before handing off to the platform's [SmsManager].
 */
class SmsSendAdapter(
    private val appContext: Context,
    private val repo: KernelRepository,
    private val smsManager: SmsManager = SmsManager.getDefault()
) {
    suspend fun send(phone: String, body: String): SaveResult = withContext(Dispatchers.IO) {
        require(phone.isNotBlank()) { "empty_phone" }
        require(body.isNotBlank()) { "empty_body" }
        val result = repo.saveSmsOut(phone, body, Instant.now())
        smsManager.sendTextMessage(phone, null, body, null, null)
        result
    }
}
