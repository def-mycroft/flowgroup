package com.mfme.kernel.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import com.mfme.kernel.telemetry.Hashing

class CanonicalReceiptJsonTest {
    @Test
    fun deterministicJsonAndHash() {
        val json1 = CanonicalReceiptJson.encodeV2(
            ok = false,
            codeWire = TelemetryCode.PermissionDenied.wire,
            tsUtcIso = "2023-01-01T00:00:00Z",
            adapter = "share",
            spanId = "abc",
            envelopeId = null,
            envelopeSha256 = null,
            message = "denied"
        )
        val json2 = CanonicalReceiptJson.encodeV2(
            ok = false,
            codeWire = TelemetryCode.PermissionDenied.wire,
            tsUtcIso = "2023-01-01T00:00:00Z",
            adapter = "share",
            spanId = "abc",
            envelopeId = null,
            envelopeSha256 = null,
            message = "denied"
        )
        assertEquals(json1, json2)
        val sha1 = Hashing.sha256Hex(json1.toByteArray(Charsets.UTF_8))
        val sha2 = Hashing.sha256Hex(json2.toByteArray(Charsets.UTF_8))
        assertEquals(sha1, sha2)
    }

    @Test
    fun utcEndsWithZ() {
        val ts = Instant.now().toString()
        assertTrue(ts.endsWith("Z"))
        val parsed = Instant.parse(ts)
        assertEquals(ts, parsed.toString())
    }
}
