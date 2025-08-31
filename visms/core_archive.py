import json, base64, hashlib, datetime
from pathlib import Path

# morph vism Archiver v0.4.0 purer-boulder 32d0e0a7
VISM_CODE = "archive"
__version__ = "0.4.0"

DOC = """# vism — archive core (plan)
input → output

Input
- {"envelope": {...}, "bytes_": BASE64, "root": "/tmp"?}

Output (Essence)
- ArchivePlan: deterministic paths + sidecar json, no I/O

Invariants
- Pure apply(payload); no filesystem effects
- Deterministic given same payload
- Date shard derived from envelope.created_at (UTC)
- Validates bytes hash vs envelope.content_hash_sha256

Failure Modes
- input_malformed | invalid_base64 | invalid_created_at | hash_mismatch
"""

def _sha256_hex(b: bytes) -> str:
    h = hashlib.sha256()
    h.update(b)
    return h.hexdigest()

def _dt_from_iso(s: str) -> datetime.datetime:
    s = s.strip()
    if s.endswith("Z"):
        s = s[:-1] + "+00:00"
    dt = datetime.datetime.fromisoformat(s)
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=datetime.timezone.utc)
    return dt.astimezone(datetime.timezone.utc)

def _infer_ext(media_type: str | None, filename: str | None) -> str:
    if filename and "." in filename:
        return filename.rsplit(".", 1)[-1].lower()
    mt = (media_type or "").lower()
    if mt.startswith("text/"):
        return "txt"
    if mt.endswith("/json"):
        return "json"
    if mt.startswith("image/") and "/" in mt:
        return mt.split("/", 1)[1]
    if mt.startswith("audio/") and "/" in mt:
        return mt.split("/", 1)[1]
    if mt.startswith("video/") and "/" in mt:
        return mt.split("/", 1)[1]
    return "bin"

class _ArchiveCore:
    def apply(self, payload: dict) -> dict:
        if not isinstance(payload, dict):
            return {"kind": "failure", "code": "input_malformed", "message": "payload must be object"}
        env = payload.get("envelope")
        b64 = payload.get("bytes_")
        root = payload.get("root") or "/tmp"
        if not isinstance(env, dict) or b64 is None:
            return {"kind": "failure", "code": "input_malformed", "message": "missing envelope or bytes_"}
        try:
            raw = base64.b64decode(b64, validate=True)
        except Exception:
            return {"kind": "failure", "code": "invalid_base64", "message": "bytes_ not strict base64"}
        if not isinstance(env.get("content_hash_sha256"), str):
            return {"kind": "failure", "code": "input_malformed", "message": "envelope.content_hash_sha256 missing"}
        if _sha256_hex(raw) != env["content_hash_sha256"]:
            return {"kind": "failure", "code": "hash_mismatch", "message": "bytes sha256 != envelope.content_hash_sha256"}
        try:
            dt = _dt_from_iso(env.get("created_at", ""))
        except Exception:
            return {"kind": "failure", "code": "invalid_created_at", "message": "created_at not ISO-8601"}
        ext = _infer_ext(env.get("media_type"), env.get("filename"))
        base = Path(root) / "archive" / f"{dt:%Y/%m/%d}"
        data_path = str(base / f"{env['content_hash_sha256']}.{ext}")
        sidecar_path = str(base / f"{env['content_hash_sha256']}.json")
        sidecar = {
            "id": env.get("id", ""),
            "content_hash_sha256": env["content_hash_sha256"],
            "created_at": env.get("created_at", ""),
            "media_type": env.get("media_type", "application/octet-stream"),
            "source": env.get("source", "unknown"),
            "filename": env.get("filename"),
            "meta": env.get("meta") if isinstance(env.get("meta"), dict) and env.get("meta") else None
        }
        if sidecar.get("meta") is None and "meta" in sidecar:
            sidecar.pop("meta")
        return {
            "kind": "plan",
            "sha256": env["content_hash_sha256"],
            "root": str(Path(root)),
            "data_path": data_path,
            "sidecar_json_path": sidecar_path,
            "sidecar": sidecar,
            "ext": ext
        }

def factory():
    return _ArchiveCore()
