# TroubleshootFlow Update Guide — Instrument

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: 2025-09-10

Purpose
- Bind spans/logs to the brief/property and ensure every run emits a Receipt (red or green) with lineage.

Span/Receipt Schema
- Spans: {trace_id, case_id, brief_id, property_id, adapter_ids[], seed, rev, outcome, duration_ms}
- Receipt V2: {ok, code, tsUtcIso, adapter, message?, envelopeId?, envelopeSha256?, spanId, receiptSha256}

Update Checklist
- Confirm `KernelRepositoryImpl.saveFromLocation` calls `ReceiptEmitter.begin/emitV2/end` (it does).  
  File: `app/src/main/java/com/mfme/kernel/data/KernelRepositoryImpl.kt`.
- For UI navigation to History, consider emitting a lightweight UI span via a UI-scoped emitter wrapper (adapter: `ui_history`).
- Until dedicated fields exist, bind `brief_id`/`property_id` in the Receipt `message` (e.g., `"brief:imbued-sycamore property:property-history-crash"`).
- Verify `ErrorEmitter` maps thrown errors into receipts with codes and closes the span.

Done When
- Repro and fix runs yield traceable spans and a Receipt row linked (at least via message) to this brief/property and commit.
