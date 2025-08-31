#!/usr/bin/env python3
# wrapper.py — vism wrapper (v0.6.0) — imbued-limb redraft
# Contract: pure core with four symbols [VISM_CODE, __version__, DOC, factory() -> instance.apply(dict)->dict]
# CLI surfaces:
#   - Normal run (JSON in → JSON out): default reads stdin; or use --input/-i to read a file; write to stdout or --output/-o
#   - --document: print core.DOC (ensures visible "input → output")
#   - --help-core: print minimal interface info
#   - --install / --uninstall: install/remove launcher at ~/.local/bin/<VISM_CODE> and stage core under ~/.local/share/vism/<VISM_CODE>/core.py
#
# Exit codes are stable and JSON error objects are emitted on stdout for machine use.

import argparse
import hashlib
import importlib
import importlib.util
import io
import json
import os
import shutil
import sys
import textwrap
import time
from pathlib import Path
from typing import Any, Dict

EXITCODES = {
    "ok": 0,
    "empty_input": 10,
    "spec_absent": 11,
    "broken_invariant": 12,
    "adapter_leak": 13,
    "trace_loss": 14,
    "drift": 15,
    "parse_error": 16,
}

# ---------- utils ----------

def _iso_now() -> str:
    # millisecond timestamp (local time with trailing Z for simplicity)
    t = time.time()
    base = time.strftime("%Y-%m-%dT%H:%M:%S", time.localtime(t))
    return f"{base}.{int((t % 1) * 1000):03d}Z"

def jhash(obj: Any) -> str:
    try:
        s = json.dumps(obj, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    except Exception:
        s = str(obj)
    return hashlib.sha256(s.encode("utf-8")).hexdigest()

def _stdout_json(obj: Dict[str, Any]) -> None:
    sys.stdout.write(json.dumps(obj, ensure_ascii=False) + "\n")

def _typed_fail(code: str, message: str, span: Dict[str, Any] | None = None, details: Dict[str, Any] | None = None) -> int:
    if span is None:
        span = {}
    err = {"ok": False, "error": {"code": code, "message": message}}
    if details:
        err["error"]["details"] = details
    if span:
        err["span"] = span
    _stdout_json(err)
    return EXITCODES.get(code, 1)

# ---------- core resolve / verify ----------

def _load_module_from_path(path: str):
    p = Path(path).expanduser().resolve()
    spec = importlib.util.spec_from_file_location(p.stem, str(p))
    if not spec or not spec.loader:
        raise ImportError("cannot create spec")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)  # type: ignore[attr-defined]
    return mod

def _resolve_core(core_ref: str | None):
    # precedence: arg → VISM_CORE env → staged core next to installed wrapper → fallback import core_echo
    if core_ref:
        if core_ref.endswith(".py") or Path(core_ref).expanduser().exists():
            return _load_module_from_path(core_ref)
        return importlib.import_module(core_ref)
    env_ref = os.environ.get("VISM_CORE")
    if env_ref:
        if env_ref.endswith(".py") or Path(env_ref).expanduser().exists():
            return _load_module_from_path(env_ref)
        return importlib.import_module(env_ref)
    here = Path(__file__).resolve().parent
    staged = here / "core.py"
    if staged.exists():
        return _load_module_from_path(str(staged))
    try:
        return importlib.import_module("core_echo")
    except Exception as e:
        raise ImportError("spec_absent") from e

def _verify_core(core):
    missing = []
    for name in ("VISM_CODE", "__version__", "DOC", "factory"):
        if not hasattr(core, name):
            missing.append(name)
    if missing:
        raise RuntimeError("spec_absent")
    if not isinstance(core.VISM_CODE, str) or not core.VISM_CODE.strip():
        raise RuntimeError("spec_absent")
    if not isinstance(core.__version__, str) or not core.__version__.strip():
        raise RuntimeError("spec_absent")
    if not isinstance(core.DOC, str) or not core.DOC.strip():
        raise RuntimeError("spec_absent")
    if ("→" not in core.DOC) and ("->" not in core.DOC):
        raise RuntimeError("spec_absent")
    fac = core.factory
    if not callable(fac):
        raise RuntimeError("spec_absent")
    inst = fac()
    if not hasattr(inst, "apply") or not callable(inst.apply):
        raise RuntimeError("spec_absent")
    return inst

# ---------- run modes ----------

def _read_payload(args: argparse.Namespace) -> tuple[Dict[str, Any] | None, str | None, str | None]:
    """Return (payload_dict_or_none, raw_text_or_none, input_hash_or_none)."""
    raw = None
    if args.input:
        try:
            raw = Path(args.input).read_text(encoding="utf-8")
        except FileNotFoundError:
            # if missing file, treat as empty input (consistent with constraint to create files as needed for output only)
            raw = ""
    else:
        # read stdin (do not block if no data? we will read; if empty string, it's empty_input)
        try:
            raw = sys.stdin.read()
        except Exception:
            raw = ""
    if raw is None or raw.strip() == "":
        return None, raw, None
    try:
        payload = json.loads(raw)
    except Exception:
        return None, raw, None
    if not isinstance(payload, dict) or payload == {}:
        # only JSON object with at least one key is accepted
        return None, raw, jhash(payload)
    return payload, raw, jhash(payload)

def _write_output(args: argparse.Namespace, obj: Dict[str, Any]) -> None:
    txt = json.dumps(obj, ensure_ascii=False) + "\n"
    if args.output:
        Path(args.output).parent.mkdir(parents=True, exist_ok=True)
        Path(args.output).write_text(txt, encoding="utf-8")
    else:
        sys.stdout.write(txt)

def run_pipe(core_ref: str | None, args: argparse.Namespace) -> int:
    try:
        core = _resolve_core(core_ref)
    except ImportError:
        return _typed_fail("spec_absent", "core not found")
    try:
        inst = _verify_core(core)
    except RuntimeError:
        return _typed_fail("spec_absent", "core missing required symbols or DOC/arrow")
    started = time.time()
    started_ts = _iso_now()
    payload, raw, inp_hash = _read_payload(args)
    span = {
        "code": getattr(core, "VISM_CODE", None),
        "version": getattr(core, "__version__", None),
        "input_sha256": inp_hash,
        "output_sha256": None,
        "started_ts": started_ts,
        "elapsed_ms": 0,
        "ok": False,
    }
    if raw is None or (raw.strip() == ""):
        span["elapsed_ms"] = int((time.time() - started) * 1000)
        err = {"ok": False, "error": {"code": "empty_input", "message": "stdin empty or --input file missing/empty"}, "span": span}
        _write_output(args, err)
        return EXITCODES["empty_input"]
    # parse error?
    if payload is None:
        span["elapsed_ms"] = int((time.time() - started) * 1000)
        # detect parse error vs empty object
        try:
            test = json.loads(raw)  # may raise
            if not isinstance(test, dict) or test == {}:
                errcode, msg = "empty_input", "payload must be non-empty JSON object"
            else:
                errcode, msg = "parse_error", "invalid json"
        except Exception:
            errcode, msg = "parse_error", "invalid json"
        err = {"ok": False, "error": {"code": errcode, "message": msg}, "span": span}
        _write_output(args, err)
        return EXITCODES[errcode]
    # invoke core twice (determinism)
    try:
        out1 = inst.apply(payload)
        out2 = inst.apply(payload)
    except SystemExit:
        span["elapsed_ms"] = int((time.time() - started) * 1000)
        err = {"ok": False, "error": {"code": "adapter_leak", "message": "core attempted to exit process"}, "span": span}
        _write_output(args, err)
        return EXITCODES["adapter_leak"]
    except Exception as e:
        span["elapsed_ms"] = int((time.time() - started) * 1000)
        err = {"ok": False, "error": {"code": "broken_invariant", "message": f"exception: {type(e).__name__}"}, "span": span}
        _write_output(args, err)
        return EXITCODES["broken_invariant"]
    if out1 != out2:
        span["elapsed_ms"] = int((time.time() - started) * 1000)
        err = {"ok": False, "error": {"code": "broken_invariant", "message": "core non-deterministic"}, "span": span}
        _write_output(args, err)
        return EXITCODES["broken_invariant"]
    out_hash = jhash(out1)
    span.update({"output_sha256": out_hash, "elapsed_ms": int((time.time() - started) * 1000), "ok": True})
    res = {"ok": True, "data": out1, "span": span}
    _write_output(args, res)
    return 0

# ---------- install / uninstall ----------

def _bin_dir() -> Path:
    return Path(os.environ.get("HOME", str(Path("~").expanduser()))) / ".local" / "bin"

def _share_dir() -> Path:
    return Path(os.environ.get("HOME", str(Path("~").expanduser()))) / ".local" / "share" / "vism"

def install(core_ref: str | None) -> int:
    # resolve + verify first
    try:
        core = _resolve_core(core_ref)
        inst = _verify_core(core)
    except Exception:
        return _typed_fail("spec_absent", "core missing or invalid for install")
    code = core.VISM_CODE.strip()
    version = core.__version__.strip()
    # stage core alongside wrapper to ensure stable path
    stage = _share_dir() / code
    stage.mkdir(parents=True, exist_ok=True)
    # write staged core as core.py (importable)
    # if core_ref is a file, copy it; else dump module source if possible
    src_path = None
    if core_ref and (core_ref.endswith(".py") or Path(core_ref).expanduser().exists()):
        src_path = Path(core_ref).expanduser().resolve()
    else:
        try:
            src_path = Path(core.__file__)
        except Exception:
            src_path = None
    dst = stage / "core.py"
    if src_path and src_path.exists():
        shutil.copy2(src_path, dst)
    else:
        # best-effort: serialize minimal stub that imports original module by name
        with open(dst, "w", encoding="utf-8") as f:
            f.write(f'from importlib import import_module\nm=import_module("{core.__name__}")\nVISM_CODE=m.VISM_CODE\n__version__=m.__version__\nDOC=m.DOC\ndef factory():\n    return m.factory()\n')
    # create launcher
    launcher = _bin_dir() / code
    _bin_dir().mkdir(parents=True, exist_ok=True)
    wrapper_path = Path(__file__).resolve()
    content = textwrap.dedent(f"""\
        #!/usr/bin/env bash
        # vism launcher for '{code}' (version {version})
        # Always expose --input/--output; default to JSON stdin/stdout.
        VISM_CORE="{dst}"
        export VISM_CORE
        exec python3 "{wrapper_path}" --core "$VISM_CORE" "$@"
    """)
    launcher.write_text(content, encoding="utf-8")
    os.chmod(launcher, 0o755)
    _stdout_json({"ok": True, "installed": str(launcher)})
    return 0

def uninstall(core_ref: str | None) -> int:
    # resolve core name to know what to remove
    try:
        core = _resolve_core(core_ref)
        _verify_core(core)
        code = core.VISM_CODE.strip()
    except Exception:
        # fall back: try reading from env or guess; still attempt removal of file named core_ref if plausible
        code = None
    removed = []
    # remove launcher(s)
    if code:
        cand = _bin_dir() / code
        if cand.exists():
            cand.unlink()
            removed.append(str(cand))
        # remove staged dir
        sdir = _share_dir() / code
        if sdir.exists():
            shutil.rmtree(sdir, ignore_errors=True)
            removed.append(str(sdir))
    else:
        # brute-force: if user passes a name, try removing that launcher
        for path in _bin_dir().glob("*"):
            if path.is_file() and os.access(path, os.X_OK) and path.name == (core_ref or ""):
                path.unlink()
                removed.append(str(path))
    _stdout_json({"ok": True, "removed": removed})
    return 0

# ---------- main ----------

def main(argv=None) -> int:
    p = argparse.ArgumentParser(prog="vism-wrapper")
    g = p.add_mutually_exclusive_group()
    g.add_argument("--install", action="store_true", help="install launcher for the resolved core")
    g.add_argument("--uninstall", action="store_true", help="uninstall launcher and staged core")
    g.add_argument("--document", action="store_true", help="print DOC from the core (must contain 'input → output')")
    g.add_argument("--help-core", action="store_true", help="print minimal interface information for the core")
    p.add_argument("--core", help="path to core .py file or importable module name")
    # ALWAYS expose input/output per constraint
    p.add_argument("--input", "-i", help="path to JSON input file (otherwise read stdin)")
    p.add_argument("--output", "-o", help="path to write JSON result (otherwise stdout)")
    args = p.parse_args(argv)

    # document/help modes do not require payload
    if args.install:
        return install(args.core)
    if args.uninstall:
        return uninstall(args.core)
    # resolve core for info modes and run mode
    try:
        core = _resolve_core(args.core)
        inst = _verify_core(core)
    except Exception:
        return _typed_fail("spec_absent", "core missing required symbols or not found")

    if args.document:
        _stdout_json({"ok": True, "code": core.VISM_CODE, "version": core.__version__, "doc": core.DOC})
        return 0

    if args.help_core:
        info = {
            "ok": True,
            "code": core.VISM_CODE,
            "version": core.__version__,
            "exports": ["VISM_CODE:str", "__version__:str", "DOC:str", "factory()->instance.apply(dict)->dict"],
            "doc_has_arrow": (("→" in core.DOC) or ("->" in core.DOC)),
        }
        _stdout_json(info)
        return 0

    # Otherwise, run the pipe (json in/out); determinism and telemetry enforced.
    return run_pipe(args.core, args)


if __name__ == "__main__":
    sys.exit(main())
