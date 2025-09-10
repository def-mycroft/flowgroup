# Telemetry Codes Taxonomy

Id: telemetry-codes-taxonomy
Updated: 2025-09-10T19:17:49Z

Success Codes
- ok_new, ok_duplicate, ok_rebound, ok_already_bound.

Error Codes
- permission_denied, empty_input, invalid_media, storage_quota,
  device_unavailable, digest_mismatch, error_not_found, unknown.

Use
- Map exceptions and validation failures to codes; record via ReceiptEmitter.
