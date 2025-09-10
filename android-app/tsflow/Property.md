# TroubleshootFlow Update Guide — Property

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash (recommended)
- updated: TBA

Purpose
- Encode the story as a single falsifiable statement that must hold. This becomes the red→green proof.

Write The Property
- Statement: Given a fresh install with location permission granted, when a single location is logged and History is opened immediately, then History completes without crashing and renders at least one item whose coordinates equal the logged value within epsilon.
- Epsilon: choose a numeric tolerance (e.g., 1e-5) for lat/lng comparisons.
- Scope: surfaces (Log → History), data (lat/lng/timestamp), adapters in play (location, persistence, UI renderer).

Check Method
- Prefer a deterministic test around ViewModel/repo with fakes or in-memory Room; UI smoke test optional.
- Assertions: process alive (no uncaught exceptions), 1+ items visible/exposed, coordinates ≈ logged.

Edge Cases
- Permission approximate vs precise; denied→granted flow; empty/large history; activity recreation.

Telemetry
- Spans tagged with {brief_id, property_id, seed, rev, outcome}; events property_start/invariant_fail|pass/receipt_emitted.

Done When
- The property text is clear, scoped, and actionable, and you can make it fail on current main.

