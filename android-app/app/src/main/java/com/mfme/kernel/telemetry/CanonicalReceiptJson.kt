package com.mfme.kernel.telemetry

object CanonicalReceiptJson {
  fun encodeV2(
    ok: Boolean,
    codeWire: String,
    tsUtcIso: String,
    adapter: String,
    spanId: String,
    envelopeId: Long?,
    envelopeSha256: String?,
    message: String?
  ): String {
    val sb = StringBuilder(160)
    var first = true
    fun add(k: String, v: String) {
      if (!first) sb.append(',') else first = false
      sb.append('"').append(k).append('"').append(':').append(v)
    }
    fun s(x: String) = "\"" + x.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    sb.append('{')
    add("version", "2")
    add("ok", if (ok) "true" else "false")
    add("code", s(codeWire))
    add("ts_utc", s(tsUtcIso))
    add("adapter", s(adapter))
    add("span_id", s(spanId))
    envelopeId?.let { add("envelope_id", it.toString()) }
    envelopeSha256?.let { add("envelope_sha256", s(it)) }
    message?.let { add("message", s(it)) }
    sb.append('}')
    return sb.toString()
  }
}
