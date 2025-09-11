package app.integration

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Skeleton wiring for the IntegrationPass pipeline. This file defines the core
 * interfaces and a minimal enqueue helper following the brief from
 * braided-grain 9c2f. The real implementations live in platform specific
 * adapters. Workers are stubs so unit tests can be added incrementally.
 */

// ---- Receipts -----------------------------------------------------------------

data class Receipt(
    val id: String,
    val kind: Kind,
    val envelopeId: Long?,
    val createdAt: Instant,
    val message: String? = null,
) {
    enum class Kind {
        OkRecorded,
        OkUploaded,
        OkSkipped,
        OkCollapsedDuplicate,
        OkRepaired,
        OkExplained,
        permission_denied,
        empty_input,
        device_unavailable,
        storage_quota,
        auth_revoked,
        digest_mismatch,
        network_unavailable,
        retry_exhausted,
    }
}

// ---- Adapters -----------------------------------------------------------------

/** Abstraction over authentication state. */
interface AuthAdapter {
    val isReady: Boolean
}

/** Minimal Drive adapter used by workers. */
interface DriveAdapter {
    fun put(contentHash: String, bytes: ByteArray, meta: Map<String, String>): Boolean
    fun head(contentHash: String): DriveHead?
    data class DriveHead(val sha256: String, val size: Long)
}

// ---- Repository ---------------------------------------------------------------

interface EnvelopeRepository {
    suspend fun save(entity: app.db.EnvelopeEntity)
    suspend fun nextPendingUpload(): app.db.EnvelopeEntity?
    suspend fun markUploaded(id: Long)
}

// ---- ReceiptEmitter -----------------------------------------------------------

/** Central emitter responsible for persisting receipts and broadcasting them. */
interface ReceiptEmitter {
    suspend fun emit(receipt: Receipt)
}

// ---- Workers ------------------------------------------------------------------

/** Worker that uploads pending envelopes to Drive. */
class UploadWorker(
    private val context: Context,
    private val repo: EnvelopeRepository,
    private val drive: DriveAdapter,
    private val auth: AuthAdapter,
    private val receipts: ReceiptEmitter,
) {
    /** One run uploads a single pending envelope if available. */
    suspend fun runOnce() {
        if (!auth.isReady) {
            receipts.emit(
                Receipt(
                    id = newId(),
                    kind = Receipt.Kind.auth_revoked,
                    envelopeId = null,
                    createdAt = Instant.now(),
                    message = "Auth not ready"
                )
            )
            return
        }
        val next = repo.nextPendingUpload() ?: return
        // Real implementation would stream bytes and call DriveAdapter.put.
        val ok = drive.put(next.sha256, ByteArray(0), emptyMap())
        if (ok) {
            repo.markUploaded(next.id)
            receipts.emit(
                Receipt(
                    id = newId(),
                    kind = Receipt.Kind.OkUploaded,
                    envelopeId = next.id,
                    createdAt = Instant.now(),
                )
            )
        } else {
            receipts.emit(
                Receipt(
                    id = newId(),
                    kind = Receipt.Kind.network_unavailable,
                    envelopeId = next.id,
                    createdAt = Instant.now(),
                    message = "Upload failed",
                )
            )
        }
    }
}

/** Worker that checks Drive and database for parity. */
class ReconcileWorker(
    private val repo: EnvelopeRepository,
    private val drive: DriveAdapter,
    private val receipts: ReceiptEmitter,
) {
    suspend fun runOnce() {
        // Placeholder: real implementation would scan DB and Drive.
        receipts.emit(
            Receipt(
                id = newId(),
                kind = Receipt.Kind.OkExplained,
                envelopeId = null,
                createdAt = Instant.now(),
                message = "Reconcile not yet implemented",
            )
        )
    }
}

// ---- WorkManager helpers ------------------------------------------------------

/** Deterministically enqueue upload work for a given envelope. */
fun enqueueUpload(context: Context, envelope: app.db.EnvelopeEntity) {
    val day = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneOffset.UTC)
        .format(envelope.createdAt)
    val requestId = hashOf(envelope.sha256 + day)
    val work = OneTimeWorkRequestBuilder<androidx.work.Worker>()
        .addTag("upload")
        .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork(requestId, ExistingWorkPolicy.KEEP, work)
}

private fun newId(): String = java.util.UUID.randomUUID().toString()

private fun hashOf(str: String): String {
    val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(str.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
