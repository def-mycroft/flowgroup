# TroubleshootFlow — Instrument

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-drive-connect-visible
- updated: 2025-09-11

Purpose
- Ensure connect/verify paths never fail silently; all outcomes are observable via spans and typed Receipts.

Spans / Receipts
- Spans: {trace_id, brief_id, property_id, event, outcome?, duration_ms?}
- Receipts (adapters):
  - `cloud_ui`: `OkDriveConnected` | `ErrAuthCancelled` | `ErrAuthNoScope`
  - `cloud_verify`: `OkVerifyQueued` | `OkVerified` | `ErrNoAccount` | `ErrVerifyFailed(code)`

Checklist
- Connect result handler (Cloud screen): emit span `cloud_connect_result` and a `cloud_ui` receipt with the outcome. Include `brief_id` and `property_id` in tags/message until first‑class fields exist.
- Verify action: emit `cloud_verify_tap`; if no account → `ErrNoAccount`. If account present, probe/queue reconciler and emit `OkVerifyQueued` then `OkVerified` or `ErrVerifyFailed(code)`.
- History chip status changes can remain silent, but consider a QA-only `OkCloudStatus(connected|not_connected)` receipt when toggled.

Done When
- Connecting, cancelling, and verifying all produce spans+receipts linked to the brief/property, visible in Room and NDJSON sink.
