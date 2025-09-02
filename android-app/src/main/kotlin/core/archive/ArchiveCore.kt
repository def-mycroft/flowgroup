package core.archive

import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.security.MessageDigest
import java.util.Locale

/** Envelope corresponds to the persisted metadata for a captured share. */
data class Envelope(
    val contentHashSha256: String,
    val createdAtUtc: Instant,
    val mediaType: String,
    val filename: String?,
    val sizeBytes: Long,
    val origin: String,
    val target: String,
    val filePath: Path? = null,
    val archivedPath: Path? = null,
    val sidecarPath: Path? = null
)

/** Plan produced by the pure archive core before any side effects. */
data class ArchivePlan(
    val dataPath: Path,
    val sidecarPath: Path,
    val sha256: String,
    val target: String,
    val sidecarBytes: ByteArray
)

private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC)

/**
 * Pure function computing an [ArchivePlan] from an [Envelope] and final byte array.
 * Ensures deterministic paths and sidecar canonicality.
 */
fun planArchive(envelope: Envelope, bytes: ByteArray): ArchivePlan {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    val hex = digest.joinToString("") { "%02x".format(it) }
    require(hex == envelope.contentHashSha256) { "hash_mismatch" }

    val dateShard = formatter.format(envelope.createdAtUtc)
    val ext = inferExtension(envelope.mediaType, envelope.filename)
    val dataPath = Paths.get("archive", dateShard, "${hex}.${ext}")
    val sidecarPath = Paths.get("archive", dateShard, "${hex}.json")

    val sidecar = buildSidecarJson(envelope)
    return ArchivePlan(dataPath, sidecarPath, hex, envelope.target, sidecar.toByteArray())
}

private fun inferExtension(mediaType: String, filename: String?): String {
    val typeExt = when (mediaType.toLowerCase(Locale.ROOT)) {
        "image/png" -> "png"
        "image/jpeg" -> "jpg"
        "text/plain" -> "txt"
        else -> null
    }
    val nameExt = filename?.substringAfterLast('.')?.toLowerCase(Locale.ROOT)
    return typeExt ?: nameExt ?: "bin"
}

private fun buildSidecarJson(envelope: Envelope): String {
    // Construct JSON with stable key order manually
    return buildString {
        append('{')
        append("\"content_hash_sha256\":\"")
        append(envelope.contentHashSha256)
        append("\",")
        append("\"created_at\":\"")
        append(envelope.createdAtUtc.toString())
        append("\",")
        append("\"media_type\":\"")
        append(envelope.mediaType)
        append("\",")
        envelope.filename?.let {
            append("\"filename\":\"")
            append(it)
            append("\",")
        }
        append("\"size_bytes\":")
        append(envelope.sizeBytes)
        append(',')
        append("\"origin\":\"")
        append(envelope.origin)
        append("\",")
        append("\"target\":\"")
        append(envelope.target)
        append('\"')
        envelope.filePath?.let {
            append(",\"file_path\":\"")
            append(it.toString())
            append('\"')
        }
        envelope.archivedPath?.let {
            append(",\"archived_path\":\"")
            append(it.toString())
            append('\"')
        }
        envelope.sidecarPath?.let {
            append(",\"sidecar_path\":\"")
            append(it.toString())
            append('\"')
        }
        append('}')
    }
}
