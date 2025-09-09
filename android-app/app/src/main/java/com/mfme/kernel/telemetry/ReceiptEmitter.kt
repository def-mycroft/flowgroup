package com.mfme.kernel.telemetry

import com.mfme.kernel.data.telemetry.ReceiptDao
import com.mfme.kernel.data.telemetry.ReceiptEntity
import com.mfme.kernel.data.telemetry.SpanDao
import com.mfme.kernel.data.telemetry.SpanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

class ReceiptEmitter(
  private val receiptDao: ReceiptDao,
  private val spanDao: SpanDao,
  private val sink: NdjsonSink,
  private val clockUtc: () -> Instant = { Instant.now() }
) {

  suspend fun begin(adapter: String): SpanEntity = withContext(Dispatchers.IO) {
    val spanId = randomSpanId()
    val now = System.nanoTime()
    val span = SpanEntity(spanId = spanId, adapter = adapter, startNanos = now, endNanos = now, envelopeId = null, envelopeSha256 = null)
    spanDao.insert(span)
    span
  }

  suspend fun end(span: SpanEntity) = withContext(Dispatchers.IO) {
    val ended = span.copy(endNanos = System.nanoTime())
    spanDao.insert(ended)
  }

  suspend fun emitV2(
    ok: Boolean,
    codeWire: String,
    adapter: String,
    spanId: String,
    envelopeId: Long?,
    envelopeSha256: String?,
    message: String?
  ): ReceiptEntity = withContext(Dispatchers.IO) {
    val tsIso = clockUtc().toString()
    val json = CanonicalReceiptJson.encodeV2(
      ok = ok,
      codeWire = codeWire,
      tsUtcIso = tsIso,
      adapter = adapter,
      spanId = spanId,
      envelopeId = envelopeId,
      envelopeSha256 = envelopeSha256,
      message = message
    )
    val sha = Hashing.sha256Hex(json.toByteArray(Charsets.UTF_8))
    val entity = ReceiptEntity(
      ok = ok,
      code = codeWire,
      adapter = adapter,
      tsUtcIso = tsIso,
      envelopeId = envelopeId,
      envelopeSha256 = envelopeSha256,
      message = message,
      spanId = spanId,
      receiptSha256 = sha
    )
    val id = receiptDao.insert(entity)
    sink.writeLine(tsIso, sha, json)
    entity.copy(id = id)
  }

  private fun randomSpanId(): String {
    val b = ByteArray(12); SecureRandom().nextBytes(b)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b)
  }
}
