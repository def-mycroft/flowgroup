# TroubleshootFlow Update Guide — Instrument

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: TBA

Purpose
- Bind spans/logs to the brief/property and ensure every run emits a Receipt (red or green) with lineage.

Span/Receipt Schema
- Spans: {trace_id, case_id, brief_id, property_id, adapter_ids[], seed, rev, outcome, duration_ms}
- Receipt V2: {ok, code, tsUtcIso, adapter, message?, envelopeId?, envelopeSha256?, spanId, receiptSha256}

Update Checklist
- Ensure `ReceiptEmitter.begin/end/emitV2` wrap the relevant actions (log location, load history).
- Attach `brief_id` and `property_id` via span/receipt metadata (message or binding API).
- Map errors to codes using `ErrorEmitter` taxonomy.
- Verify spans close on both success and failure, and a Receipt row is written.

Done When
- A repro or fix run yields traceable spans and a Receipt row linked to this brief/property and commit.

