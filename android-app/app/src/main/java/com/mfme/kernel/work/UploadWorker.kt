package com.mfme.kernel.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.zero.core.cloud.DriveAdapter
import app.zero.core.cloud.UploadSpec
import com.mfme.kernel.data.KernelDatabase
import com.mfme.kernel.data.cloud.CloudBinding
import com.mfme.kernel.di.AppModule
import com.mfme.kernel.telemetry.ReceiptEmitter
import com.mfme.kernel.telemetry.TelemetryCode
import com.mfme.kernel.telemetry.TelemetryCode.OkDuplicateUpload
import com.mfme.kernel.telemetry.TelemetryCode.OkUploaded
import com.mfme.kernel.telemetry.TelemetryCode.OkAlreadyBound
import com.mfme.kernel.telemetry.TelemetryCode.ResolverError
import com.mfme.kernel.telemetry.TelemetryCode.DigestMismatch
import com.mfme.kernel.telemetry.TelemetryCode.NetworkBackoff
import com.mfme.kernel.telemetry.TelemetryCode.PermissionDeniedAuth
import com.mfme.kernel.telemetry.TelemetryCode.UnknownDriveError
import com.mfme.kernel.cloud.InMemoryDriveAdapter
import java.io.File
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.Instant

/**
 * Background uploader: streams payload by sha256 to Drive via [DriveAdapter], verifies,
 * then binds in `cloud_binding` and emits typed receipts.
 */
class UploadWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val sha = inputData.getString(KEY_SHA256)
        if (sha.isNullOrBlank()) return Result.success()

        val db: KernelDatabase = AppModule.provideDatabase(applicationContext)
        val emitter: ReceiptEmitter = AppModule.provideReceiptEmitter(applicationContext, db)
        val span = emitter.begin("uploader")
        val envelope = db.envelopeDao().findBySha(sha)
        if (envelope == null) {
            emitter.emitV2(false, ResolverError.wire, "uploader", span.spanId, null, sha, "envelope_missing")
            emitter.end(span)
            return Result.success()
        }
        val bindingDao = db.cloudBindingDao()
        val existing = bindingDao.findByEnvelopeId(envelope.id)
        if (existing != null) {
            emitter.emitV2(true, OkAlreadyBound.wire, "uploader", span.spanId, envelope.id, sha, null)
            emitter.end(span)
            return Result.success()
        }

        val payload = resolvePayloadFile(envelope.sha256)
        if (payload == null || !payload.exists()) {
            emitter.emitV2(false, ResolverError.wire, "uploader", span.spanId, envelope.id, sha, "payload_missing")
            emitter.end(span)
            return Result.success()
        }

        val drive: DriveAdapter = InMemoryDriveAdapter.instance
        // Resolve folder: mfme/ingest/YYYY/MM
        val received = envelope.receivedAtUtc
        val y = received.atZone(ZoneOffset.UTC).get(ChronoField.YEAR)
        val m = received.atZone(ZoneOffset.UTC).get(ChronoField.MONTH_OF_YEAR)
        val folderRef = drive.ensureFolder(listOf("mfme", "ingest", "%04d".format(y), "%02d".format(m))).getOrElse {
            emitter.emitV2(false, PermissionDeniedAuth.wire, "uploader", span.spanId, envelope.id, sha, it.message)
            emitter.end(span)
            return Result.success()
        }
        // Collapse duplicates by sha256 in same folder
        val dup = drive.findBySha256(sha, folderRef.id).getOrNull()
        if (dup != null) {
            val bind = CloudBinding(envelope.id, dup.id, Instant.now(), dup.md5, dup.bytes)
            bindingDao.upsert(bind)
            emitter.emitV2(true, OkDuplicateUpload.wire, "uploader", span.spanId, envelope.id, sha, null)
            emitter.end(span)
            return Result.success()
        }

        val bytes = payload.length()
        val ext = payload.extensionIfAny()
        val mime = envelope.mime ?: "application/octet-stream"
        val spec = UploadSpec(
            folderId = folderRef.id,
            sha256 = sha,
            bytes = bytes,
            mime = mime,
            ext = ext,
            receivedAtUtc = envelope.receivedAtUtc.toString(),
            idempotencyKey = sha
        )
        val uploaded = drive.uploadResumable(spec).getOrElse { err ->
            val code = when (err) {
                is SecurityException -> PermissionDeniedAuth.wire
                is java.io.IOException -> NetworkBackoff.wire
                else -> UnknownDriveError().wire
            }
            emitter.emitV2(false, code, "uploader", span.spanId, envelope.id, sha, err.message)
            emitter.end(span)
            return Result.success()
        }
        val meta = drive.getMetadata(uploaded.id).getOrNull()
        if (meta == null || meta.bytes != bytes) {
            emitter.emitV2(false, DigestMismatch.wire, "uploader", span.spanId, envelope.id, sha, "bytes_mismatch")
            emitter.end(span)
            return Result.success()
        }

        val binding = CloudBinding(envelope.id, uploaded.id, Instant.now(), meta.md5, meta.bytes)
        bindingDao.upsert(binding)
        emitter.emitV2(true, OkUploaded.wire, "uploader", span.spanId, envelope.id, sha, null)
        emitter.end(span)
        return Result.success()
    }

    private fun resolvePayloadFile(sha: String): File? {
        val dir = File(applicationContext.filesDir, "envelopes/$sha")
        if (!dir.exists()) return null
        val files = dir.listFiles() ?: return null
        // Prefer any payload.* over others; avoid .tmp
        val chosen = files.firstOrNull { it.isFile && it.name.startsWith("payload.") && !it.name.endsWith(".tmp") }
            ?: files.firstOrNull { it.isFile && it.name == "payload.txt" }
        return chosen
    }

    private fun File.extensionIfAny(): String? {
        val name = this.name
        val idx = name.lastIndexOf('.')
        if (idx <= 0 || idx == name.length - 1) return null
        return name.substring(idx + 1)
    }

    companion object {
        const val KEY_SHA256 = "sha256"
    }
}
