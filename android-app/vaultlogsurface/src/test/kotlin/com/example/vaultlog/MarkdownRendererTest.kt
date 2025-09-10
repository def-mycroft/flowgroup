package com.example.vaultlog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.time.Instant
import java.time.LocalDate

class MarkdownRendererTest {
    @Test
    fun envelopeBriefDeterministic() {
        val data = EnvelopeBriefData(
            sha256 = "abc",
            source = "share_in",
            mime = "text/plain",
            sizeBytes = 10,
            createdAtUtc = Instant.parse("2024-06-09T12:00:00Z"),
            driveState = "pending",
            driveFileId = null,
            driveWebLink = null,
            tags = listOf("mfme/envelope","source/share_in"),
            title = "Envelope abc",
            summary = "hello",
            bodyJson = "{\"foo\":1}"
        )
        val a = MarkdownRenderer.envelopeBrief(data)
        val b = MarkdownRenderer.envelopeBrief(data)
        assertEquals(a, b)
        val bytes = a.toByteArray(Charsets.UTF_8)
        assertEquals(a, String(bytes, Charsets.UTF_8))
        assertTrue(a.contains("mfme: envelope"))
    }

    @Test
    fun dailyLogDeterministic() {
        val entry = DailyLogLine(
            timestamp = Instant.parse("2024-06-09T12:00:00Z"),
            source = "share_in",
            mime = "text/plain",
            sizeBytes = 5,
            sha256 = "abc",
            tags = listOf("mfme/log","source/share_in")
        )
        val a = MarkdownRenderer.dailyLog(LocalDate.parse("2024-06-09"), listOf(entry))
        val b = MarkdownRenderer.dailyLog(LocalDate.parse("2024-06-09"), listOf(entry))
        assertEquals(a, b)
        val bytes = a.toByteArray(Charsets.UTF_8)
        assertEquals(a, String(bytes, Charsets.UTF_8))
        assertTrue(a.contains("mfme: daily_log"))
    }
}

