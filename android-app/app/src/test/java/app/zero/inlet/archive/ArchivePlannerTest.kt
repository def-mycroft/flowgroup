package app.zero.inlet.archive

import org.junit.Assert.*
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.time.Instant

class ArchivePlannerTest {
    private val now = Instant.parse("2024-03-04T05:06:07Z")
    private val meta = EnvelopeMeta(
        sha256 = "a".repeat(64),
        mediaType = "image/jpeg",
        filename = "photo.jpg"
    )
    private val target = "disk"

    @Test
    fun deterministicPaths() {
        val plan1 = ArchivePlanner.plan(meta, target, now)
        val plan2 = ArchivePlanner.plan(meta, target, now)
        assertEquals(plan1.dataPath, plan2.dataPath)
        assertEquals(plan1.sidecarPath, plan2.sidecarPath)
        assertEquals(plan1.shardPath, plan2.shardPath)
        assertEquals(plan1.ext, plan2.ext)
    }

    @Test
    fun sidecarByteStability() {
        val plan1 = ArchivePlanner.plan(meta, target, now)
        val plan2 = ArchivePlanner.plan(meta, target, now)
        assertTrue(plan1.sidecarBytes.contentEquals(plan2.sidecarBytes))
    }

    @Test
    fun sidecarCanonical() {
        val plan = ArchivePlanner.plan(meta, target, now)
        val expected = "{\"created_at\":\"2024-03-04T05:06:07Z\",\"ext\":\"jpg\",\"filename\":\"photo.jpg\",\"media_type\":\"image/jpeg\",\"sha256\":\"${"a".repeat(64)}\",\"target\":\"disk\"}"
        assertEquals(expected, String(plan.sidecarBytes, StandardCharsets.UTF_8))
    }

    @Test
    fun shardingFormat() {
        val plan = ArchivePlanner.plan(meta, target, now)
        assertEquals(Paths.get("archive", "2024", "03", "04"), plan.shardPath)
        assertEquals(plan.shardPath.resolve("${meta.sha256}.${plan.ext}"), plan.dataPath)
        assertEquals(plan.shardPath.resolve("${meta.sha256}.json"), plan.sidecarPath)
    }

    @Test
    fun extFromMimeJpg() {
        val m = EnvelopeMeta(sha256 = "b".repeat(64), mediaType = "image/jpeg", filename = null)
        val plan = ArchivePlanner.plan(m, target, now)
        assertEquals("jpg", plan.ext)
    }

    @Test
    fun extFromMimePng() {
        val m = EnvelopeMeta(sha256 = "c".repeat(64), mediaType = "image/png", filename = null)
        val plan = ArchivePlanner.plan(m, target, now)
        assertEquals("png", plan.ext)
    }

    @Test
    fun extFromFilenamePdf() {
        val m = EnvelopeMeta(sha256 = "d".repeat(64), mediaType = null, filename = "report.PDF")
        val plan = ArchivePlanner.plan(m, target, now)
        assertEquals("pdf", plan.ext)
    }

    @Test
    fun extFallbackBin() {
        val m = EnvelopeMeta(sha256 = "e".repeat(64), mediaType = null, filename = null)
        val plan = ArchivePlanner.plan(m, target, now)
        assertEquals("bin", plan.ext)
    }
}
