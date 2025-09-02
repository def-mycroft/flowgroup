package app.zero.inlet.archive

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TreeMap

/** Metadata needed to plan an archive. */
data class EnvelopeMeta(
    val sha256: String,
    val mediaType: String? = null,
    val filename: String? = null,
)

/** Deterministic archive plan with pre-built sidecar bytes. */
data class ArchivePlan(
    val dataPath: Path,
    val sidecarPath: Path,
    val target: String,
    val ext: String,
    val shardPath: Path,
    val sha256: String,
    val nowZ: Instant,
    val sidecarBytes: ByteArray,
)

/** Pure planner computing archive locations and sidecar JSON. */
object ArchivePlanner {
    private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC)

    fun plan(meta: EnvelopeMeta, target: String, now: Instant = Instant.now()): ArchivePlan {
        val ext = inferExt(meta.mediaType, meta.filename)
        val dateShard = formatter.format(now)
        val shardPath = Paths.get("archive", *dateShard.split('/').toTypedArray())
        val dataPath = shardPath.resolve("${meta.sha256}.$ext")
        val sidecarPath = shardPath.resolve("${meta.sha256}.json")
        val sidecarJson = canonicalJson(meta, target, ext, now)
        val bytes = sidecarJson.toByteArray(StandardCharsets.UTF_8)
        return ArchivePlan(dataPath, sidecarPath, target, ext, shardPath, meta.sha256, now, bytes)
    }

    private fun inferExt(mediaType: String?, filename: String?): String {
        val mimeExt = when (mediaType?.lowercase(Locale.ROOT)) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "application/pdf" -> "pdf"
            else -> null
        }
        val nameExt = filename?.substringAfterLast('.', "")
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotEmpty() }
        return mimeExt ?: nameExt ?: "bin"
    }

    private fun canonicalJson(meta: EnvelopeMeta, target: String, ext: String, now: Instant): String {
        val map = TreeMap<String, String>()
        map["created_at"] = now.toString()
        map["ext"] = ext
        meta.filename?.let { map["filename"] = it }
        meta.mediaType?.let { map["media_type"] = it }
        map["sha256"] = meta.sha256
        map["target"] = target
        val sb = StringBuilder()
        sb.append('{')
        val iter = map.entries.iterator()
        while (iter.hasNext()) {
            val (k, v) = iter.next()
            sb.append('"').append(k).append('"').append(':').append('"').append(escape(v)).append('"')
            if (iter.hasNext()) sb.append(',')
        }
        sb.append('}')
        return sb.toString()
    }

    private fun escape(s: String): String {
        val out = StringBuilder()
        for (c in s) {
            when (c) {
                '\\' -> out.append("\\\\")
                '"' -> out.append("\\\"")
                else -> out.append(c)
            }
        }
        return out.toString()
    }
}
