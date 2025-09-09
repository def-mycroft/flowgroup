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
  data class Unknown(val name: String = "unknown") : TelemetryCode {
    override val ok = false
    override val wire = name
  }
}
