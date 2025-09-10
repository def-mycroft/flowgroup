package com.example.vaultlog

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Data for rendering an envelope brief. */
data class EnvelopeBriefData(
    val sha256: String,
    val source: String,
    val mime: String?,
    val sizeBytes: Long?,
    val createdAtUtc: Instant,
    val driveState: String,
    val driveFileId: String?,
    val driveWebLink: String?,
    val tags: List<String>,
    val title: String,
    val summary: String,
    val bodyJson: String,
)

/** Data for rendering a single daily log line. */
data class DailyLogLine(
    val timestamp: Instant,
    val source: String,
    val mime: String?,
    val sizeBytes: Long?,
    val sha256: String,
    val tags: List<String>,
)

/** Deterministic markdown renderer used by the Obsidian exporter. */
object MarkdownRenderer {
    private val instantFormatter = DateTimeFormatter.ISO_INSTANT
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss'Z'").withZone(ZoneOffset.UTC)

    fun envelopeBrief(data: EnvelopeBriefData): String {
        val yaml = buildString {
            appendLine("---")
            appendLine("mfme: envelope")
            appendLine("sha256: \"${data.sha256}\"")
            appendLine("source: \"${data.source}\"")
            appendLine("mime: \"${data.mime ?: ""}\"")
            appendLine("size_bytes: ${data.sizeBytes ?: "null"}")
            appendLine("created_at_utc: \"${instantFormatter.format(data.createdAtUtc)}\"")
            appendLine("drive:")
            appendLine("  state: ${data.driveState}")
            appendLine("  file_id: ${data.driveFileId?.let { "\"$it\"" } ?: "null"}")
            appendLine("  web_link: ${data.driveWebLink?.let { "\"$it\"" } ?: "null"}")
            appendLine("tags: [${data.tags.joinToString(",") { "\"$it\"" }}]")
            appendLine("---")
        }
        val body = buildString {
            appendLine("# ${data.title}")
            if (data.summary.isNotEmpty()) appendLine(data.summary)
            appendLine("```json")
            appendLine(data.bodyJson)
            appendLine("```")
        }
        return yaml + body
    }

    fun formatDailyLogLine(entry: DailyLogLine): String {
        val size = entry.sizeBytes?.let { ", ${it} B" } ?: ""
        val tags = entry.tags.joinToString(" ") { "#${it}" }
        val mime = entry.mime ?: ""
        val time = timeFormatter.format(entry.timestamp)
        return "- [${time}] ${entry.source} (${mime}${size}) [[envelopes/${entry.sha256}|${entry.sha256.take(7)}]] $tags"
    }

    fun dailyLog(date: LocalDate, entries: List<DailyLogLine>): String {
        val lines = entries.map { formatDailyLogLine(it) }
        val yaml = buildString {
            appendLine("---")
            appendLine("mfme: daily_log")
            appendLine("date_utc: \"$date\"")
            appendLine("entries: ${lines.size}")
            appendLine("---")
        }
        val body = lines.joinToString(System.lineSeparator()) + System.lineSeparator()
        return yaml + body
    }
}

