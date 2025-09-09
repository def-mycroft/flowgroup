package com.mfme.kernel.telemetry

import com.mfme.kernel.data.telemetry.ReceiptCode

object CanonicalReceiptJson {
  fun encode(
    version: Int = 1,
    adapter: String,
    code: ReceiptCode,
    tsUtcIso: String,
    spanId: String,
    envelopeId: Long?,
    envelopeSha256: String?,
    message: String?
  ): String {
    val sb = StringBuilder(128)
    sb.append('{')
    var first = true
    fun f(k: String, v: String) {
      if (!first) sb.append(',') else first = false
      sb.append('"').append(k).append('"').append(':').append(v)
    }
    fun s(x: String) = "\"" + x.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    f("version", "1")
    f("adapter", s(adapter))
    f("code", s(code.name))
    f("ts_utc", s(tsUtcIso))
    f("span_id", s(spanId))
    envelopeId?.let { f("envelope_id", it.toString()) }
    envelopeSha256?.let { f("envelope_sha256", s(it)) }
    message?.let { f("message", s(it)) }

    sb.append('}')
    return sb.toString()
  }
}
