# TroubleshootFlow Update Guide — Emit

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: 2025-09-10

Purpose
- Produce typed Receipts and link them to spans so investigation and fix are observable and auditable.

Receipt Shape (V2)
- {ok, code, tsUtcIso, adapter, message?, envelopeId?, envelopeSha256?, spanId, receiptSha256}

Update Checklist
- Ensure save path emits Receipt on both success and failure (already via `KernelRepositoryImpl` and `ErrorEmitter`).
- During repro/fix, include `brief_id` and `property_id` in `message` or a tag (until first-class fields exist).
- Map outcomes to codes consistently: `ok_fixed`, `ok_explained`, `err_non_repro`, `err_invariant_fail`, `err_adapter`.
- Verify rows in Room (`receiptDao().observeAll()`) and mirror lines in NDJSON sink.
- Add message linking PR/commit that fixes the property (e.g., `fix: imbued-sycamore property-history-crash @<sha>`).

Done When
- Receipts exist for failing and passing runs with correct codes/bindings, and NDJSON export matches Room.
