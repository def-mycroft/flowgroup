package com.mfme.kernel.data

import java.time.Instant

interface KernelRepositorySms {
    suspend fun saveSmsOut(phone: String, body: String, sentAtUtc: Instant): SaveResult
    suspend fun ingestSmsIn(sender: String, body: String, receivedAtUtc: Instant): SaveResult
}
