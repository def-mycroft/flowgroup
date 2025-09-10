package com.mfme.kernel.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.vaultlog.DailyLogLine
import com.example.vaultlog.EnvelopeBriefData
import com.example.vaultlog.MarkdownRenderer
import com.mfme.kernel.data.Envelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Writes envelope briefs and daily logs into an Obsidian vault when configured.
 * If no vault tree URI is provided, operations are no-ops.
 */
class ObsidianExporter(
    private val context: Context,
    treeUri: Uri?,
) {
    private val resolver = context.contentResolver
    private val root: DocumentFile? = treeUri?.let { DocumentFile.fromTreeUri(context, it) }

    suspend fun upsertEnvelopeBrief(env: Envelope) = withContext(Dispatchers.IO) {
        val rootDir = root ?: return@withContext
        val dir = rootDir.getOrCreateDir("envelopes") ?: return@withContext
        val fileName = "${env.sha256}.md"
        val content = MarkdownRenderer.envelopeBrief(
            EnvelopeBriefData(
                sha256 = env.sha256,
                source = env.sourcePkgRef,
                mime = env.mime,
                sizeBytes = null,
                createdAtUtc = env.receivedAtUtc,
                driveState = "pending",
                driveFileId = null,
                driveWebLink = null,
                tags = listOf("mfme/envelope", "source/${env.sourcePkgRef}"),
                title = "Envelope ${env.sha256}",
                summary = env.text?.take(160) ?: env.filename.orEmpty(),
                bodyJson = org.json.JSONObject(
                    mapOf(
                        "sha256" to env.sha256,
                        "mime" to env.mime,
                        "filename" to env.filename,
                        "source" to env.sourcePkgRef,
                        "receivedAtUtc" to env.receivedAtUtc.toString()
                    )
                ).toString(),
            )
        )
        writeIfChanged(dir, fileName, content)
    }

    suspend fun upsertDailyLog(env: Envelope) = withContext(Dispatchers.IO) {
        val rootDir = root ?: return@withContext
        val dir = rootDir.getOrCreateDir("logs") ?: return@withContext
        val date = env.receivedAtUtc.atOffset(ZoneOffset.UTC).toLocalDate()
        val fileName = "$date.md"

        val file = dir.findFile(fileName)
        val existingEntries = mutableListOf<DailyLogLine>()
        var alreadyLogged = false
        if (file != null) {
            resolver.openInputStream(file.uri)?.bufferedReader()?.use { reader ->
                var inBody = false
                var dashCount = 0
                reader.forEachLine { line ->
                    if (!inBody) {
                        if (line == "---") {
                            dashCount++
                            if (dashCount == 2) inBody = true
                        }
                    } else if (line.startsWith("- ")) {
                        if (line.contains(env.sha256)) {
                            alreadyLogged = true
                        } else {
                            parseLine(line, date)?.let { existingEntries.add(it) }
                        }
                    }
                }
            }
        }
        if (alreadyLogged) return@withContext
        val entry = DailyLogLine(
            timestamp = env.receivedAtUtc,
            source = env.sourcePkgRef,
            mime = env.mime,
            sizeBytes = null,
            sha256 = env.sha256,
            tags = listOf("mfme/log", "source/${env.sourcePkgRef}"),
        )
        existingEntries.add(0, entry)
        val content = MarkdownRenderer.dailyLog(date, existingEntries)
        writeIfChanged(dir, fileName, content)
    }

    private fun parseLine(line: String, date: LocalDate): DailyLogLine? {
        val regex = Regex("- \\[([0-9]{2}:[0-9]{2}:[0-9]{2}Z)] ([^ ]+) \\(([^,)]+)(?:, ([0-9]+) B)?\\) \\[[^[]*envelopes/([^|]+)\\|[^]]+]] (.*)")
        val m = regex.matchEntire(line) ?: return null
        val time = m.groupValues[1]
        val source = m.groupValues[2]
        val mime = m.groupValues[3].ifBlank { null }
        val size = m.groupValues[4].ifBlank { null }?.toLong()
        val sha = m.groupValues[5]
        val tags = m.groupValues[6].split(" ").filter { it.isNotBlank() }.map { it.removePrefix("#") }
        val ts = date.atTime(LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm:ss'Z'"))).toInstant(ZoneOffset.UTC)
        return DailyLogLine(ts, source, mime, size, sha, tags)
    }

    private fun DocumentFile.getOrCreateDir(name: String): DocumentFile? {
        listFiles().firstOrNull { it.isDirectory && it.name == name }?.let { return it }
        return createDirectory(name)
    }

    private fun DocumentFile.findFile(name: String): DocumentFile? =
        listFiles().firstOrNull { it.name == name }

    private fun writeIfChanged(dir: DocumentFile, fileName: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val existing = dir.findFile(fileName)?.let { resolver.openInputStream(it.uri)?.use { it.readBytes() } }
        if (existing != null && existing.contentEquals(bytes)) return
        val tmp = dir.createFile("text/markdown", "$fileName.new") ?: return
        resolver.openOutputStream(tmp.uri)?.use { os ->
            os.write(bytes)
            os.flush()
        }
        dir.findFile(fileName)?.delete()
        tmp.renameTo(fileName)
    }
}
