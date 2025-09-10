package com.mfme.kernel.export

import android.content.Context
import com.mfme.kernel.data.Envelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Minimal envelope chainer that logs envelope hashes and mirrors
 * them into an optional Obsidian vault. This implementation is
 * intentionally simple for the exercise and does not attempt to
 * replicate the full MFME behavior.
 */
class EnvelopeChainer(
    private val appContext: Context,
    private val exporter: ObsidianExporter = ObsidianExporter()
) {
    suspend fun chain(env: Envelope) = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "telemetry").apply { mkdirs() }
        val log = File(dir, "envelopes.ndjson")
        FileOutputStream(log, true).use { fos ->
            fos.write(env.sha256.toByteArray())
            fos.write('\n'.code)
            fos.fd.sync()
        }
        exporter.export(env)
    }
}
