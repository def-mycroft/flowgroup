"""
see: morph brief vism wrap easy-birch 125a40bb

easy-birch
125a40bb-9afa-477a-9ad1-8ccafb5a0032
2025-08-28T13:55:37.748323-06:00

"""
# vism_wrap.py
from __future__ import annotations

from dataclasses import dataclass, asdict
from typing import Optional, Dict, Any, Generic, TypeVar
from datetime import datetime, timezone
import base64, hashlib, json, sys, uuid

T = TypeVar("T")


# ---------- Core Result Type ----------
@dataclass(frozen=True)
class Outcome(Generic[T]):
    ok: bool
    value: Optional[T] = None
    error: Optional[str] = None
    receipts: Dict[str, Any] = None


# ---------- Artifacts ----------
@dataclass(frozen=True)
class Payload:
    # bytes_: base64-encoded payload bytes (matches your CLI example)
    bytes_: str
    media_type: str
    source: str


@dataclass(frozen=True)
class Envelope:
    id: str
    content_hash_sha256: str
    created_at: str          # UTC ISO-8601
    media_type: str
    source: str


# ---------- Helpers ----------
def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()

def _sha256(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()

def _decode_payload_bytes(b64_or_bytes: Optional[str]) -> bytes:
    if not b64_or_bytes:
        return b""
    # Treat input as base64 text. If it's not valid b64, raise to caller.
    return base64.b64decode(b64_or_bytes)


# ---------- Vism (pure) ----------
def vism_wrap(p: Payload) -> Outcome[Envelope]:
    """
    Pure arrow: Payload -> Envelope
    Invariants:
      - deterministic sha256 over exact bytes
      - unique id (uuid4)
      - created_at is UTC ISO-8601
    Failure modes:
      - empty payload -> error 'empty_payload'
      - bad base64   -> error 'invalid_base64'
    """
    try:
        raw = _decode_payload_bytes(p.bytes_)
    except Exception as e:
        return Outcome(ok=False, error=f"invalid_base64: {e}", receipts={"wrap": "error"})

    if len(raw) == 0:
        return Outcome(ok=False, error="empty_payload", receipts={"wrap": "error"})

    env = Envelope(
        id=str(uuid.uuid4()),
        content_hash_sha256=_sha256(raw),
        created_at=_utc_now_iso(),
        media_type=p.media_type,
        source=p.source,
    )
    return Outcome(ok=True, value=env, receipts={"wrap": "ok"})


# ---------- JSON (de)serialization ----------
def _payload_from_json(d: Dict[str, Any]) -> Payload:
    # Accept either {"bytes_": "..."} or {"bytes_b64": "..."} for convenience
    bytes_ = d.get("bytes_")
    if bytes_ is None and "bytes_b64" in d:
        bytes_ = d["bytes_b64"]
    return Payload(
        bytes_=bytes_ or "",
        media_type=d.get("media_type", "application/octet-stream"),
        source=d.get("source", "unknown"),
    )

def _out_to_json(out: Outcome[Envelope]) -> Dict[str, Any]:
    return {
        "ok": out.ok,
        "value": asdict(out.value) if out.value else None,
        "error": out.error,
        "receipts": out.receipts or {},
    }


# ---------- CLI Entry ----------
def main() -> int:
    """
    Reads a JSON request from stdin:
      {
        "vism": "wrap",
        "input": {"bytes_": "BASE64...", "media_type": "text/plain", "source": "cli"}
      }
    Writes a JSON Outcome to stdout.
    """
    try:
        req = json.load(sys.stdin)
        vism = req.get("vism")
        if vism != "wrap":
            raise ValueError(f"unsupported vism: {vism}")

        payload = _payload_from_json(req.get("input", {}))
        out = vism_wrap(payload)
        json.dump(_out_to_json(out), sys.stdout, ensure_ascii=False)
        sys.stdout.write("\n")
        return 0 if out.ok else 1
    except Exception as e:
        json.dump({"ok": False, "error": f"cli_error: {e}", "value": None, "receipts": {}}, sys.stdout)
        sys.stdout.write("\n")
        return 2


if __name__ == "__main__":
    raise SystemExit(main())

