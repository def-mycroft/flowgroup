#!/usr/bin/env python3
"""wrap vism — payload -> envelope."""

from __future__ import annotations

import argparse
import base64
import binascii
import dataclasses
from contextlib import contextmanager
import datetime as _dt
import hashlib
import json
import os
import shutil
import stat
import sys
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Dict, Generic, Optional, Tuple, TypeVar

VISM_CODE = "wrap"
__version__ = "0.2.1"
DOCS_TARGET = str(Path("~/.local/share/vism/docs").expanduser() / f"{VISM_CODE}.{__version__}.md")

T = TypeVar("T")


@dataclass(frozen=True)
class Outcome(Generic[T]):
    ok: bool
    value: Optional[T] = None
    error: Optional[str] = None
    receipts: Dict[str, Any] = dataclasses.field(default_factory=dict)


@dataclass(frozen=True)
class Payload:
    bytes_: bytes
    media_type: str
    source: str


@dataclass(frozen=True)
class Envelope:
    id: str
    content_hash: str
    created_at: str
    media_type: str
    source: str


@dataclass(frozen=True)
class Ctx:
    clock: Callable[[], _dt.datetime]
    sha256: Callable[[bytes], str]
    uuid7: Callable[[], str]
    span: Callable[[str, Any], Any]


def _utc_now() -> _dt.datetime:
    return _dt.datetime.now(tz=_dt.timezone.utc)


def _sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _uuid7() -> str:
    return uuid.uuid7().hex if hasattr(uuid, "uuid7") else uuid.uuid4().hex


@contextmanager
def _null_span(name: str, **fields: Any):  # pragma: no cover - telemetry stub
    yield


DEFAULT_CTX = Ctx(
    clock=_utc_now,
    sha256=_sha256,
    uuid7=_uuid7,
    span=lambda name, **fields: _null_span(name, **fields),
)


# ----- core -------------------------------------------------------------------

def wrap_apply(payload: Payload, ctx: Ctx = DEFAULT_CTX) -> Outcome[Envelope]:
    """Wrap ``payload`` into an :class:`Envelope`."""

    with ctx.span("wrap.apply", source=payload.source, media_type=payload.media_type):
        if not payload.bytes_:
            return Outcome(False, None, "empty_payload", receipts={})

        content_hash = ctx.sha256(payload.bytes_)
        eid = ctx.uuid7()
        created_at = ctx.clock().isoformat()
        env = Envelope(
            id=eid,
            content_hash=content_hash,
            created_at=created_at,
            media_type=payload.media_type,
            source=payload.source,
        )
        receipts = {"content_hash": content_hash, "created_at": created_at}
        return Outcome(True, env, None, receipts=receipts)


# ----- helpers ----------------------------------------------------------------

def _req_from_stream_or_file(arg: Optional[str]) -> Dict[str, Any]:
    if arg == "-":
        return json.load(sys.stdin)
    if arg:
        with open(arg, "r", encoding="utf-8") as fh:
            return json.load(fh)
    if not sys.stdin.isatty():
        data = sys.stdin.read()
        if data.strip():
            return json.loads(data)
    return {}


def _parse_overrides(items: Optional[list[str]]) -> Dict[str, str]:
    out: Dict[str, str] = {}
    for part in items or []:
        if "=" not in part:
            raise ValueError(f"bad override '{part}', expected k=v")
        k, v = part.split("=", 1)
        out[k.strip()] = v.strip()
    return out


def _merge_overrides(base: Dict[str, Any], overrides: Dict[str, str]) -> Dict[str, Any]:
    d = dict(base or {})
    inp = dict(d.get("input") or {})
    for k, v in overrides.items():
        inp[k] = v
    d["input"] = inp
    d.setdefault("vism", VISM_CODE)
    return d


def _interactive_request() -> Dict[str, Any]:
    def ask(prompt: str, default: str = "") -> str:
        resp = input(f"{prompt} [{default}]: ").strip()
        return resp or default

    print("No JSON input detected. Enter values interactively.", file=sys.stderr)
    b64 = ask("Enter base64 bytes_")
    media_type = ask("Enter media_type", "application/octet-stream")
    source = ask("Enter source", "cli")
    return {"vism": VISM_CODE, "input": {"bytes_": b64, "media_type": media_type, "source": source}}


def _out_to_json(out: Outcome[Envelope]) -> Dict[str, Any]:
    val = dataclasses.asdict(out.value) if out.value else None
    return {"ok": out.ok, "value": val, "error": out.error, "receipts": out.receipts}


# ----- installer and docs -----------------------------------------------------

def _install_paths(code: str = VISM_CODE) -> Tuple[str, str]:
    home = Path.home()
    share_dir = home / ".local" / "share" / "vism"
    bin_dir = home / ".local" / "bin"
    share_dir.mkdir(parents=True, exist_ok=True)
    bin_dir.mkdir(parents=True, exist_ok=True)
    return str(share_dir / f"{code}.py"), str(bin_dir / code)


_LAUNCHER = """#!/usr/bin/env bash
set -euo pipefail
PYTHON_BIN="${PYTHON_BIN:-python3}"
exec "$PYTHON_BIN" "{module_path}" "$@"
"""


def install_self() -> Dict[str, str]:
    module_target, launcher_target = _install_paths(VISM_CODE)
    this_path = os.path.abspath(sys.argv[0])
    shutil.copyfile(this_path, module_target)
    with open(launcher_target, "w", encoding="utf-8") as f:
        f.write(_LAUNCHER.format(module_path=module_target))
    st = os.stat(launcher_target)
    os.chmod(launcher_target, st.st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    return {"module": module_target, "launcher": launcher_target}


def emit_document(path: str = DOCS_TARGET) -> str:
    p = Path(path).expanduser()
    p.parent.mkdir(parents=True, exist_ok=True)
    lines = [
        f"# docs for {VISM_CODE}",
        f"version: {__version__}",
        "input: Payload{bytes_, media_type, source}",
        "output: Outcome<Envelope{content_hash,id,created_at,media_type,source}>",
        "invariants:",
        "- hash_is_deterministic",
        "- rejects_empty",
        "failures:",
        "- empty_payload",
        "- bad_base64",
        "- invalid_input",
    ]
    p.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return str(p)


# ----- properties -------------------------------------------------------------

def _hash_is_deterministic() -> None:
    payload = Payload(b"abc", "text/plain", "unit-test")
    out1 = wrap_apply(payload, DEFAULT_CTX)
    out2 = wrap_apply(payload, DEFAULT_CTX)
    assert out1.ok and out2.ok
    assert out1.value.content_hash == out2.value.content_hash
    print("hash_is_deterministic: ok", file=sys.stderr)


def _rejects_empty() -> None:
    payload = Payload(b"", "text/plain", "unit-test")
    out = wrap_apply(payload, DEFAULT_CTX)
    assert not out.ok and out.error == "empty_payload"
    print("rejects_empty: ok", file=sys.stderr)


def run_self_tests() -> int:
    _hash_is_deterministic()
    _rejects_empty()
    return 0


# ----- CLI --------------------------------------------------------------------

def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog=VISM_CODE, description="Wrap vism — Payload → Envelope")
    p.add_argument("--input", help="JSON request path or '-' for stdin")
    p.add_argument("--override", action="append", help="Override input field (k=v)")
    p.add_argument("--pretty", action="store_true", help="Pretty-print output JSON")
    p.add_argument("--install", action="store_true", help="Install launcher to ~/.local/bin")
    p.add_argument("--document", action="store_true", help=f"Write docs to {DOCS_TARGET}")
    p.add_argument("--version", action="store_true", help="Print version and exit")
    p.add_argument("--self-test", action="store_true", help="Run self-tests and exit")
    return p


def main(argv: Optional[list[str]] = None) -> int:
    ap = build_parser()
    args = ap.parse_args(argv)

    if args.version:
        print(__version__)
        return 0

    if args.self_test:
        return run_self_tests()

    if args.document:
        path = emit_document()
        print(path)
        return 0

    if args.install:
        info = install_self()
        print("installed:", file=sys.stderr)
        print(f"  module  : {info['module']}", file=sys.stderr)
        print(f"  launcher: {info['launcher']}", file=sys.stderr)
        print(f"next: {VISM_CODE} --help", file=sys.stderr)
        return 0

    try:
        req = _req_from_stream_or_file(args.input)
    except Exception:
        req = {}

    if not req:
        req = _interactive_request()

    if args.override:
        try:
            overrides = _parse_overrides(args.override)
            req = _merge_overrides(req, overrides)
        except ValueError:
            out = Outcome[Envelope](ok=False, error="invalid_input", receipts={})
            json.dump(_out_to_json(out), sys.stdout, ensure_ascii=False)
            sys.stdout.write("\n")
            return 1

    try:
        if req.get("vism") not in (None, VISM_CODE):
            raise ValueError("wrong vism")
        inp = req.get("input")
        if not isinstance(inp, dict):
            raise ValueError("missing input")
        b64 = inp.get("bytes_")
        if not isinstance(b64, str):
            raise ValueError("missing bytes_")
        media_type = (inp.get("media_type") or "application/octet-stream").strip() or "application/octet-stream"
        source = (inp.get("source") or "cli").strip() or "cli"
    except Exception:
        out = Outcome[Envelope](ok=False, error="invalid_input", receipts={})
        json.dump(_out_to_json(out), sys.stdout, indent=2 if args.pretty else None, ensure_ascii=False)
        sys.stdout.write("\n")
        return 1

    try:
        raw = base64.b64decode(b64, validate=True)
    except (binascii.Error, ValueError):
        out = Outcome[Envelope](ok=False, error="bad_base64", receipts={})
        json.dump(_out_to_json(out), sys.stdout, indent=2 if args.pretty else None, ensure_ascii=False)
        sys.stdout.write("\n")
        return 1

    payload = Payload(bytes_=raw, media_type=media_type, source=source)
    out = wrap_apply(payload, DEFAULT_CTX)
    json.dump(_out_to_json(out), sys.stdout, indent=2 if args.pretty else None, ensure_ascii=False)
    sys.stdout.write("\n")
    return 0 if out.ok else 1


run_cli = main


__all__ = [
    "Payload",
    "Envelope",
    "Outcome",
    "Ctx",
    "wrap_apply",
    "install_self",
    "emit_document",
    "run_self_tests",
    "run_cli",
    "__version__",
]


if __name__ == "__main__":  # pragma: no cover
    raise SystemExit(main())

