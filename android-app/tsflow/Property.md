# TroubleshootFlow Update Guide — Property

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: 2025-09-10

Purpose
- Encode a falsifiable claim that binds the user story to behavior and can be turned red→green.

Property Statement
- Given a fresh in-memory database and default app state, when a single location capture is saved via `KernelRepository.saveFromLocation("{}")` and the History screen is opened immediately, then:  
  - No uncaught exceptions occur during composition/render of `HistoryScreen`, and  
  - At least one Receipt with `adapter == "location"` appears, and  
  - An Envelope exists (observed via `observeEnvelopes`) consistent with the capture.

Scope
- Surface: Capture → History panel navigation (`PanelsBuiltin.kt`, `HistoryScreen.kt`).
- Data: Receipt V2 and Envelope entities (`KernelDatabase`, DAOs).
- Adapters: `ReceiptEmitter`, `KernelRepositoryImpl.saveFromLocation`.

Check Method
- Deterministic test using Robolectric + in-memory Room (see `RepositoryIdempotencyTest`).
- Seed time to `Instant.EPOCH` for capture actions where applicable.
- Compose test can assert that “Receipts” header exists and that no crash occurs while collecting flows.

Assertions
- Process alive (no thrown exception) while switching to History.
- `observeReceipts().first()` contains a row with `adapter == "location"`.
- UI smoke: History shows the “Receipts” section and no error banner.

Edge Cases
- Empty history (baseline), rapid navigation to History immediately after capture, large history list, and SMS-related receipts interleaving.

Telemetry
- Spans/Receipts include: `{adapter: "location" | "ui_history", spanId, tsUtcIso, receiptSha256}`.
- Bind `brief_id`/`property_id` via message or dedicated tags once available.

Done When
- The property is actionable and reproducible as a red on current main, with a clear path to flip it green.
