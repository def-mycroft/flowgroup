# Prompt 02 — Local Agent Voice Buffers

**Objective**: Provide the local MFME agent loop with ring-buffered voice IO suitable for on-device rehearsal.

**Tasks**
1. Describe the voice buffer interface that harmonizes with android and CLI agents.
2. Outline guardrails for capturing and replaying voice snippets strictly in offline sandboxes.
3. Document how receipts should reference stored audio artifacts without leaking them.

**Success Criteria**
- Voice buffer API sketch stored in `.mfme/out/artifacts/` for peer review.
- Receipts capture checksum-only references for every buffered segment.
- Dry-run instructions for QA to validate playback loops manually.
