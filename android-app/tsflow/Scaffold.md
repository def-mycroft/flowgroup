# TroubleshootFlow — Scaffold

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-drive-connect-visible
- updated: 2025-09-11

Purpose
- Create a deterministic, injectable harness to reproduce the “connect Drive shows no feedback / verify no-op” behavior and flip it.

Seams to Introduce (no real network)
- Auth seam: wrap `GoogleSignIn.getLastSignedInAccount()` behind an injectable `AuthFacade` or extend `TokenProvider` with an injectable fake in tests.
- Drive seam: keep `DriveServiceFactory` but allow injection of `TokenProvider` for tests (fake returns `hasAccount=true/false`).
- Telemetry seam: fake `ReceiptEmitter` that records emitted receipts in-memory for assertions.

Harness
- Use Robolectric for Compose UI tests and in-memory Room for persistence.
- Render `CloudScreen()` and simulate:
  - account selection success → fake `TokenProvider.hasAccount()` returns true;
  - account cancellation → fake returns false.
- Render `HistoryScreen(viewModel)` and assert that the cloud chip reflects the fake connection state.
- Invoke “Verify now” and assert a receipt is recorded with the correct code.

Artifacts
- Test classes under `app/src/test/java/com/mfme/kernel/cloud/`:
  - `CloudConnectPropertyTest.kt` (connect/disconnect + verify receipts)
  - `HistoryCloudStatusPropertyTest.kt` (chip reflects connection)

Invariants
- Same fake inputs → same state/receipts; no device services or network calls.

Done When
- Tests can force red (no state/receipt) on current main and then pass (green) after the implementation fixes.
