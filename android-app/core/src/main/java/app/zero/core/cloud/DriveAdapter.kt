package app.zero.core.cloud

interface DriveAdapter {
    suspend fun ensureFolder(pathSegments: List<String>): Result<DriveFolderRef>
    suspend fun findBySha256(sha256: String, folderId: String): Result<DriveFileRef?>
    suspend fun probe(): Result<Unit>
}
