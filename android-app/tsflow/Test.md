# TroubleshootFlow Update Guide — Test

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: TBA

Purpose
- Execute the property as tests to prove red→green and guard against regression.

Update Checklist
- Create a deterministic test using in-memory Room and `KernelRepositoryImpl` (see existing Robolectric tests).
- Seed time/PRNG; reset DB per run; use fixed coordinates.
- Assertions: no crash when loading history after logging one location; 1+ items exposed; coordinates ≈ logged.
- Optionally add a UI smoke test for History navigation.

CI Notes
- Prefer `test`/Robolectric over full instrumentation; fake external services.

Done When
- Property test fails on main (red) and passes after the fix (green) with stability in CI.

