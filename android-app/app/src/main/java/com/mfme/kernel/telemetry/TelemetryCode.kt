package com.mfme.kernel.telemetry

sealed interface TelemetryCode {
  val ok: Boolean
  val wire: String

  // Successes
  data object OkNew : TelemetryCode {
    override val ok = true
    override val wire = "ok_new"
  }
  data object OkDuplicate : TelemetryCode {
    override val ok = true
    override val wire = "ok_duplicate"
  }
  data object OkRebound : TelemetryCode {
    override val ok = true
    override val wire = "ok_rebound"
  }
  data object OkAlreadyBound : TelemetryCode {
    override val ok = true
    override val wire = "ok_already_bound"
  }

  // Cloud/Drive successes
  data object OkUploaded : TelemetryCode {
    override val ok = true
    override val wire = "ok_uploaded"
  }
  data object OkDuplicateUpload : TelemetryCode {
    override val ok = true
    override val wire = "ok_duplicate_upload"
  }
  data object OkDriveConnected : TelemetryCode {
    override val ok = true
    override val wire = "ok_drive_connected"
  }
  data object OkVerifyQueued : TelemetryCode {
    override val ok = true
    override val wire = "ok_verify_queued"
  }
  data object OkVerified : TelemetryCode {
    override val ok = true
    override val wire = "ok_verified"
  }

  // Errors
  data object PermissionDenied : TelemetryCode {
    override val ok = false
    override val wire = "permission_denied"
  }
  data object EmptyInput : TelemetryCode {
    override val ok = false
    override val wire = "empty_input"
  }
  data object InvalidMedia : TelemetryCode {
    override val ok = false
    override val wire = "invalid_media"
  }
  data object StorageQuota : TelemetryCode {
    override val ok = false
    override val wire = "storage_quota"
  }
  data object DeviceUnavailable : TelemetryCode {
    override val ok = false
    override val wire = "device_unavailable"
  }
  data object DigestMismatch : TelemetryCode {
    override val ok = false
    override val wire = "digest_mismatch"
  }
  data object ErrorNotFound : TelemetryCode {
    override val ok = false
    override val wire = "error_not_found"
  }
  // Cloud/Drive errors
  data object PermissionDeniedAuth : TelemetryCode {
    override val ok = false
    override val wire = "permission_denied_auth"
  }
  data object NetworkBackoff : TelemetryCode {
    override val ok = false
    override val wire = "network_backoff"
  }
  data object ResolverError : TelemetryCode {
    override val ok = false
    override val wire = "resolver_error"
  }
  data class UnknownDriveError(val code: Int? = null, val name: String = "unknown_drive_error") : TelemetryCode {
    override val ok = false
    override val wire = name
  }
  data class Unknown(val name: String = "unknown") : TelemetryCode {
    override val ok = false
    override val wire = name
  }
  // Cloud/Drive UI/verify errors
  data object ErrNoAccount : TelemetryCode {
    override val ok = false
    override val wire = "err_no_account"
  }
  data object ErrAuthCancelled : TelemetryCode {
    override val ok = false
    override val wire = "err_auth_cancelled"
  }
  data object ErrAuthNoScope : TelemetryCode {
    override val ok = false
    override val wire = "err_auth_no_scope"
  }
  data class ErrVerifyFailed(val name: String = "err_verify_failed") : TelemetryCode {
    override val ok = false
    override val wire = name
  }
}
