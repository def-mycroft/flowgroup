package app.zero.inlet.archive

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import app.zero.inlet.upload.UploadWorker
import java.io.File
import java.io.FileOutputStream

/** Simple tracing span placeholder. */
data class Span(val name: String)

/** Simple receipt event. */
data class Receipt(val type: String)

/** Plan describing where to store data and sidecar. */
data class ArchivePlan(
    val dataPath: String,
    val sidecarPath: String,
    val sha256: String,
    val target: String,
    val sidecarBytes: ByteArray
)

/**
 * Adapter committing archive data to disk and enqueuing upload work.
 * Files are first written to temporary `.part` files under a `tmp` directory,
 * fsynced and then atomically renamed into their final shard path.
 */
class ArchiveCommitter(
    private val context: Context,
    private val rootDir: File,
    private val events: MutableCollection<Any> = mutableListOf()
) {
    fun commit(plan: ArchivePlan, bytes: ByteArray, failAfterWrite: Boolean = false) {
        val dataFile = File(rootDir, plan.dataPath)
        val sidecarFile = File(rootDir, plan.sidecarPath)
        dataFile.parentFile?.mkdirs()
        sidecarFile.parentFile?.mkdirs()

        val tmpDir = File(rootDir, "tmp").apply { mkdirs() }
        val tmpData = File(tmpDir, dataFile.name + ".part")
        val tmpSide = File(tmpDir, sidecarFile.name + ".part")

        FileOutputStream(tmpData).use { fos ->
            fos.write(bytes)
            fos.fd.sync()
        }
        FileOutputStream(tmpSide).use { fos ->
            fos.write(plan.sidecarBytes)
            fos.fd.sync()
        }

        if (failAfterWrite) {
            // Simulate failure before rename for testing
            throw RuntimeException("simulated_failure")
        }

        if (!tmpData.renameTo(dataFile) || !tmpSide.renameTo(sidecarFile)) {
            tmpData.delete()
            tmpSide.delete()
            throw IllegalStateException("rename_failed")
        }

        val name = "${plan.target}:${plan.sha256}"
        val request = OneTimeWorkRequestBuilder<UploadWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(name, ExistingWorkPolicy.KEEP, request)

        events.add(Span("archiver.commit"))
        events.add(Receipt("archiver.commit"))
    }

    fun getEvents(): List<Any> = events.toList()
}

