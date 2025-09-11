package app.zero.core.cloud

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory fake DriveAdapter for tests. Simulates folders and files keyed by sha256.
 */
class FakeDriveAdapter : DriveAdapter {
    private val ids = AtomicLong(1)
    private val folders = ConcurrentHashMap<String, DriveFolderRef>()
    private val filesById = ConcurrentHashMap<String, DriveFileRef>()
    private val byFolderBySha = ConcurrentHashMap<String, ConcurrentHashMap<String, DriveFileRef>>()

    override suspend fun ensureFolder(pathSegments: List<String>): Result<DriveFolderRef> {
        val key = pathSegments.joinToString("/")
        val ref = folders.computeIfAbsent(key) { DriveFolderRef("fld_${ids.getAndIncrement()}") }
        return Result.success(ref)
    }

    override suspend fun findBySha256(sha256: String, folderId: String): Result<DriveFileRef?> {
        val map = byFolderBySha[folderId]
        return Result.success(map?.get(sha256))
    }

    override suspend fun getMetadata(fileId: String): Result<DriveFileRef?> {
        return Result.success(filesById[fileId])
    }

    override suspend fun uploadResumable(spec: UploadSpec): Result<DriveFileRef> {
        val id = "fil_${ids.getAndIncrement()}"
        val ref = DriveFileRef(id = id, md5 = null, bytes = spec.bytes)
        filesById[id] = ref
        byFolderBySha.computeIfAbsent(spec.folderId) { ConcurrentHashMap() }[spec.sha256] = ref
        return Result.success(ref)
    }

    override suspend fun probe(): Result<Unit> = Result.success(Unit)
}

