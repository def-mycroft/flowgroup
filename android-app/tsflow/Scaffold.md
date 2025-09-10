# TroubleshootFlow Update Guide — Scaffold

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: TBA

Purpose
- Build a deterministic repro harness with seams (adapters) to localize the defect and reproduce the property in red.

Update Checklist
- Decide seeds: fixed clock/time and PRNG for repeatable runs.
- Choose harness: in-memory Room + real `KernelRepositoryImpl` under Robolectric, or fakes for location/repository.
- Create fixtures: fixed coordinate provider, clean DB per test run.
- Drive flow: simulate log-location call then navigate to/read History data source.
- Record spans around actions (start→end) and ensure failures propagate to the test as red.

Artifacts
- New test class under `app/src/test/.../history/` for property repro.
- Test doubles (fakes) if Android dependencies block determinism.

Invariants
- Same seed → same outcome; no IO in core; only edges mocked.

Done When
- The harness consistently reproduces the failure on main with minimal setup and clear traces.

