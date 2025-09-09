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
}
