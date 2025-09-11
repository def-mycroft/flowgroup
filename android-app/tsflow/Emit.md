# TroubleshootFlow — Emit

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-drive-connect-visible
- updated: 2025-09-11

Purpose
- Emit typed Receipts for connect/verify so the flow is auditable end‑to‑end and never silent.

Receipt Shape (V2)
- {ok, code, tsUtcIso, adapter, message?, envelopeId?, envelopeSha256?, spanId, receiptSha256}

Taxonomy (for this flow)
- Adapter `cloud_ui`: `OkDriveConnected`, `ErrAuthCancelled`, `ErrAuthNoScope`.
- Adapter `cloud_verify`: `OkVerifyQueued`, `OkVerified`, `ErrNoAccount`, `ErrVerifyFailed(code)`.

Checklist
- On connect result, emit `cloud_ui` receipts with brief/property tags in `message` until first‑class fields exist.
- On verify, emit `cloud_verify` receipts for both no‑account and account present paths; if probing, include a compact code/message.
- Ensure receipts are visible via Room (`receiptDao().observeAll()`) and NDJSON sink; spot‑check during tests.
- Include a message linking the fixing commit: `fix: imbued-sycamore property-drive-connect-visible @<sha>`.

Done When
- Receipts exist and are correctly coded for tap/cancel/verify outcomes in failing and passing runs, with stable NDJSON parity.
