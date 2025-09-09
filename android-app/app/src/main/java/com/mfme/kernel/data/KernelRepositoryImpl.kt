package com.mfme.kernel.data

import android.content.Context
import com.mfme.kernel.adapters.share.SharePayload
import com.mfme.kernel.util.sha256OfStream
import com.mfme.kernel.util.sha256OfUtf8
import com.mfme.kernel.util.toHex
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant

class KernelRepositoryImpl(
    private val context: Context,
    private val db: KernelDatabase,
    private val io: CoroutineDispatcher
) : KernelRepository {

    private val receiptsFlow = MutableSharedFlow<List<Receipt>>(replay = 1)

    override fun observeReceipts(): Flow<List<Receipt>> =
        merge(db.receiptDao().observeAll(), receiptsFlow)

    override fun observeEnvelopes(): Flow<List<Envelope>> =
        db.envelopeDao().observeAll()

    override suspend fun saveEnvelope(env: Envelope): SaveResult = withContext(io) {
        val now = Instant.now()
        try {
            db.receiptDao().insert(
                Receipt(
                    envelopeSha256 = env.sha256,
                    status = "pending",
                    code = "initialized",
                    message = null,
                    tsUtc = now
                )
            )

            val existing = db.envelopeDao().findBySha(env.sha256)
            val id = if (existing != null) {
                db.receiptDao().insert(
                    Receipt(
                        envelopeSha256 = env.sha256,
                        status = "ok",
                        code = "duplicate_collapse",
                        message = null,
                        tsUtc = Instant.now()
                    )
                )
                return@withContext SaveResult.Duplicate(existing.id)
            } else {
                val newId = db.envelopeDao().insert(env)
                db.receiptDao().insert(
                    Receipt(
                        envelopeSha256 = env.sha256,
                        status = "ok",
                        code = "saved",
                        message = null,
                        tsUtc = Instant.now()
                    )
                )
                newId
            }
            SaveResult.Success(id)
        } catch (t: Throwable) {
            db.receiptDao().insert(
                Receipt(
                    envelopeSha256 = env.sha256,
                    status = "error",
                    code = "exception",
                    message = t.message,
                    tsUtc = Instant.now()
                )
            )
            SaveResult.Error(t)
        }
    }

    override suspend fun saveFromShare(payload: SharePayload): SaveResult = withContext(io) {
        try {
            val shaBytes = when (payload) {
                is SharePayload.Text -> sha256OfUtf8(payload.text)
                is SharePayload.Stream -> context.contentResolver.openInputStream(payload.uri)
                    ?.use { sha256OfStream(it) } ?: return@withContext SaveResult.Error(IllegalArgumentException("stream_not_found"))
            }
            val shaHex = toHex(shaBytes)
            val env = Envelope(
                sha256 = shaHex,
                mime = when (payload) {
                    is SharePayload.Text -> "text/plain"
                    is SharePayload.Stream -> payload.mime
                },
                text = (payload as? SharePayload.Text)?.text,
                filename = (payload as? SharePayload.Stream)?.displayName,
                sourcePkgRef = payload.sourceRef,
                receivedAtUtc = payload.receivedAtUtc,
                metaJson = null
            )
            saveEnvelope(env)
        } catch (t: Throwable) {
            SaveResult.Error(t)
        }
    }

    override suspend fun saveFromCamera(bytes: ByteArray, meta: Map<String, Any?>): SaveResult =
        withContext(io) {
            if (bytes.size > MAX_BYTES) return@withContext SaveResult.Error(IllegalArgumentException("oversize"))
            try {
                val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
                val env = Envelope(
                    sha256 = toHex(sha),
                    mime = "image/jpeg",
                    text = null,
                    filename = meta["filename"] as? String,
                    sourcePkgRef = "camera",
                    receivedAtUtc = (meta["tsUtc"] as? Instant) ?: Instant.now(),
                    metaJson = metaToJson(meta)
                )
                saveEnvelope(env)
            } catch (t: Throwable) {
                SaveResult.Error(t)
            }
        }

    override suspend fun saveFromMic(bytes: ByteArray, meta: Map<String, Any?>): SaveResult =
        withContext(io) {
            if (bytes.size > MAX_BYTES) return@withContext SaveResult.Error(IllegalArgumentException("oversize"))
            try {
                val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
                val env = Envelope(
                    sha256 = toHex(sha),
                    mime = "audio/wav",
                    text = null,
                    filename = meta["filename"] as? String,
                    sourcePkgRef = "mic",
                    receivedAtUtc = (meta["tsUtc"] as? Instant) ?: Instant.now(),
                    metaJson = metaToJson(meta)
                )
                saveEnvelope(env)
            } catch (t: Throwable) {
                SaveResult.Error(t)
            }
        }

    override suspend fun saveFromFile(uri: android.net.Uri, meta: Map<String, Any?>): SaveResult =
        withContext(io) {
            try {
                val resolver = context.contentResolver
                val size = resolver.openFileDescriptor(uri, "r")?.statSize ?: 0
                if (size > MAX_BYTES) return@withContext SaveResult.Error(IllegalArgumentException("oversize"))
                val sha = resolver.openInputStream(uri)?.use { sha256OfStream(it) }
                    ?: return@withContext SaveResult.Error(IllegalArgumentException("stream_not_found"))
                val mime = resolver.getType(uri) ?: meta["mime"] as? String
                val env = Envelope(
                    sha256 = toHex(sha),
                    mime = mime,
                    text = null,
                    filename = meta["filename"] as? String,
                    sourcePkgRef = "files",
                    receivedAtUtc = (meta["tsUtc"] as? Instant) ?: Instant.now(),
                    metaJson = metaToJson(meta)
                )
                saveEnvelope(env)
            } catch (t: Throwable) {
                SaveResult.Error(t)
            }
        }

    override suspend fun saveFromLocation(json: String): SaveResult = withContext(io) {
        try {
            val sha = sha256OfUtf8(json)
            val env = Envelope(
                sha256 = toHex(sha),
                mime = "application/json",
                text = json,
                filename = null,
                sourcePkgRef = "location",
                receivedAtUtc = Instant.now(),
                metaJson = json
            )
            saveEnvelope(env)
        } catch (t: Throwable) {
            SaveResult.Error(t)
        }
    }

    override suspend fun saveFromSensors(json: String): SaveResult = withContext(io) {
        try {
            val sha = sha256OfUtf8(json)
            val env = Envelope(
                sha256 = toHex(sha),
                mime = "application/json",
                text = json,
                filename = null,
                sourcePkgRef = "sensors",
                receivedAtUtc = Instant.now(),
                metaJson = json
            )
            saveEnvelope(env)
        } catch (t: Throwable) {
            SaveResult.Error(t)
        }
    }

    private fun metaToJson(meta: Map<String, Any?>): String? {
        if (meta.isEmpty()) return null
        val sanitized = meta.mapValues { (_, v) ->
            when (v) {
                is Instant -> v.toString()
                else -> v
            }
        }
        return org.json.JSONObject(sanitized).toString()
    }

    companion object {
        private const val MAX_BYTES: Int = 50 * 1024 * 1024 // 50MB
    }
}
