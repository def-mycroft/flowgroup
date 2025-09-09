package com.mfme.kernel.data

import com.mfme.kernel.adapters.share.SharePayload
import kotlinx.coroutines.flow.Flow

interface KernelRepository {
    suspend fun saveEnvelope(env: Envelope): SaveResult
    suspend fun saveFromShare(payload: SharePayload): SaveResult
    fun observeReceipts(): Flow<List<Receipt>>
    fun observeEnvelopes(): Flow<List<Envelope>>
}
