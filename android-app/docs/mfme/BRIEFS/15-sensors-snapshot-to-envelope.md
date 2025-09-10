# 15 Sensors Snapshot To Envelope — Morphism Brief

Id: 15-sensors-snapshot-to-envelope
Updated: 2025-09-10T19:17:49Z

Input → Output
- Input: As named; see adapters and callers.
- Output: Persisted artifact(s) as implied by title.

Invariants
- Idempotence under declared keys (e.g., sha256 for payload-equivalence).
- UTC timestamps, UTF-8 bytes at all boundaries.
- Atomicity for file artifacts (tmp → fsync → rename).

Failure Modes
- empty_input, oversize, permission_denied, device_unavailable, unknown.

Adapters
- See corresponding adapter specs under ADAPTERS/ for IO boundaries.

Composition
- Upstream: UI/intent/worker invoking this arrow.
- Downstream: Telemetry receipts, span binding, optional vault/log export.

Telemetry
- Span emitted with adapter + start/end; receipt recorded with code and sha256 when applicable.
