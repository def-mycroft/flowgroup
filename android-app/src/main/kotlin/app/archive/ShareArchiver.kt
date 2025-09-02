package app.archive

import android.content.Context
import android.net.Uri
import core.archive.Envelope
import core.archive.planArchive
import edge.archive.fs.ArchiveFsAdapter
import edge.archive.fs.ArchiverCommitBus
import java.nio.file.Path
import java.time.Instant

class ShareArchiver(private val context: Context) {
    private val bus = ArchiverCommitBus()
    private val adapter = ArchiveFsAdapter(bus)

    /**
     * Archive bytes from a given [Uri]. Returns the resulting [Envelope].
     */
    fun archive(uri: Uri, filename: String?, mediaType: String, target: String): Envelope {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
        val sha = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val env = Envelope(
            contentHashSha256 = sha,
            createdAtUtc = Instant.now(),
            mediaType = mediaType,
            filename = filename,
            sizeBytes = bytes.size.toLong(),
            origin = "share-intent",
            target = target,
            filePath = uri.path?.let { Path.of(it) }
        )
        val plan = planArchive(env, bytes)
        adapter.commit(bytes, plan)
        return env.copy(archivedPath = plan.dataPath, sidecarPath = plan.sidecarPath)
    }
}
