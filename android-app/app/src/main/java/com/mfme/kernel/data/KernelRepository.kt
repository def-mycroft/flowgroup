package com.mfme.kernel.data

import kotlinx.coroutines.flow.Flow

sealed interface SaveResult {
    data class Success(val envelopeId: Long): SaveResult
    data class Duplicate(val envelopeId: Long): SaveResult
    data class Error(val cause: Throwable): SaveResult
}

interface KernelRepository {
    suspend fun saveEnvelope(env: Envelope): SaveResult
    fun observeReceipts(): Flow<List<Receipt>>
    fun observeEnvelopes(): Flow<List<Envelope>>
}
