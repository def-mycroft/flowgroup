package com.example.vaultlog

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class LogSurfaceTest {
    private fun setup(): Triple<VaultAdapter, EnvelopeNoteWriter, LogWriter> {
        val tmp = Files.createTempDirectory("vault")
        val adapter = VaultAdapter(tmp)
        val noteWriter = EnvelopeNoteWriter(adapter)
        val logWriter = LogWriter(adapter)
        return Triple(adapter, noteWriter, logWriter)
    }

    @Test
    fun `duplicate envelopes collapse`() = runBlocking {
        val (adapter, noteWriter, logWriter) = setup()
        val sha = "abc123"
        noteWriter.write(sha, "{\"type\":\"camera\"}")
        val ts = Instant.parse("2025-09-10T08:52:00Z")
        val entry = LogWriter.Entry(ts, "camera", sha)
        logWriter.append(entry)
        logWriter.append(entry)
        val logFile = adapter.logFileFor(LocalDate.of(2025, 9, 10))
        val lines = Files.readAllLines(logFile)
        assertEquals(1, lines.count { it.contains(sha) })
    }

    @Test
    fun `new day rotates file`() = runBlocking {
        val (adapter, noteWriter, logWriter) = setup()
        noteWriter.write("a1", "{}"); logWriter.append(LogWriter.Entry(Instant.parse("2025-09-10T23:59:00Z"), "x", "a1"))
        noteWriter.write("a2", "{}"); logWriter.append(LogWriter.Entry(Instant.parse("2025-09-11T00:00:10Z"), "x", "a2"))
        assertTrue(Files.exists(adapter.logFileFor(LocalDate.of(2025,9,10))))
        assertTrue(Files.exists(adapter.logFileFor(LocalDate.of(2025,9,11))))
    }

    @Test
    fun `log links to envelope note`() = runBlocking {
        val (adapter, noteWriter, logWriter) = setup()
        val sha = "abc123"
        noteWriter.write(sha, "{}");
        val ts = Instant.parse("2025-09-10T08:52:00Z")
        logWriter.append(LogWriter.Entry(ts, "camera", sha))
        val logFile = adapter.logFileFor(LocalDate.of(2025,9,10))
        val lines = Files.readAllLines(logFile)
        assertTrue(lines.any { it.contains("../envelopes/$sha.md") })
    }
}
