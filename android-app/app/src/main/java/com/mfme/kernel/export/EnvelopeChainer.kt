package com.mfme.kernel.export

import android.content.Context
import com.mfme.kernel.data.Envelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Minimal envelope chainer that logs envelope hashes and mirrors
 * them into an optional Obsidian vault.
 */
class EnvelopeChainer(
    private val appContext: Context,
    private val exporter: ObsidianExporter,
) {
    suspend fun chain(env: Envelope) = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "telemetry").apply { mkdirs() }
        val log = File(dir, "envelopes.ndjson")
        FileOutputStream(log, true).use { fos ->
            fos.write(env.sha256.toByteArray())
            fos.write('\n'.code)
            fos.fd.sync()
        }
        exporter.upsertEnvelopeBrief(env)
        exporter.upsertDailyLog(env)
    }
}

