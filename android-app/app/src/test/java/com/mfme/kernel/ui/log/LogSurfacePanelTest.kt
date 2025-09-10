package com.mfme.kernel.ui.log

import com.mfme.kernel.data.Envelope
import com.mfme.kernel.data.telemetry.ReceiptEntity
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertTrue

class LogSurfacePanelTest {
    @Test
    fun markdownPreviewHasEntries() {
        val receipts = listOf(
            ReceiptEntity(id = 1, ok = true, code = "ok", adapter = "a", tsUtcIso = "2023-01-01T00:00:00Z", envelopeId = null, envelopeSha256 = "e1", message = null, spanId = "s", receiptSha256 = "r1")
        )
        val envelopes = listOf(
            Envelope(id = 1, sha256 = "e1", mime = "text/plain", text = "hello", filename = null, sourcePkgRef = "src", receivedAtUtc = Instant.EPOCH, metaJson = null)
        )
        val preview = buildMarkdownPreview(receipts, envelopes)
        assertTrue(preview.contains("receipt ok"))
        assertTrue(preview.contains("envelope e1"))
    }
}
