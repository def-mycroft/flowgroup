package com.mfme.kernel.telemetry

import com.mfme.kernel.data.telemetry.ReceiptEntity

class ErrorEmitter(private val receiptEmitter: ReceiptEmitter) {
  suspend fun emit(
    adapter: String,
    code: TelemetryCode,
    spanId: String,
    envelopeId: Long? = null,
    envelopeSha256: String? = null,
    message: String?
  ): ReceiptEntity {
    require(!code.ok)
    return receiptEmitter.emitV2(
      ok = false,
      codeWire = code.wire,
      adapter = adapter,
      spanId = spanId,
      envelopeId = envelopeId,
      envelopeSha256 = envelopeSha256,
      message = message
    )
  }
}
