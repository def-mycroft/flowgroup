# TroubleshootFlow Update Guide — Scaffold

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: 2025-09-10

Purpose
- Build a deterministic repro harness with seams to localize and reproduce the property in red.

Update Checklist
- Seeds: fix time to `Instant.EPOCH` where used; stable PRNG not required here.
- Harness: Robolectric + in-memory Room with real `KernelRepositoryImpl` (see `RepositoryIdempotencyTest`).
- Fixtures: construct `ReceiptEmitter`, `ErrorEmitter`, `EnvelopeChainer` and DB DAOs as in tests.
- Drive flow: call `repo.saveFromLocation("{}")`, then render `HistoryScreen(KernelViewModel(repo, VaultConfig(context)))`.
- Telemetry: ensure `ReceiptEmitter.begin/end/emitV2` is exercised during save and visible via `observeReceipts()`.

Artifacts
- Test class: `app/src/test/java/com/mfme/kernel/history/HistoryNoCrashPropertyTest.kt`.
- Optional Compose test that clicks “History” on `PluginPanelHost` and asserts “Receipts” appears.

Invariants
- Same seed/setup → same outcome; Room is in-memory per test; no network.

Done When
- The harness consistently reproduces a crash (red) on main, with clear traces, or proves green after fix.
