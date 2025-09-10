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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Appends envelope entries to daily markdown log files.
 */
class LogWriter(private val adapter: VaultAdapter) {
    data class Entry(val timestamp: Instant, val label: String, val sha256: String)

    suspend fun append(entry: Entry): Path = withContext(Dispatchers.IO) {
        if (entry.timestamp.atOffset(ZoneOffset.UTC).offset != ZoneOffset.UTC) {
            throw IllegalArgumentException("Timestamp must be UTC")
        }
        val date = LocalDate.ofInstant(entry.timestamp, ZoneOffset.UTC)
        val logFile = adapter.logFileFor(date)

        val existing = if (Files.exists(logFile)) Files.readAllLines(logFile) else emptyList()
        val frontMatterLines = mutableListOf(
            "---",
            "date: $date",
            "generated_by: vault-log-surface@1.0.0",
            "item_count: 0",
            "---",
        )
        val entries = existing.drop(frontMatterLines.size).toMutableList()
        if (entries.any { it.contains(entry.sha256) }) return@withContext logFile

        val newLine = "- ${entry.timestamp} — [${entry.label}](../envelopes/${entry.sha256}.md)"
        entries.add(newLine)
        frontMatterLines[3] = "item_count: ${entries.size}"

        val content = (frontMatterLines + entries).joinToString(System.lineSeparator()) + System.lineSeparator()

        val tmp = Files.createTempFile(logFile.parent, logFile.fileName.toString(), ".tmp")
        FileChannel.open(tmp, StandardOpenOption.WRITE).use { ch ->
            ch.write(ByteBuffer.wrap(content.toByteArray(Charsets.UTF_8)))
            ch.force(true)
        }
        try {
            Files.move(tmp, logFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            Files.deleteIfExists(tmp)
            throw e
        }
        logFile
    }
}
