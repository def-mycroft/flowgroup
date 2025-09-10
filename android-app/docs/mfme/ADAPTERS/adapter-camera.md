# Adapter Camera — Adapter Contract

Id: adapter-camera
Updated: 2025-09-10T19:17:48Z

Port & Responsibilities
- Defines the IO seam; normalizes inputs; delegates to core arrow.

Contract
- Inputs: typed; encoding/time rules (UTF-8, UTC).
- Effects: none beyond delegation; errors mapped to TelemetryCode.

Fakes/Stubs
- Describe a minimal fake for tests and docs.

Failure Modes
- permission_denied, empty_input, device_unavailable, invalid_media.

Telemetry Fields
- adapter, span_id, ok, code, envelope_sha256? , message? .
