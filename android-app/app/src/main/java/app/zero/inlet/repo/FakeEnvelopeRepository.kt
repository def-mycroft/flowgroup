package app.zero.inlet.repo

import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import app.zero.inlet.db.Envelope

class FakeEnvelopeRepository : EnvelopeRepository {
    private val items = mutableMapOf<String, Envelope>()
    private val flow = MutableStateFlow<List<Envelope>>(emptyList())
    private var idCounter = 1L

    override fun observeNewest(): Flow<List<Envelope>> = flow.asStateFlow()

    override suspend fun saveEnvelopes(
        items: List<String>,
        sourcePkg: String,
        receivedAtUtc: Instant
    ): List<EnvelopeRepository.EnvelopeResult> = items.map {
        saveEnvelope(it, null, sourcePkg, receivedAtUtc)
    }

    override suspend fun saveEnvelope(
        text: String,
        subject: String?,
        sourcePkg: String,
        receivedAtUtc: Instant
    ): EnvelopeRepository.EnvelopeResult {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val sha = bytes.sha()
        val existing = this.items[sha]
        return if (existing != null) {
            EnvelopeRepository.EnvelopeResult.Duplicate(existing)
        } else {
            val env = Envelope(
                id = idCounter++,
                sha256 = sha,
                media_type = "text/plain",
                filename = "$sha.txt",
                bytes_path = "",
                text = text,
                size_bytes = bytes.size.toLong(),
                created_at_utc = receivedAtUtc,
                source_pkg = sourcePkg
            )
            items[sha] = env
            flow.value = this.items.values.sortedByDescending { it.created_at_utc }
            EnvelopeRepository.EnvelopeResult.Success(env)
        }
    }

    private fun ByteArray.sha(): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(this)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
