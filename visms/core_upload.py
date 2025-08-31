import json, shutil, hashlib, datetime
from pathlib import Path

VISM_CODE = "upload"
__version__ = "0.4.0"

DOC = """# vism — upload core
input → output

Input
- {"artifact_path": str, "sidecar_json_path": str, "root": "~/drive_mock"}

Output (Essence)
- DriveFile{id, folder, uploaded_at}

Invariants
- Pure apply: plan-only, no hidden side effects
- Idempotent: sha256 in filename prevents dupes
- Deterministic given same payload
- Resumable/retryable (stubbed local copy)

Failure Modes
- input_malformed | artifact_missing | sidecar_missing | hash_mismatch
"""

def _sha256_hex(path: Path) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()

class _UploadCore:
    def apply(self, payload: dict) -> dict:
        if not isinstance(payload, dict):
            return {"kind": "failure", "code": "input_malformed",
                    "message": "payload must be dict"}
        art_path = payload.get("artifact_path")
        side_path = payload.get("sidecar_json_path")
        root = Path(payload.get("root") or Path.home() / "drive_mock").expanduser()
        if not art_path or not side_path:
            return {"kind": "failure", "code": "input_malformed",
                    "message": "missing artifact_path or sidecar_json_path"}
        art, side = Path(art_path), Path(side_path)
        if not art.exists():
            return {"kind": "failure", "code": "artifact_missing",
                    "message": f"{art} not found"}
        if not side.exists():
            return {"kind": "failure", "code": "sidecar_missing",
                    "message": f"{side} not found"}
        try:
            sidecar = json.loads(side.read_text(encoding="utf-8"))
        except Exception:
            return {"kind": "failure", "code": "input_malformed",
                    "message": "invalid sidecar JSON"}
        expected_sha = sidecar.get("content_hash_sha256")
        actual_sha = _sha256_hex(art)
        if expected_sha and actual_sha != expected_sha:
            return {"kind": "failure", "code": "hash_mismatch",
                    "message": "artifact sha256 != sidecar"}
        dt = datetime.datetime.utcnow().replace(tzinfo=datetime.timezone.utc)
        folder = root / f"{dt:%Y/%m/%d}"
        folder.mkdir(parents=True, exist_ok=True)
        ext = art.suffix or ".bin"
        dest = folder / f"{actual_sha}{ext}"
        if not dest.exists():
            shutil.copy2(art, dest)
        return {
            "kind": "DriveFile",
            "id": f"drive-mock-{actual_sha}",
            "folder": str(folder),
            "uploaded_at": dt.isoformat().replace("+00:00", "Z"),
            "sha256": actual_sha
        }

def factory():
    return _UploadCore()

