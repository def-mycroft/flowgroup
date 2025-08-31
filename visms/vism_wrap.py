"""
Wrap vism stub - 要 point , this is what should be updated 
"""

from __future__ import annotations
import argparse
import base64
import binascii
import dataclasses
import datetime as _dt
import hashlib
import json
import sys
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Dict, Optional, TypedDict, Union, cast

__all__ = [
    "Envelope",
    "Outcome",
    "Ports",
    "wrap_core",
    "run_cli",
    "__version__",
]

__version__ = "0.2.0"

@dataclass(frozen=True)
class Envelope:
    id: str
    content_hash_sha256: str
    created_at: str
    media_type: str
    source: str

@dataclass(frozen=True)
class Outcome:
    ok: bool
    value: Optional[Envelope]
    error: Optional[str]
    receipts: Dict[str, Any]

class Ports(TypedDict):
    clock: Callable[[], _dt.datetime]
    crypto: Callable[[bytes], str]

def _utc_now() -> _dt.datetime:
    return _dt.datetime.now(tz=_dt.timezone.utc)

def _sha256_hex(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()

DEFAULT_PORTS: Ports = {
    "clock": _utc_now,
    "crypto": _sha256_hex,
}

_WRAP_NAMESPACE = uuid.UUID("5a9a8e3d-1b55-43c6-9b0b-0a6a6e1a0d67")

def _b64_to_bytes(s: str) -> bytes:
    try:
        return base64.b64decode(s, validate=True)
    except (binascii.Error, ValueError) as e:
        raise ValueError("invalid_base64") from e

def _ensure_media_type(mt: Optional[str]) -> str:
    mt = (mt or "").strip()
    return mt or "application/octet-stream"

def _ensure_source(src: Optional[str]) -> str:
    src = (src or "").strip()
    return src or "cli"

def _make_id_from_hash(hash_hex: str) -> str:
    return str(uuid.uuid5(_WRAP_NAMESPACE, hash_hex))

def _isoformat_utc(dt: _dt.datetime) -> str:
    return dt.isoformat()

def wrap_core(input_obj: Dict[str, Any], ports: Optional[Ports] = None) -> Outcome:
    p = ports or DEFAULT_PORTS
    try:
        if not isinstance(input_obj, dict):
            raise KeyError
        if input_obj.get("vism") != "wrap":
            pass
        payload = input_obj["input"]
        if not isinstance(payload, dict):
            raise KeyError
        b64 = payload["bytes_"]
        media_type = _ensure_media_type(payload.get("media_type"))
        source = _ensure_source(payload.get("source"))
        if not isinstance(b64, str):
            raise KeyError
    except KeyError:
        return Outcome(ok=False, value=None, error="input_malformed",
                       receipts={"wrap": "error", "ports": {"clock": "utc", "crypto": "sha256"}})
    try:
        raw_bytes = _b64_to_bytes(b64)
    except ValueError as e:
        return Outcome(ok=False, value=None, error=str(e),
                       receipts={"wrap": "error", "ports": {"clock": "utc", "crypto": "sha256"}})
    if len(raw_bytes) == 0:
        return Outcome(ok=False, value=None, error="empty_payload",
                       receipts={"wrap": "error", "ports": {"clock": "utc", "crypto": "sha256"}})
    hash_hex = p["crypto"](raw_bytes)
    env_id = _make_id_from_hash(hash_hex)
    created_at = _isoformat_utc(p["clock"]())
    envelope = Envelope(id=env_id, content_hash_sha256=hash_hex,
                        created_at=created_at, media_type=media_type, source=source)
    receipts = {"wrap": "ok", "ports": {"clock": "utc", "crypto": "sha256"}}
    return Outcome(ok=True, value=envelope, error=None, receipts=receipts)

_DOCS_MD = """# Wrap Vism — Morph Brief 冊 documentation
Documentation intentionally stripped down for module integrity.
"""

def _print_json(obj: Any, pretty: bool) -> None:
    if pretty:
        json.dump(obj, sys.stdout, indent=2, sort_keys=False)
    else:
        json.dump(obj, sys.stdout, separators=(",", ":"), ensure_ascii=False)
    sys.stdout.write("\n")
    sys.stdout.flush()

def _read_stdin_json() -> Union[Dict[str, Any], list, None]:
    data = sys.stdin.read()
    if not data.strip():
        return None
    try:
        return cast(Union[Dict[str, Any], list], json.loads(data))
    except json.JSONDecodeError:
        return None

def run_cli(argv: Optional[list[str]] = None) -> int:
    parser = argparse.ArgumentParser(prog="vism_wrap", description="Wrap Vism — Payload → Envelope")
    parser.add_argument("--pretty", action="store_true")
    parser.add_argument("--document", action="store_true")
    parser.add_argument("-o","--output", type=str, default="")
    parser.add_argument("--version", action="store_true")
    args = parser.parse_args(argv)
    if args.version:
        print(__version__)
        return 0
    if args.document:
        if args.output:
            outdir = Path(args.output)
            outdir.mkdir(parents=True, exist_ok=True)
            (outdir / "wrap_vism.md").write_text(_DOCS_MD, encoding="utf-8")
            print(str(outdir / "wrap_vism.md"))
        else:
            print(_DOCS_MD)
        return 0
    parsed = _read_stdin_json()
    if parsed is None or not isinstance(parsed, dict):
        _print_json({"ok": False,"value": None,"error": "input_malformed",
                    "receipts": {"wrap": "error","ports": {"clock": "utc", "crypto": "sha256"}}}, pretty=args.pretty)
        return 1
    outcome = wrap_core(parsed, ports=DEFAULT_PORTS)
    if outcome.value is not None:
        value = dataclasses.asdict(outcome.value)
    else:
        value = None
    _print_json({"ok": outcome.ok,"value": value,"error": outcome.error,"receipts": outcome.receipts}, pretty=args.pretty)
    return 0

if __name__ == "__main__":
    raise SystemExit(run_cli())
