#!/usr/bin/env bash
set -euo pipefail

prompt_id="00_bootstrap_flowgroup"
session_tag="${SUSHI_SESSION_TAG:-sushi-20250921-0215}"

timestamp() {
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
mfme_root="${repo_root}/.mfme"
out_root="${mfme_root}/out"

prompts_dir="${out_root}/prompts"
artifacts_dir="${out_root}/artifacts"
receipts_dir="${out_root}/receipts"
logs_dir="${out_root}/logs"
state_dir="${out_root}/state"
rcpts_dir="${out_root}/rcpts"

start_ts="$(timestamp)"

mkdir -p "${prompts_dir}" "${artifacts_dir}" "${receipts_dir}" "${logs_dir}" "${state_dir}" "${rcpts_dir}"

log_file="${logs_dir}/run_sushi.log"
{
  echo "[${start_ts}] session ${session_tag} — bootstrap ritual starting"
} >> "${log_file}"

autogen_mode="dry_run"
if [[ -n "${OPENAI_API_KEY:-}" ]]; then
  autogen_mode="enabled"
fi

write_receipt() {
  local step="$1"
  local description="$2"
  local artifact_path="$3"
  local now="$(timestamp)"
  local receipt_file="${receipts_dir}/00_bootstrap.${step}.json"
  REPO_ROOT="${repo_root}" \
  RECEIPT_FILE="${receipt_file}" \
  SESSION_TAG="${session_tag}" \
  STEP="${step}" \
  DESC="${description}" \
  ART="${artifact_path}" \
  NOW="${now}" \
  PROMPT_ID="${prompt_id}" \
  AUTOGEN_MODE="${autogen_mode}" \
  python - <<'PY'
import json
import hashlib
import os
from pathlib import Path

repo_root = Path(os.environ["REPO_ROOT"])
receipt_path = Path(os.environ["RECEIPT_FILE"])
artifact_spec = os.environ.get("ART", "")
artifact_path = Path(artifact_spec) if artifact_spec else None
checksum = None
if artifact_path and artifact_path.exists():
    if artifact_path.is_file():
        checksum = hashlib.sha256(artifact_path.read_bytes()).hexdigest()
    else:
        checksum = None
if artifact_path:
    try:
        artifact_rel = str(artifact_path.relative_to(repo_root))
    except ValueError:
        artifact_rel = str(artifact_path)
else:
    artifact_rel = None
receipt = {
    "tag": os.environ["SESSION_TAG"],
    "prompt": os.environ["PROMPT_ID"],
    "step": os.environ["STEP"],
    "timestamp": os.environ["NOW"],
    "status": "ok",
    "description": os.environ["DESC"],
    "artifact": artifact_rel,
    "checksum": checksum,
    "autogen": os.environ["AUTOGEN_MODE"],
}
receipt_path.write_text(json.dumps(receipt, indent=2) + "\n")
PY
}

write_receipt "step01_scaffold" "created sushi output scaffolding" "${out_root}"

overview_prompt="${prompts_dir}/session_${session_tag}_overview.md"
cat <<EOF_PROMPT > "${overview_prompt}"
# Sushi Ritual Session Overview

- **Session Tag**: ${session_tag}
- **Prompt Kernel**: ${prompt_id}
- **Scope**: Prepare local MFME workspace for internal-only experimentation.
- **Guarantees**: Reversible-first posture, manual release gate, preview lane only.

## Next Prompts
1. Run prompt '01_cadence_shadow_sim' to bootstrap cadence shadow simulation tasks.
2. Run prompt '02_local_agent_voice_buffers' to enable voice buffer plumbing for local agents.
3. Run prompt '03_phase4_functional_sim' to progress toward Phase-4 functional simulation readiness.

- Re-run ./.mfme/bin/run_sushi.sh whenever you need fresh receipts.
- Inspect generated artifacts with the preview lane instructions under tools/preview_lane/README.md.
- Autogen mode: ${autogen_mode}.
EOF_PROMPT

cadence_prompt="${prompts_dir}/01_cadence_shadow_sim.md"
cat <<'EOF_PROMPT' > "${cadence_prompt}"
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
EOF_PROMPT

voice_prompt="${prompts_dir}/02_local_agent_voice_buffers.md"
cat <<'EOF_PROMPT' > "${voice_prompt}"
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
EOF_PROMPT

phase_prompt="${prompts_dir}/03_phase4_functional_sim.md"
cat <<'EOF_PROMPT' > "${phase_prompt}"
# Prompt 03 — Phase-4 Functional Simulation Prep

**Objective**: Chart the remaining blockers between the current sushi kernel and a full Phase-4 functional simulation.

**Tasks**
1. Inventory outstanding integration points across agents, android surfaces, and back-end cadence loops.
2. Score each blocker by risk, reversible effort, and dependency depth.
3. Nominate preview-lane validation experiments that keep the kernel internal/unsafe.

**Success Criteria**
- Artifact summarizing blockers stored in `.mfme/out/artifacts/` with owner assignments.
- Receipts capture prioritization rationale and suggested experiment order.
- Debrief updated with 10-minute validation steps for each high-risk item.
EOF_PROMPT

write_receipt "step02_prompts" "recorded dry-run prompts" "${prompts_dir}"

autogen_note_file="${logs_dir}/autogen_status.txt"
if [[ "${autogen_mode}" == "enabled" ]]; then
  cat <<'EOF_AUTOGEN' > "${autogen_note_file}"
Autogen pass permitted — OPENAI_API_KEY detected. Confirm preview-lane settings before invoking remote calls.
EOF_AUTOGEN
else
  cat <<'EOF_AUTOGEN' > "${autogen_note_file}"
Autogen pass skipped — no OPENAI_API_KEY detected. Remaining in dry-run documentation mode.
EOF_AUTOGEN
fi

write_receipt "step03_autogen" "evaluated autogen posture" "${autogen_note_file}"

run_state_file="${state_dir}/run.${session_tag}.json"
python - <<PY
import json
from pathlib import Path

repo_root = Path("${repo_root}").resolve()
run_path = Path("${run_state_file}")
data = {
    "session_tag": "${session_tag}",
    "prompt": "${prompt_id}",
    "start": "${start_ts}",
    "end": None,
    "status": "running",
    "autogen_mode": "${autogen_mode}",
    "paths": {
        "prompts": str(Path("${prompts_dir}").resolve().relative_to(repo_root)),
        "artifacts": str(Path("${artifacts_dir}").resolve().relative_to(repo_root)),
        "receipts": str(Path("${receipts_dir}").resolve().relative_to(repo_root)),
        "logs": str(Path("${logs_dir}").resolve().relative_to(repo_root)),
        "state": str(Path("${state_dir}").resolve().relative_to(repo_root)),
    },
}
run_path.write_text(json.dumps(data, indent=2) + "\n")
PY

write_receipt "step04_run_state" "initialized run state" "${run_state_file}"

cat <<EOF_FLAGS > "${state_dir}/dev.flags.json"
{
  "reversible_first": false,
  "audit_live": false,
  "preview_lane_only": true,
  "kernel_status": "internal_unsafe",
  "session_tag": "${session_tag}",
  "updated": "${start_ts}"
}
EOF_FLAGS

write_receipt "step05_flags" "documented development flags" "${state_dir}/dev.flags.json"

prompt_count=$(find "${prompts_dir}" -type f | wc -l | tr -d ' ')
artifact_count=$(find "${artifacts_dir}" -type f | wc -l | tr -d ' ')
receipt_count=$(find "${receipts_dir}" -type f -name '00_bootstrap.*.json' | wc -l | tr -d ' ')

state_debrief="${state_dir}/debrief.${session_tag}.md"
cat <<EOF_DEBRIEF > "${state_debrief}"
# Sushi Ritual Debrief — ${session_tag}

- Start: ${start_ts} (UTC)
- Autogen Mode: ${autogen_mode}
- Kernel Status: internal/unsafe — preview lane only.

## Inventory Summary
- Prompts: ${prompt_count}
- Artifacts: ${artifact_count}
- Receipts: ${receipt_count}

## Validation (next 10-minute commands)
1. tree .mfme/out -L 2
2. cat .mfme/out/state/dev.flags.json
3. cat .mfme/out/logs/autogen_status.txt

## Notes
- Confirm that no artifacts leave the preview lane until all invariants pass.
- Receipts refreshed on every run for traceability.
- Re-run this bootstrap if new engineers join the session.
EOF_DEBRIEF

write_receipt "step06_debrief" "captured debrief summary" "${state_debrief}"

end_ts="$(timestamp)"
python - <<PY
import json
from datetime import datetime
from pathlib import Path

run_path = Path("${run_state_file}")
if run_path.exists():
    data = json.loads(run_path.read_text())
else:
    data = {}
data["end"] = "${end_ts}"
data["status"] = "complete"
try:
    def parse(ts):
        return datetime.fromisoformat(ts.replace("Z", "+00:00"))
    start = parse(data.get("start", "${start_ts}"))
    end = parse("${end_ts}")
    data["duration_seconds"] = max(int((end - start).total_seconds()), 0)
except Exception:
    pass
run_path.write_text(json.dumps(data, indent=2) + "\n")
PY

{
  echo "[${end_ts}] session ${session_tag} — bootstrap ritual complete"
} >> "${log_file}"

printf "Sushi bootstrap complete for %s (mode: %s)\n" "${session_tag}" "${autogen_mode}"
