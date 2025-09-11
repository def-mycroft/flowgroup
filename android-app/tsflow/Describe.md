
TroubleshootFlow — Describe (imbued-sycamore · b5e49dc5)

Story (voice memo)
- “Open Cloud → Google Drive. Tap ‘Connect Drive’, pick my account. Nothing changes. Tap ‘Verify now’ — nothing. Toggle ‘Upload on Wi‑Fi only’ — no effect. Go to History and it still shows ‘Drive: Not connected’. Receipts show no cloud activity. I don’t know what’s going on.”

Expected vs Actual
- Expected: After selecting an account, Cloud shows connected identity or actionable error. ‘Verify now’ performs a check and surfaces a receipt/outcome. History chip reflects connection state.
- Actual: Account chooser appears but no visible state change; ‘Verify now’ yields no feedback; History chip remains “Not connected”.

Brief / Binding
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- primary_property: property-drive-connect-visible
- scope: Cloud screen (sign‑in + verify), History cloud status chip, `DriveServiceFactory`/`TokenProvider` seams.

Inputs → Outputs (contract)
- Input: user taps connect, selects a Google account with Drive scope; user taps ‘Verify now’.
- Output: observable state transition (connected/disconnected), typed Receipts for auth/verify outcomes, History chip reflects state.

Hypotheses
- UI state is local-only; not persisted/observable outside `CloudScreen`.
- No span/receipt emitted on success/failure, leaving no trail.
- `DriveServiceFactory.hasAccount()` returns false due to missing scope or stale cache.
- ‘Verify now’ enqueues work without surfaced feedback or is a no‑op when no account.

Invariants (MFME)
- Every attempt emits a typed Receipt bound to brief/property.
- Connectivity status derives from a single source of truth (TokenProvider.hasAccount or equivalent), not duplicated checks.
- Verify action always yields an observable outcome (ok or error), never silent.

Failure Modes
- empty_story (N/A), missing_property (fix), missing_telemetry, adapter_shadowing (edge only), non_reproducible, drift_from_brief.

Done (Describe)
- The story is bound to a falsifiable property and explicit seams to measure and fix.

