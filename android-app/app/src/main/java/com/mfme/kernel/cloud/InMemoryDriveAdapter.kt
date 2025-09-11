package com.mfme.kernel.cloud

import app.zero.core.cloud.DriveAdapter
import app.zero.core.cloud.DriveFileRef
import app.zero.core.cloud.DriveFolderRef
import app.zero.core.cloud.UploadSpec
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Simple in-memory DriveAdapter used for local/dev flows.
 * Not persisted across process restarts; intended as a stub.
 */
class InMemoryDriveAdapter private constructor() : DriveAdapter {
    private val ids = AtomicLong(1)
    private val folders = ConcurrentHashMap<String, DriveFolderRef>()
    private val folderIdByPath = ConcurrentHashMap<String, String>()
    private val filesById = ConcurrentHashMap<String, DriveFileRef>()
    private val byFolderBySha = ConcurrentHashMap<String, ConcurrentHashMap<String, DriveFileRef>>()

    override suspend fun ensureFolder(pathSegments: List<String>): Result<DriveFolderRef> {
        val path = pathSegments.joinToString("/")
        val existingId = folderIdByPath[path]
        if (existingId != null) return Result.success(DriveFolderRef(existingId))
        val id = "fld_${ids.getAndIncrement()}"
        folderIdByPath[path] = id
        val ref = DriveFolderRef(id)
        folders[id] = ref
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
        val map = byFolderBySha.computeIfAbsent(spec.folderId) { ConcurrentHashMap() }
        map[spec.sha256] = ref
        return Result.success(ref)
    }

    override suspend fun probe(): Result<Unit> = Result.success(Unit)

    companion object {
        val instance: InMemoryDriveAdapter by lazy { InMemoryDriveAdapter() }
    }
}

