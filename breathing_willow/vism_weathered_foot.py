"""

要 point prompt  here is "morph prompt codex payload bytes → envelope meta weathered-foot 251ad0f0"


## 要 point prompt — vism #1: `wrap` (Payload → Envelope)

Implement the **WrapVism** as a pure developer morphism that turns a `Payload` into an `Envelope`. Use the existing Vism scaffold (base class, ports, context, registry, CLI). Do not duplicate scaffold code; add only what’s required for this vism.

**Contract (must match exactly)**

* Name: `wrap`

* Version: `0.1.0`

* Input type: `Payload{bytes_: bytes, media_type: str, source: str}`

* Output type: `Envelope{id: str, content_hash: str, created_at: str, media_type: str, source: str}`

* Invariants:

1. `id` is non-empty and unique

2. `content_hash = sha256(payload.bytes_)`

3. `created_at` is UTC ISO-8601

4. Pure: same input → same `content_hash`

* Failures: `"empty_payload"` when `bytes_` is empty

**Apply**

* Start a telemetry span `"wrap.apply"` with fields: `source`, `media_type`.

* If `bytes_` empty → return `Outcome(False, error="empty_payload")`.

* Else:

* `h = ctx.crypto.sha256(bytes_)`

* `eid = ctx.crypto.uuidv7()`

* `ts = ctx.clock.now().isoformat()`

* Construct `Envelope(eid, h, ts, payload.media_type, payload.source)`

* Return `Outcome(True, value=envelope, receipts={"content_hash": h, "created_at": ts})`

**Properties (runnable, no IO)**

* `hash_is_deterministic`: applying to `Payload(b"abc")` twice yields same `content_hash` equal to `sha256("abc")`.

* `rejects_empty`: applying to `Payload(b"")` returns `ok=False` and `error="empty_payload"`.

**Registry & CLI**

* Register instance under `REGISTRY["wrap"]`.

* Ensure CLI path runs this vism when `--vism wrap` (or corresponding JSON stdin) is used; JSON uses base64 for `bytes_` and is decoded before constructing `Payload`. Output JSON must include `ok`, `value` (as dict), `error`, `receipts`.

**Acceptance**

* Running properties prints success with no assertion failures.

* Example (stdin JSON):

`{"vism":"wrap","input":{"bytes_":"YWJj","media_type":"text/plain","source":"share://browser"}}`

returns `ok:true`, `value.media_type:"text/plain"`, `receipts.content_hash` = `sha256("abc")`.

Keep the vism core pure, isolate side-effects behind ports, and include span logging for traceability.

"""
