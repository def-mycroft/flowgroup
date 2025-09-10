package com.mfme.kernel.export

import com.mfme.kernel.data.Envelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Simple exporter that mirrors envelopes into an Obsidian vault if configured. */
class ObsidianExporter(private val root: File? = null) {
    suspend fun export(env: Envelope) = withContext(Dispatchers.IO) {
        val rootDir = root ?: return@withContext
        val dir = File(rootDir, "envelopes").apply { mkdirs() }
        val file = File(dir, "${env.sha256}.md")
        FileOutputStream(file).use { fos ->
            fos.write("# Envelope ${env.sha256}\n".toByteArray())
            fos.fd.sync()
        }
    }
}
