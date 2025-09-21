# Prompt 01 — Cadence Shadow Simulation

**Objective**: Establish a dry-run cadence that mirrors production simulation while staying within internal preview lanes.

**Tasks**
1. Wire up cadence schedulers so they can invoke the sushi ritual builder with synthetic inputs.
2. Capture shadow simulation telemetry into `.mfme/out/logs/cadence/` without touching production channels.
3. Emit receipts documenting simulated cadence windows and observed invariants.

**Success Criteria**
- Shadow cadence runs can be re-triggered locally without mutating external systems.
- Receipts include timestamps, session tags, and summarized telemetry hashes.
- Preview lane reviewers can reconstruct the run from emitted artifacts.
