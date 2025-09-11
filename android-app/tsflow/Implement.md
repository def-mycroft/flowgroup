# TroubleshootFlow — Implement

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-drive-connect-visible
- updated: 2025-09-11

Purpose
- Make Drive connection state visible and verifiable; ensure ‘Verify now’ always yields a typed outcome.

Minimal Patch Plan
- Source of truth: use `DriveServiceFactory.getAdapter(context) != null` or `TokenProvider.hasAccount()` as the only check for “connected”.
- Cloud connect result:
  - On success: update UI immediately and invalidate `DriveServiceFactory`; emit `cloud_ui:OkDriveConnected`.
  - On cancel/no-scope: emit `cloud_ui:ErrAuthCancelled|ErrAuthNoScope`.
- Verify now:
  - If not connected: emit `cloud_verify:ErrNoAccount` and show a brief snackbar.
  - If connected: either queue reconciler (`ReconcilerScheduler.verifyOnce`) with `OkVerifyQueued` and optionally probe adapter to emit `OkVerified|ErrVerifyFailed(code)`.
- History chip:
  - Derive `connected` from the same source of truth; remove duplicate checks.

Guardrails
- No network or Google APIs in core; only through `TokenProvider`/adapter.
- All new outcomes must produce receipts with `brief_id`/`property_id` tagged.

Non-Goals
- Redesigning upload/binding workflow; changing receipt schema.

Done When
- After connecting, Cloud and History reflect connected state; verifying yields receipts. Property tests pass; no regressions in sibling tests.
