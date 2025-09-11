package app.zero.core.cloud

data class DriveFolderRef(val id: String)
data class DriveFileRef(val id: String, val md5: String?, val bytes: Long?)
data class UploadSpec(
    val folderId: String,
    val sha256: String,
    val bytes: Long,
    val mime: String,
    val ext: String?,
    val receivedAtUtc: String,
    val idempotencyKey: String,
    // Optional absolute path to the local payload file to stream for upload (if applicable)
    val localPath: String? = null,
    // Optional progress callback invoked from adapter during upload
    val onProgress: ((sent: Long, total: Long) -> Unit)? = null
)
sealed interface CloudAuthError {
    data class PermissionDeniedAuth(val reason: String): CloudAuthError
    data class UserCancelledAuth(val reason: String): CloudAuthError
    data class AuthScopeRevoked(val reason: String): CloudAuthError
    data class AuthTokenRefreshFailed(val reason: String): CloudAuthError
    data class UnknownAuthError(val code: Int?, val message: String): CloudAuthError
}
