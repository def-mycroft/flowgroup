package com.mfme.kernel.telemetry

import com.mfme.kernel.data.telemetry.ReceiptCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class CanonicalReceiptJsonTest {
    @Test
    fun deterministicJsonAndHash() {
        val json1 = CanonicalReceiptJson.encode(
            adapter = "share",
            code = ReceiptCode.OK_NEW,
            tsUtcIso = "2023-01-01T00:00:00Z",
            spanId = "abc",
            envelopeId = 1L,
            envelopeSha256 = "deadbeef",
            message = null
        )
        val json2 = CanonicalReceiptJson.encode(
            adapter = "share",
            code = ReceiptCode.OK_NEW,
            tsUtcIso = "2023-01-01T00:00:00Z",
            spanId = "abc",
            envelopeId = 1L,
            envelopeSha256 = "deadbeef",
            message = null
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
