package edge.archive.fs

import core.archive.ArchivePlan
import kotlinx.coroutines.channels.Channel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Simple commit bus using a Kotlin Channel. */
class ArchiverCommitBus {
    val channel = Channel<ArchivePlan>(Channel.UNLIMITED)
}

/** Adapter writing data and sidecar atomically then emitting commit event. */
class ArchiveFsAdapter(private val bus: ArchiverCommitBus) {
    fun commit(bytes: ByteArray, plan: ArchivePlan) {
        val tmpData = Files.createTempFile("data", null)
        val tmpSidecar = Files.createTempFile("sidecar", null)
        Files.write(tmpData, bytes)
        Files.write(tmpSidecar, plan.sidecarBytes)
        Files.createDirectories(plan.dataPath.parent)
        Files.move(tmpData, plan.dataPath, StandardCopyOption.REPLACE_EXISTING)
        Files.move(tmpSidecar, plan.sidecarPath, StandardCopyOption.REPLACE_EXISTING)
        bus.channel.offer(plan)
    }
}
