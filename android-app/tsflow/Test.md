# TroubleshootFlow — Test

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-drive-connect-visible
- updated: 2025-09-11

Purpose
- Prove the Drive connect/verify flow is observable (no silent no‑ops) and reflected across Cloud and History.

Checklist
- Compose Robolectric test renders `CloudScreen()` with fake `TokenProvider` injected via seam; simulate:
  - account selected → assert UI shows connected and a `cloud_ui:OkDriveConnected` receipt recorded.
  - user cancels → assert `cloud_ui:ErrAuthCancelled`.
- Tap ‘Verify now’ under both states:
  - connected → assert `cloud_verify:OkVerifyQueued` and then `OkVerified` (if probe simulated ok).
  - not connected → assert `cloud_verify:ErrNoAccount` and no enqueue.
- Render `HistoryScreen(viewModel)` and assert chip text is “Drive: Connected” when fake says connected.

Example Files
- `app/src/test/java/com/mfme/kernel/cloud/CloudConnectPropertyTest.kt`
- `app/src/test/java/com/mfme/kernel/cloud/HistoryCloudStatusPropertyTest.kt`

CI Notes
- Stick to Robolectric; avoid device-only Google services. Keep fakes pure; no network.

Done When
- Tests reproduce the original silent behavior (red) on current main and pass (green) after fixes, with receipts verifying outcomes.
