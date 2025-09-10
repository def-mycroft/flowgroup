# TroubleshootFlow Update Guide — Test

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: 2025-09-10

Purpose
- Execute the property to prove red→green and guard against regression.

Update Checklist
- Build in-memory DB: `Room.inMemoryDatabaseBuilder(context, KernelDatabase::class.java).build()`.
- Wire emitters: `ReceiptEmitter(db.receiptDao(), db.spanDao(), NdjsonSink(context))`, `ErrorEmitter`.
- Create repo: `KernelRepositoryImpl(context, db, Dispatchers.IO, receiptEmitter, errorEmitter, db.spanDao(), EnvelopeChainer(...))`.
- Repro: `repo.saveFromLocation("{}")`; then render `HistoryScreen(KernelViewModel(repo, VaultConfig(context)))`.
- Assertions: no crash; `repo.observeReceipts().first().any { it.adapter == "location" } == true`.

Example Files
- Unit test: `app/src/test/java/com/mfme/kernel/history/HistoryNoCrashPropertyTest.kt`.
- Compose UI smoke: see pattern in `PluginPanelHostTest` to assert “Receipts” after switching tabs.

CI Notes
- Prefer Robolectric (`testDebugUnitTest` where appropriate). Avoid device-only dependencies.

Done When
- Property test fails on main (red) and passes after the fix (green) with stability in CI.
