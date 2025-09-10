# TroubleshootFlow Update Guide — Emit

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: TBA

Purpose
- Produce typed Receipts and link them to spans so the investigation and fix are observable and auditable.

Receipt Shape (V2)
- {ok, code, tsUtcIso, adapter, message?, envelopeId?, envelopeSha256?, spanId, receiptSha256}

Update Checklist
- Emit a Receipt for both red and green runs; include `brief_id`, `property_id`, `rev` in span/receipt context.
- Map outcomes to codes: ok_fixed, ok_explained, err_non_repro, err_invariant_fail, err_adapter.
- Store in Room (`receipts`) and NDJSON sink; verify entries exist.
- Add a short message linking the PR/commit that applied the fix.

Done When
- Receipts exist for failing and passing runs with correct codes and bindings, and NDJSON export matches Room.

