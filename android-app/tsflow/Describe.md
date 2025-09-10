# TroubleshootFlow Update Guide — Describe

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: TBA (set in Property step)
- branch/rev: TBA (e.g., main@<sha>)
- date/owner: TBA

Purpose
- Capture the story and turn it into an explicit contract (inputs → outputs, invariants, failure modes) before any code changes.

User Story (paste verbatim)
- When I open the app and use one of the buttons to log location, and then select the "history" button, app crashes. I should be able to see what I just logged (ie a gps pin).

Update Checklist
- Environment: device/emulator, Android version, app variant, commit SHA.
- Permissions: location precise/approx; while-in-use/always; prompts shown/choice made.
- Repro path: exact button labels/routes from launch → log → history; timing notes.
- Inputs/Outputs: what data is written (lat/lng/timestamp) and what view must render.
- Invariants: no crash on History; latest entry visible; core stays pure (adapters at edges).
- Scope: what’s in/out (history list/map, not background tracking, etc.).
- Failure modes: non_reproducible, permission_mismatch, schema_drift, adapter_shadowing, lifecycle_race.
- Determinism: seed time/PRNG; fixed coordinates for tests.

Evidence To Attach
- Minimal steps to reproduce.
- logcat snippet showing the crash cause.
- Screenshots/gifs if useful.

Telemetry Anchors
- Plan fields: {trace_id, case_id, brief_id, property_id, seed, rev} and events: describe_start/describe_done.

Done When
- Story + environment documented, a single falsifiable property candidate is articulated, and trace fields are chosen.

