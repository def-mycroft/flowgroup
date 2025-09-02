package app.zero.inlet.repo

import android.content.Context
import androidx.room.Room
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.zero.inlet.db.*

class EnvelopeRepositoryImpl(
    private val db: InletDatabase,
    private val filesDir: File
) : EnvelopeRepository {
    override fun observeNewest() = db.envelopeDao().observeNewest()

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
        val byteArray = text.toByteArray(Charsets.UTF_8)
        if (byteArray.size > 1_000_000) {
            return EnvelopeRepository.EnvelopeResult.Error(IllegalArgumentException("too large"))
        }
        return withContext(Dispatchers.IO) {
            val sha = sha256(byteArray)
            val start = System.nanoTime()
            db.receiptDao().insert(Receipt(envelopeSha = sha, status = "pending", created_at_utc = receivedAtUtc))
            try {
                val existing = db.envelopeDao().findBySha(sha)
                val envelope = if (existing != null) {
                    existing
                } else {
                    val dir = File(File(filesDir, "envelopes"), sha).apply { mkdirs() }
                    val payload = File(dir, "payload.txt")
                    if (!payload.exists()) {
                        payload.writeBytes(byteArray)
                    }
                    val env = Envelope(
                        sha256 = sha,
                        media_type = "text/plain",
                        filename = "$sha.txt",
                        bytes_path = payload.absolutePath,
                        text = text,
                        size_bytes = byteArray.size.toLong(),
                        created_at_utc = receivedAtUtc,
                        source_pkg = sourcePkg
                    )
                    db.envelopeDao().upsert(env)
                    env
                }
                val end = System.nanoTime()
                db.spanDao().insert(Span(envelopeSha = sha, start_nanos = start, end_nanos = end))
                db.receiptDao().insert(Receipt(envelopeSha = sha, status = "ok", created_at_utc = Instant.now()))
                if (existing != null) {
                    EnvelopeRepository.EnvelopeResult.Duplicate(envelope)
                } else {
                    EnvelopeRepository.EnvelopeResult.Success(envelope)
                }
            } catch (t: Throwable) {
                db.spanDao().insert(Span(envelopeSha = sha, start_nanos = start, end_nanos = System.nanoTime()))
                db.receiptDao().insert(Receipt(envelopeSha = sha, status = "error", created_at_utc = Instant.now()))
                val dir = File(File(filesDir, "envelopes"), sha)
                dir.deleteRecursively()
                EnvelopeRepository.EnvelopeResult.Error(t)
            }
        }
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
