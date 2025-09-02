package app.zero.inlet.repo

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import app.zero.inlet.db.Envelope

interface EnvelopeRepository {
    sealed interface EnvelopeResult {
        data class Success(val envelope: Envelope) : EnvelopeResult
        data class Duplicate(val envelope: Envelope) : EnvelopeResult
        data class Error(val cause: Throwable) : EnvelopeResult
    }

    suspend fun saveEnvelope(
        text: String,
        subject: String?,
        sourcePkg: String,
        receivedAtUtc: Instant
    ): EnvelopeResult

    suspend fun saveEnvelopes(
        items: List<String>,
        sourcePkg: String,
        receivedAtUtc: Instant
    ): List<EnvelopeResult>

    fun observeNewest(): Flow<List<Envelope>>
}
