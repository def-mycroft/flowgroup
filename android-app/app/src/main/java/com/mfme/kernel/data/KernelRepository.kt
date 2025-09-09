package com.mfme.kernel.data

import com.mfme.kernel.adapters.share.SharePayload
import kotlinx.coroutines.flow.Flow

interface KernelRepository {
    suspend fun saveEnvelope(env: Envelope): SaveResult
    suspend fun saveFromShare(payload: SharePayload): SaveResult
    suspend fun saveFromCamera(bytes: ByteArray, meta: Map<String, Any?>): SaveResult
    suspend fun saveFromMic(bytes: ByteArray, meta: Map<String, Any?>): SaveResult
    suspend fun saveFromFile(uri: android.net.Uri, meta: Map<String, Any?>): SaveResult
    suspend fun saveFromLocation(json: String): SaveResult
    suspend fun saveFromSensors(json: String): SaveResult
    fun observeReceipts(): Flow<List<Receipt>>
    fun observeEnvelopes(): Flow<List<Envelope>>
}
