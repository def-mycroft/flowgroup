package com.mfme.kernel.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext
import java.time.Instant

class KernelRepositoryImpl(
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
}
