# TroubleshootFlow Update Guide — Implement

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: TBA

Purpose
- Apply the smallest patch under the contract to flip the property from red to green without regressions.

Update Checklist
- Localize fault from repro/stacktrace; decide layer (UI mapping, persistence, adapters).
- Make the minimal change consistent with arrow contracts (core stays pure, IO at edges).
- Add guards for nullability/lifecycle if the issue is UI; align schema if persistence.
- Keep changes surgical; reference brief/property IDs in PR/title/commit message.

Non-Goals
- Feature additions or redesign; telemetry schema changes.

Done When
- Property test passes locally; sibling tests remain green; telemetry shows `patch_applied` and final ok outcome.

