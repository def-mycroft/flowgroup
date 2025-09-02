import core.archive.Envelope
import core.archive.planArchive
import edge.archive.fs.ArchiveFsAdapter
import edge.archive.fs.ArchiverCommitBus
import edge.upload.work.CommitListener
import edge.upload.work.WorkManagerStub
import java.nio.file.Files
import java.time.Instant

/** Simple tests verifying ordering fence, idempotency and canonical sidecar. */
fun main() {
    val bus = ArchiverCommitBus()
    val wm = WorkManagerStub()
    CommitListener(bus.channel, wm)
    val adapter = ArchiveFsAdapter(bus)

    val bytes = "hello world".toByteArray()
    val sha = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    val env = Envelope(
        sha, Instant.parse("2024-01-01T00:00:00Z"),
        mediaType = "text/plain",
        filename = "hello.txt",
        sizeBytes = bytes.size.toLong(),
        origin = "android.share", target = "testTarget"
    )
    val plan = planArchive(env, bytes)

    // Ordering: no commit yet -> no work enqueued
    check(wm.getTasks().isEmpty()) { "work should not start before commit" }

    // Commit to fs
    adapter.commit(bytes, plan)
    // give coroutine time to process
    Thread.sleep(100)
    check(wm.getTasks().size == 1) { "work should enqueue once" }

    // Idempotency: second commit same sha should not enqueue again
    adapter.commit(bytes, plan)
    Thread.sleep(100)
    check(wm.getTasks().size == 1) { "duplicate commit enqueued second job" }

    // Canonicality: sidecar bytes equal to stored file
    val storedSidecar = Files.readAllBytes(plan.sidecarPath)
    check(storedSidecar.contentEquals(plan.sidecarBytes)) { "sidecar drift" }

    println("morph-grader: all checks passed")
}
