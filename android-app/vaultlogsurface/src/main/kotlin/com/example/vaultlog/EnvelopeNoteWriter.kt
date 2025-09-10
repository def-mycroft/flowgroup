package com.example.vaultlog

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Writes pretty-printed envelope JSON into markdown notes.
 */
class EnvelopeNoteWriter(private val adapter: VaultAdapter) {
    class Result(val path: Path)

    /**
     * Writes the envelope note if it does not already exist.
     * Max size: 100 KiB.
     */
    suspend fun write(sha256: String, json: String): Result = withContext(Dispatchers.IO) {
        val target = adapter.envelopeFileFor(sha256)
        if (Files.exists(target)) return@withContext Result(target)

        val content = """```json\n$json\n```\n"""
        val bytes = content.toByteArray(Charsets.UTF_8)
        require(bytes.size <= 100 * 1024) { "Envelope note exceeds 100 KiB" }

        val tmp = Files.createTempFile(target.parent, sha256, ".tmp")
        FileChannel.open(tmp, StandardOpenOption.WRITE).use { ch ->
            ch.write(ByteBuffer.wrap(bytes))
            ch.force(true)
        }
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            Files.deleteIfExists(tmp)
            throw e
        }
        Result(target)
    }
}
