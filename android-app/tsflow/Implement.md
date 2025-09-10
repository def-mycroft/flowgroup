# TroubleshootFlow Update Guide — Implement

Context
- brief_id: imbued-sycamore · b5e49dc5-b13d-4a60-891d-2c5716a238cc
- property_id: property-history-crash
- updated: 2025-09-10

Purpose
- Apply the smallest patch under the contract to flip the property from red to green without regressions.

Update Checklist
- Use the red stacktrace/logs to decide layer: UI compose/state, repository flow collection, or DAO emissions.
- UI guardrails: ensure `HistoryScreen` gracefully handles empty lists and null `message`/`mime` fields; key lists are namespaced (already done).
- Repository/DAO: verify Room queries don’t emit an initial null/invalid item; ensure Flow catch blocks map to error state without crashing.
- Keep changes surgical; reference `brief_id`/`property_id` in PR/title/commit.

Non-Goals
- Feature redesign; telemetry schema changes.

Done When
- Property test passes locally; sibling tests remain green; telemetry shows `patch_applied` and final ok outcome.
