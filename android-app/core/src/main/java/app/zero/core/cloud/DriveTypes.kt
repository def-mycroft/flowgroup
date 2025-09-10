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
    val idempotencyKey: String
)
sealed interface CloudAuthError {
    data class PermissionDeniedAuth(val reason: String): CloudAuthError
    data class UserCancelledAuth(val reason: String): CloudAuthError
    data class AuthScopeRevoked(val reason: String): CloudAuthError
    data class AuthTokenRefreshFailed(val reason: String): CloudAuthError
    data class UnknownAuthError(val code: Int?, val message: String): CloudAuthError
}
