package com.mfme.kernel.data

import android.content.Context
import com.mfme.kernel.adapters.share.SharePayload
import com.mfme.kernel.data.telemetry.ReceiptEntity
import com.mfme.kernel.data.telemetry.SpanDao
import com.mfme.kernel.telemetry.ErrorEmitter
import com.mfme.kernel.telemetry.ReceiptEmitter
import com.mfme.kernel.telemetry.TelemetryCode
import com.mfme.kernel.export.EnvelopeChainer
import com.mfme.kernel.util.sha256OfStream
import com.mfme.kernel.util.sha256OfUtf8
import com.mfme.kernel.util.toHex
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.time.Instant

class KernelRepositoryImpl(
    private val context: Context,
    private val db: KernelDatabase,
    private val io: CoroutineDispatcher,
    private val receiptEmitter: ReceiptEmitter,
    private val errorEmitter: ErrorEmitter,
    private val spanDao: SpanDao,
    private val chainer: EnvelopeChainer
) : KernelRepository {

    override fun observeReceipts(): Flow<List<ReceiptEntity>> =
        db.receiptDao().observeAll()

    override fun observeEnvelopes(): Flow<List<Envelope>> =
        db.envelopeDao().observeAll()

    private suspend fun persistEnvelope(env: Envelope): Pair<Long, Boolean> {
        val existing = db.envelopeDao().findBySha(env.sha256)
        return if (existing != null) existing.id to false else db.envelopeDao().insert(env) to true
    }

    private fun mapError(t: Throwable): TelemetryCode = when (t) {
        is SecurityException -> TelemetryCode.PermissionDenied
        is IllegalArgumentException -> TelemetryCode.EmptyInput
        is java.io.IOException -> TelemetryCode.DeviceUnavailable
        else -> TelemetryCode.Unknown()
    }

    private suspend fun saveWithAdapter(adapter: String, buildEnv: suspend () -> Envelope): SaveResult = withContext(io) {
        val span = receiptEmitter.begin(adapter)
        try {
            val env = buildEnv()
            val (id, isNew) = persistEnvelope(env)
            val code = if (isNew) TelemetryCode.OkNew else TelemetryCode.OkDuplicate
            chainer.chain(env)
            receiptEmitter.emitV2(
                ok = true,
                codeWire = code.wire,
                adapter = adapter,
                spanId = span.spanId,
                envelopeId = id,
                envelopeSha256 = env.sha256,
                message = null
            )
            spanDao.bindEnvelope(span.spanId, id, env.sha256)
            receiptEmitter.end(span)
            if (isNew) {
                // Schedule background upload for new envelopes; unique by sha256
                // In unit tests, WorkManager may not be initialized; swallow errors to avoid failing persistence.
                try {
                    com.mfme.kernel.work.UploadScheduler.enqueue(context, env.sha256)
                } catch (_: Throwable) {
                    // no-op in tests or when WorkManager is unavailable
                }
            }
            if (isNew) SaveResult.Success(id) else SaveResult.Duplicate(id)
        } catch (t: Throwable) {
            val code = mapError(t)
            errorEmitter.emit(
                adapter = adapter,
                code = code,
                spanId = span.spanId,
                envelopeId = null,
                envelopeSha256 = null,
                message = t.message
            )
            receiptEmitter.end(span)
            SaveResult.Error(t)
        }
    }

    override suspend fun saveEnvelope(env: Envelope): SaveResult =
        saveWithAdapter(env.sourcePkgRef) { env }

    override suspend fun saveFromShare(payload: SharePayload): SaveResult =
        saveWithAdapter("share") {
            val shaBytes = when (payload) {
                is SharePayload.Text -> sha256OfUtf8(payload.text)
                is SharePayload.Stream -> context.contentResolver.openInputStream(payload.uri)
                    ?.use { sha256OfStream(it) }
                    ?: throw IllegalArgumentException("stream_not_found")
            }
            val shaHex = toHex(shaBytes)
            // Persist payload for later upload
            when (payload) {
                is SharePayload.Text -> {
                    val bytes = payload.text.toByteArray(Charsets.UTF_8)
                    writePayload(shaHex, bytes)
                }
                is SharePayload.Stream -> {
                    val ext = guessExt(payload.mime, payload.displayName)
                    context.contentResolver.openInputStream(payload.uri)
                        ?.use { writePayload(shaHex, it, ext) }
                        ?: throw IllegalArgumentException("stream_not_found")
                }
            }
            Envelope(
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
        }

    override suspend fun saveFromCamera(bytes: ByteArray, meta: Map<String, Any?>): SaveResult =
        saveWithAdapter("camera") {
            if (bytes.size > MAX_BYTES) throw IllegalArgumentException("oversize")
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            Envelope(
                sha256 = toHex(sha),
                mime = "image/jpeg",
                text = null,
                filename = meta["filename"] as? String,
                sourcePkgRef = "camera",
                receivedAtUtc = (meta["tsUtc"] as? Instant) ?: Instant.now(),
                metaJson = metaToJson(meta)
            )
        }

    override suspend fun saveFromMic(bytes: ByteArray, meta: Map<String, Any?>): SaveResult =
        saveWithAdapter("mic") {
            if (bytes.size > MAX_BYTES) throw IllegalArgumentException("oversize")
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            Envelope(
                sha256 = toHex(sha),
                mime = "audio/wav",
                text = null,
                filename = meta["filename"] as? String,
                sourcePkgRef = "mic",
                receivedAtUtc = (meta["tsUtc"] as? Instant) ?: Instant.now(),
                metaJson = metaToJson(meta)
            )
        }

    override suspend fun saveFromFile(uri: android.net.Uri, meta: Map<String, Any?>): SaveResult =
        saveWithAdapter("files") {
            val resolver = context.contentResolver
            val size = resolver.openFileDescriptor(uri, "r")?.statSize ?: 0
            if (size > MAX_BYTES) throw IllegalArgumentException("oversize")
            val sha = resolver.openInputStream(uri)?.use { sha256OfStream(it) }
                ?: throw IllegalArgumentException("stream_not_found")
            val mime = resolver.getType(uri) ?: meta["mime"] as? String
            // Persist payload for later upload
            val ext = guessExt(mime, meta["filename"] as? String)
            resolver.openInputStream(uri)
                ?.use { writePayload(toHex(sha), it, ext) }
                ?: throw IllegalArgumentException("stream_not_found")
            Envelope(
                sha256 = toHex(sha),
                mime = mime,
                text = null,
                filename = meta["filename"] as? String,
                sourcePkgRef = "files",
                receivedAtUtc = (meta["tsUtc"] as? Instant) ?: Instant.now(),
                metaJson = metaToJson(meta)
            )
        }

    override suspend fun saveFromLocation(json: String): SaveResult =
        saveWithAdapter("location") {
            val sha = sha256OfUtf8(json)
            Envelope(
                sha256 = toHex(sha),
                mime = "application/json",
                text = json,
                filename = null,
                sourcePkgRef = "location",
                receivedAtUtc = Instant.now(),
                metaJson = json
            )
        }

    override suspend fun saveFromSensors(json: String): SaveResult =
        saveWithAdapter("sensors") {
            val sha = sha256OfUtf8(json)
            Envelope(
                sha256 = toHex(sha),
                mime = "application/json",
                text = json,
                filename = null,
                sourcePkgRef = "sensors",
                receivedAtUtc = Instant.now(),
                metaJson = json
            )
        }

    override suspend fun saveSmsOut(phone: String, body: String, sentAtUtc: Instant): SaveResult =
        saveWithAdapter("sms_out") {
            if (phone.isBlank() || body.isBlank()) throw IllegalArgumentException("empty_input")
            val bytes = body.toByteArray(Charsets.UTF_8)
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            writePayload(toHex(sha), bytes)
            Envelope(
                sha256 = toHex(sha),
                mime = "text/plain",
                text = body,
                filename = null,
                sourcePkgRef = "sms_out",
                receivedAtUtc = sentAtUtc,
                metaJson = org.json.JSONObject(mapOf("phone" to phone)).toString()
            )
        }

    override suspend fun ingestSmsIn(sender: String, body: String, receivedAtUtc: Instant): SaveResult =
        saveWithAdapter("sms_in") {
            if (sender.isBlank() || body.isBlank()) throw IllegalArgumentException("empty_input")
            val bytes = body.toByteArray(Charsets.UTF_8)
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes)
            writePayload(toHex(sha), bytes)
            val meta = org.json.JSONObject(
                mapOf(
                    "sender" to sender,
                    "body_sha256" to toHex(sha),
                    "receivedAtUtc" to receivedAtUtc.toString()
                )
            )
            Envelope(
                sha256 = toHex(sha),
                mime = "text/plain",
                text = body,
                filename = null,
                sourcePkgRef = "sms_in",
                receivedAtUtc = receivedAtUtc,
                metaJson = meta.toString()
            )
        }

    private fun writePayload(shaHex: String, bytes: ByteArray) {
        val dir = File(context.filesDir, "envelopes/$shaHex").apply { mkdirs() }
        val tmp = File(dir, "payload.txt.tmp")
        val out = File(dir, "payload.txt")
        FileOutputStream(tmp).use { fos ->
            fos.write(bytes)
            fos.fd.sync()
        }
        tmp.renameTo(out)
    }

    private fun writePayload(shaHex: String, input: InputStream, ext: String?) {
        val dir = File(context.filesDir, "envelopes/$shaHex").apply { mkdirs() }
        val safeExt = when {
            ext.isNullOrBlank() -> "bin"
            else -> ext.trim().lowercase().removePrefix(".")
        }
        val tmp = File(dir, "payload.$safeExt.tmp")
        val out = File(dir, "payload.$safeExt")
        FileOutputStream(tmp).use { fos ->
            input.copyTo(fos)
            fos.fd.sync()
        }
        tmp.renameTo(out)
    }

    private fun guessExt(mime: String?, displayName: String?): String? {
        val fromName = displayName?.substringAfterLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() && it.length <= 8 }
        if (!fromName.isNullOrBlank()) return fromName.lowercase()
        return when (mime?.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "video/mp4" -> "mp4"
            "audio/wav" -> "wav"
            "audio/mpeg" -> "mp3"
            "application/pdf" -> "pdf"
            "text/plain" -> "txt"
            "application/json" -> "json"
            else -> null
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
