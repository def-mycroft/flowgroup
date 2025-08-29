# begin code 
#!/usr/bin/env python3
# vismmorphtoc.py

"""匣 vism — morphtoc

Vault -> stable markdown table ("matrix") of morph briefs.

MFME / 匣 canon:
- Arrow-first: pure core with FS/Clock edge effects via CLI.
- Properties: idempotent bytes for identical inputs; strict sort by UTC mtime desc then name.
- Telemetry: concise receipts in Outcome; human confirmation to STDERR.
- Installable: --install creates ~/.local/bin/vismmorphtoc launcher.
- Docs artifact: --document writes a minimal validation file.

CLI examples:
  echo '{"vism":"morphtoc","input":{"path":"/field","pattern":"morph*.md"}}' \
    | python vismmorphtoc.py --input - --output /field/morphtoc.md

  python vismmorphtoc.py --install
  vismmorphtoc --help
"""

from __future__ import annotations

import argparse
import glob
import json
import os
import shutil
import stat
import sys
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from typing import Any, Dict, Generic, List, Optional, Tuple, TypeVar

# ----- Canon metadata ----------------------------------------------------------

VISM_CODE = "vismmorphtoc"
__version__ = "1.0.0"

DOCS_TARGET = "/field/docs-vismmorphtoc.py"

# ----- Result types ------------------------------------------------------------

T = TypeVar("T")

@dataclass(frozen=True)
class Outcome(Generic[T]):
    ok: bool
    value: Optional[T] = None
    error: Optional[str] = None
    receipts: Dict[str, Any] = None

@dataclass(frozen=True)
class MorphtocEntry:
    date: str   # YYYY-MM-DD (UTC)
    time: str   # HH:MM:SS (UTC)
    file: str   # basename

@dataclass(frozen=True)
class MorphtocValue:
    path: str
    generated_at: str       # ISO-8601 UTC
    entries: List[MorphtocEntry]

@dataclass(frozen=True)
class CopiedEntry:
    file: str   # basename
    src: str    # absolute source path
    dest: str   # absolute dest path
    date: str   # YYYY-MM-DD (UTC)
    time: str   # HH:MM:SS (UTC)

@dataclass(frozen=True)
class CopiedValue:
    generated_at: str       # ISO-8601 UTC
    copied: List[CopiedEntry]

# ----- Utilities ---------------------------------------------------------------

def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")

def _expand(p: str) -> str:
    return os.path.abspath(os.path.expanduser(os.path.expandvars(p)))

def _parse_params(s: Optional[str]) -> Dict[str, str]:
    if not s:
        return {}
    out: Dict[str, str] = {}
    for part in s.split(","):
        part = part.strip()
        if not part:
            continue
        if "=" not in part:
            raise ValueError(f"bad param '{part}', expected k=v")
        k, v = part.split("=", 1)
        out[k.strip()] = v.strip()
    return out

def _ensure_parent(path: str) -> None:
    parent = os.path.dirname(path)
    if parent and not os.path.isdir(parent):
        os.makedirs(parent, exist_ok=True)

def _error_out(msg: str, receipts: Optional[Dict[str, Any]] = None) -> int:
    out = Outcome[None](ok=False, value=None, error=msg, receipts=receipts or {})
    json.dump(asdict(out), sys.stdout, ensure_ascii=False)
    sys.stdout.write("\n")
    print(f"[{VISM_CODE}] error: {msg}", file=sys.stderr)
    return 1

# ----- Core arrow (with FS/Clock adapters at edges) ---------------------------

def _glob_morphs(root: str, pattern: str) -> List[Tuple[str, float]]:
    pat = os.path.join(root, "**", pattern)
    paths = glob.glob(pat, recursive=True)
    out: List[Tuple[str, float]] = []
    for p in paths:
        if not os.path.isfile(p):
            continue
        base = os.path.basename(p)
        if not base.endswith(".md"):
            continue
        if not base.startswith("morph"):
            continue
        try:
            st = os.stat(p)
        except FileNotFoundError:
            continue
        out.append((os.path.abspath(p), float(st.st_mtime)))
    return out

def _entries(items: List[Tuple[str, float]], limit: Optional[int]) -> List[MorphtocEntry]:
    items_sorted = sorted(items, key=lambda t: (-t[1], os.path.basename(t[0]).casefold()))
    if limit is not None and limit >= 0:
        items_sorted = items_sorted[:limit]
    out: List[MorphtocEntry] = []
    for path, mtime in items_sorted:
        dt = datetime.fromtimestamp(mtime, tz=timezone.utc)
        out.append(MorphtocEntry(
            date=dt.date().isoformat(),
            time=dt.strftime("%H:%M:%S"),
            file=os.path.basename(path),
        ))
    return out

def _render(entries: List[MorphtocEntry]) -> str:
    lines = [
        "# Morphism Table of Contents",
        "",
        "| Date       | Time     | File |",
        "|------------|----------|------|",
    ]
    for e in entries:
        lines.append(f"| {e.date} | {e.time} | {e.file} |")
    lines.append("")
    return "\n".join(lines)

def vism_morphtoc(vault_path: str,
                  out_path: str,
                  pattern: str = "morph*.md",
                  limit: Optional[int] = None) -> Outcome[MorphtocValue]:
    receipts: Dict[str, Any] = {"stage": "start"}
    root = _expand(vault_path)
    target = _expand(out_path)
    receipts["vault"] = root
    receipts["target"] = target

    if not os.path.isdir(root):
        return Outcome(ok=False, error="path_not_found", receipts=receipts)

    found = _glob_morphs(root, pattern)
    receipts["found"] = len(found)
    if not found:
        return Outcome(ok=False, error="no_morphs_found", receipts=receipts)

    ents = _entries(found, limit)
    md = _render(ents)

    try:
        _ensure_parent(target)
        with open(target, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(md)
    except OSError as e:
        receipts["io_error"] = str(e)
        return Outcome(ok=False, error="io_error", receipts=receipts)

    val = MorphtocValue(path=target, generated_at=_utc_now_iso(), entries=ents)
    receipts["stage"] = "ok"
    receipts["rows"] = len(ents)
    receipts["bytes"] = len(md.encode("utf-8"))
    return Outcome(ok=True, value=val, receipts=receipts)

def copy_recent_morphs(vault_path: str,
                       pattern: str = "morph*.md",
                       n: int = 17,
                       dest_root: str = "/l/tmp") -> Outcome[CopiedValue]:
    receipts: Dict[str, Any] = {"stage": "start"}
    root = _expand(vault_path)
    dest = _expand(dest_root)
    receipts["vault"] = root
    receipts["dest"] = dest

    if not os.path.isdir(root):
        return Outcome(ok=False, error="path_not_found", receipts=receipts)

    found = _glob_morphs(root, pattern)
    receipts["found"] = len(found)
    if not found:
        return Outcome(ok=False, error="no_morphs_found", receipts=receipts)

    items_sorted = sorted(found, key=lambda t: (-t[1], os.path.basename(t[0]).casefold()))
    if n is not None and n >= 0:
        items_sorted = items_sorted[:n]

    try:
        os.makedirs(dest, exist_ok=True)
    except OSError as e:
        receipts["io_error"] = str(e)
        return Outcome(ok=False, error="io_error", receipts=receipts)

    copied_entries: List[CopiedEntry] = []
    for src, mtime in items_sorted:
        target = os.path.join(dest, os.path.basename(src))
        try:
            shutil.copy2(src, target)
        except OSError as e:
            receipts["io_error"] = str(e)
            return Outcome(ok=False, error="io_error", receipts=receipts)
        dt = datetime.fromtimestamp(mtime, tz=timezone.utc)
        copied_entries.append(CopiedEntry(
            file=os.path.basename(src),
            src=src,
            dest=target,
            date=dt.date().isoformat(),
            time=dt.strftime("%H:%M:%S"),
        ))

    val = CopiedValue(generated_at=_utc_now_iso(), copied=copied_entries)
    receipts["stage"] = "ok"
    receipts["copied"] = len(copied_entries)
    return Outcome(ok=True, value=val, receipts=receipts)

# ----- Request/Response glue --------------------------------------------------

def _req_from_stream_or_file(arg: Optional[str]) -> Dict[str, Any]:
    if arg == "-":
        return json.load(sys.stdin)
    if arg:
        with open(arg, "r", encoding="utf-8") as fh:
            return json.load(fh)
    return {}

def _merge_overrides(base: Dict[str, Any], overrides: Dict[str, str]) -> Dict[str, Any]:
    d = dict(base or {})
    inp = dict(d.get("input") or {})
    for k, v in overrides.items():
        if k == "limit":
            try:
                inp[k] = int(v)
            except ValueError:
                raise ValueError(f"limit must be int, got '{v}'")
        else:
            inp[k] = v
    d["input"] = inp
    return d

def _out_to_json(out: Outcome[MorphtocValue]) -> Dict[str, Any]:
    val = None
    if out.value is not None:
        val = {
            "path": out.value.path,
            "generated_at": out.value.generated_at,
            "entries": [asdict(e) for e in out.value.entries],
        }
    return {"ok": out.ok, "value": val, "error": out.error, "receipts": out.receipts or {}}

def _copy_out_to_json(out: Outcome[CopiedValue]) -> Dict[str, Any]:
    val = None
    if out.value is not None:
        val = {
            "generated_at": out.value.generated_at,
            "copied": [asdict(e) for e in out.value.copied],
        }
    return {"ok": out.ok, "value": val, "error": out.error, "receipts": out.receipts or {}}

def _interactive_request() -> Dict[str, Any]:
    def ask(prompt: str, default: Optional[str]) -> str:
        suff = f" [{default}]" if default else ""
        resp = input(f"{prompt}{suff}: ").strip()
        return resp or (default or "")
    print("No JSON input detected. Enter values interactively.", file=sys.stderr)
    path = ask("Enter vault path", "/field")
    pattern = ask("Enter glob pattern", "morph*.md")
    limit_raw = ask("Enter limit (optional)", "")
    limit = int(limit_raw) if limit_raw else None
    return {"vism": "morphtoc", "input": {"path": path, "pattern": pattern, "limit": limit}}

# ----- Installer and docs -----------------------------------------------------

def _install_paths(code: str = VISM_CODE) -> Tuple[str, str]:
    home = os.path.expanduser("~")
    share_dir = os.path.join(home, ".local", "share", "vism")
    bin_dir = os.path.join(home, ".local", "bin")
    os.makedirs(share_dir, exist_ok=True)
    os.makedirs(bin_dir, exist_ok=True)
    return os.path.join(share_dir, f"{code}.py"), os.path.join(bin_dir, code)

_LAUNCHER = """#!/usr/bin/env bash
set -euo pipefail
PYTHON_BIN="${{PYTHON_BIN:-python3}}"
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
    _ensure_parent(path)
    lines = [
        f"# docs for {VISM_CODE}",
        f"# version: {__version__}",
        f"# generated: {_utc_now_iso()}",
        "# type: documentation artifact (markdown-in-.py)",
        "***",
        "匣 vism base def validation successful",
    ]
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")
    return path

# ----- CLI --------------------------------------------------------------------

def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(
        prog=VISM_CODE,
        description="Generate a morph table of contents for an Obsidian vault."
    )
    p.add_argument("--input", help="JSON request path or '-' for stdin")
    p.add_argument("--output", help="Write table to this path; default <vault>/morphtoc.md")
    p.add_argument("--params", help="Comma-separated overrides: path,pattern,limit")
    p.add_argument("--install", action="store_true", help="Install launcher to ~/.local/bin")
    p.add_argument("--document", action="store_true", help=f"Write docs to {DOCS_TARGET}")
    p.add_argument("--recent", action="store_true", help="Copy recent morphs to /l/tmp")
    p.add_argument("--n", type=int, default=17, help="Count for --recent (default 17)")
    p.add_argument("--version", action="store_true", help="Print version and exit")
    return p

def main(argv: Optional[List[str]] = None) -> int:
    ap = build_parser()
    args = ap.parse_args(argv)

    if args.version:
        print(f"{VISM_CODE} {__version__}")
        return 0

    acted = False

    if args.document:
        acted = True
        path = emit_document()
        print(path)

    if args.install:
        acted = True
        info = install_self()
        print("installed:", file=sys.stderr)
        print(f"  module  : {info['module']}", file=sys.stderr)
        print(f"  launcher: {info['launcher']}", file=sys.stderr)
        print(f"next: {VISM_CODE} --help", file=sys.stderr)
    if args.recent:
        try:
            req = _req_from_stream_or_file(args.input) if args.input else {}
            if args.params:
                req = _merge_overrides(req, _parse_params(args.params))

            if req.get("vism") not in (None, "morphtoc"):
                raise ValueError(f"unsupported vism: {req.get('vism')}")

            inp = req.get("input") or {}
            vault = inp.get("path") or "/field"
            pattern = inp.get("pattern") or "morph*.md"
            n = args.n if args.n is not None else 17

            out = copy_recent_morphs(vault, pattern, n)

            json.dump(_copy_out_to_json(out), sys.stdout, ensure_ascii=False)
            sys.stdout.write("\n")

            if out.ok and out.value:
                count = len(out.value.copied)
                print(f"[{VISM_CODE}] copied {count} to /l/tmp", file=sys.stderr)
                return 0
            else:
                print(f"[{VISM_CODE}] no files copied.", file=sys.stderr)
                return 1

        except KeyboardInterrupt:
            return _error_out("aborted")
        except Exception as e:
            return _error_out(f"cli_error: {e}")

    if args.input or not acted:
        try:
            req = _req_from_stream_or_file(args.input) if args.input else {}
            if not req:
                req = _interactive_request()

            if args.params:
                req = _merge_overrides(req, _parse_params(args.params))

            if req.get("vism") != "morphtoc":
                raise ValueError(f"unsupported vism: {req.get('vism')}")

            inp = req.get("input") or {}
            vault = inp.get("path") or "/field"
            pattern = inp.get("pattern") or "morph*.md"
            limit = inp.get("limit")
            if isinstance(limit, str) and limit.strip():
                limit = int(limit)
            elif limit is None:
                limit = None

            out_path = args.output or os.path.join(_expand(vault), "morphtoc.md")

            out = vism_morphtoc(vault, out_path, pattern, limit)

            json.dump(_out_to_json(out), sys.stdout, ensure_ascii=False)
            sys.stdout.write("\n")

            if out.ok and out.value:
                rows = len(out.value.entries)
                print(
                    f"[{VISM_CODE}] wrote matrix to {out.value.path} "
                    f"(rows={rows}, generated_at={out.value.generated_at} UTC)",
                    file=sys.stderr,
                )
                return 0
            else:
                print(f"[{VISM_CODE}] no matrix written.", file=sys.stderr)
                return 1

        except KeyboardInterrupt:
            return _error_out("aborted")
        except Exception as e:
            return _error_out(f"cli_error: {e}")

    return 0

if __name__ == "__main__":
    raise SystemExit(main())

# end code 
