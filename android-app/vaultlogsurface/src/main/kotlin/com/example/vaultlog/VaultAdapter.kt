package com.example.vaultlog

import java.nio.file.Files
import java.nio.file.Path

/**
 * Resolves directories inside an Obsidian vault for log and envelope storage.
 * All paths are lazily created on demand.
 */
class VaultAdapter(private val vaultRoot: Path) {
    val logsDir: Path = vaultRoot.resolve("logs").also { Files.createDirectories(it) }
    val envelopesDir: Path = vaultRoot.resolve("envelopes").also { Files.createDirectories(it) }

    fun logFileFor(date: java.time.LocalDate): Path = logsDir.resolve("$date.md")
    fun envelopeFileFor(sha256: String): Path = envelopesDir.resolve("$sha256.md")
}
