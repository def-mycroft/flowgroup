# TroubleshootFlow — Property

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-drive-connect-visible
- updated: 2025-09-11

Purpose
- Encode a falsifiable claim mapping the Drive connect story to observable behavior and receipts.

Property Statement
- Given default app state with no signed-in account, when the user selects a Google account on Cloud screen and then taps “Verify now”, then:
  - The Cloud screen reflects a connected state (shows the account or a ‘Connected’ indicator), and
  - A typed Receipt is emitted for the verification attempt (`OkVerifyQueued|OkVerified|ErrNoAccount|ErrAuthScope`), and
  - History shows the cloud chip as “Drive: Connected” when an account with Drive scope is present; otherwise “Not connected”.

Scope
- Surface: `CloudScreen` connect/verify, `HistoryScreen` cloud chip.
- Seams: `DriveServiceFactory` and `TokenProvider.hasAccount()` as the single source of truth for connection.
- Telemetry: typed receipts for connect/verify outcomes.

Check Method
- Compose/Robolectric test exercising the onboarding path:
  - Simulate successful account selection (via injected seam; see Scaffold) → assert Cloud reflects connected and History chip shows connected.
  - Tap “Verify now” → assert a Receipt is recorded with an expected code.
  - Simulate sign-out → assert both Cloud and History reflect not connected and a Receipt is emitted on verify with `ErrNoAccount`.

Assertions
- Connection state is observable outside `CloudScreen` (i.e., `HistoryScreen` reflects it from the shared seam), and
- Verify action never silently does nothing; it emits a Receipt with outcome.

Edge Cases
- Account without Drive scope, cancelled chooser, toggling Wi‑Fi only has no effect on connection state but should not break verify.

Telemetry
- Every verify and connect/disconnect path yields a span and a typed Receipt bound to `{brief_id, property_id}`.

Done When
- The property runs red on current main if behavior is silent/incorrect and flips green after fixes; receipts/spans evidence the outcome.
