import argparse
from pathlib import Path
from w_cli import diff
from datetime import datetime, timezone
import os
import re
from zoneinfo import ZoneInfo
import subprocess
from typing import Sequence
import uuid

UUID_RE = re.compile(
    r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
)

def _extract_uuids(text: str) -> list[str]:
    return UUID_RE.findall(text)

def _tag_cloud(text: str, max_len: int = 500) -> str:
    tokens = re.findall(r"\b\w+\b", text.lower())
    freq = {}
    for t in tokens:
        freq[t] = freq.get(t, 0) + 1
    words = []
    for word, count in sorted(freq.items(), key=lambda x: (-x[1], x[0])):
        words.extend([word] * count)
    cloud = " ".join(words)
    return cloud[:max_len]

def _tokens(text: str) -> list[str]:
    return re.findall(r"\b\w+\b", text.lower())

def _all_tokens_in_log(log_text: str) -> set[str]:
    tokens: set[str] = set()
    for match in re.findall(r"\*\*Tag Cloud:\*\*\s*\n([\s\S]+?)\n---", log_text):
        tokens.update(_tokens(match))
    return tokens

def _last_points(log_text: str) -> int:
    m = re.search(r"## Shaping Log \u2014 .*? \u2014 (\d+) pts", log_text)
    return int(m.group(1)) if m else 0

def append_shaping_log(file_path: Path, clusters: list[list[str]] | None = None) -> None:
    log_file = Path(os.environ.get("WILLOW_SHAPING_LOG", "/l/obs-chaotic/willow-shaping.md"))
    log_file.parent.mkdir(parents=True, exist_ok=True)
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    text = file_path.read_text()
    prev_text = log_file.read_text() if log_file.exists() else ""

    previous_tokens = _all_tokens_in_log(prev_text)
    current_tokens = set(_tokens(text))
    new_terms = len(current_tokens - previous_tokens)
    volume = len(current_tokens)
    run_points = volume + new_terms if new_terms else 0
    total_points = _last_points(prev_text) + run_points

    uuids = set(_extract_uuids(file_path.name) + _extract_uuids(text))
    uuid_str = ", ".join(sorted(uuids)) if uuids else ""

    entry = (
        f"## Shaping Log — {now} — {total_points} pts\n\n"
        f"**File:** {file_path}  \n"
        f"**Filename:** {file_path.name}  \n"
        f"**UUIDs:** {uuid_str}  \n"
        f"**Points:** {total_points}  \n"
    )

    if clusters:
        cluster_lines = [f"- Cluster {i+1}: {' '.join(c)}  " for i, c in enumerate(clusters)]
        entry += "**Top Concepts:**  \n" + "\n".join(cluster_lines) + "\n"

    entry += (
        f"**Tag Cloud:**  \n"
        f"{_tag_cloud(text)}\n\n---\n"
    )

    log_file.write_text(entry + prev_text)

def save_snapshot(src: Path, snapshot_dir: Path | None = None) -> Path:
    """Save a snapshot copy of ``src`` with UUID and timestamp header."""
    dest_dir = snapshot_dir or src.parent
    dest_dir.mkdir(parents=True, exist_ok=True)
    uid = uuid.uuid4()
    dt = datetime.now(ZoneInfo("America/Denver"))
    header = (
        f"{uid}  \n"
        f"{dt.strftime('%Y-%m-%d %H:%M:%S %z America/Denver')}  \n"
        "\n***\n"
    )
    dest = dest_dir / f"{src.stem}-{uid}.md"
    dest.write_text(header + src.read_text())
    return dest

from breathing_willow.willow_viz import WillowGrowth

def log_prompt(title: str, task_link: str, commit_link: str | None = None) -> None:
    """Append a prompt entry to the meta/prompt-log.md file."""
    log_path = Path(__file__).resolve().parent.parent / "meta" / "prompt-log.md"
    if not log_path.exists():
        log_path.parent.mkdir(parents=True, exist_ok=True)
        log_path.write_text("| Timestamp | Title | Task Link | Commit/PR |\n|-----------|-------|-----------|-----------|\n")

    timestamp = datetime.now(timezone.utc).isoformat(timespec="seconds")
    commit_cell = commit_link if commit_link else ""
    row = f"| {timestamp} | {title} | {task_link} | {commit_cell} |\n"
    with log_path.open("a") as fh:
        fh.write(row)

def mark_vc_step(note: str) -> None:
    """Append a vc loop step entry to meta/vc-loop.md."""
    log_path = Path(__file__).resolve().parent.parent / "meta" / "vc-loop.md"
    if not log_path.exists():
        log_path.parent.mkdir(parents=True, exist_ok=True)
        log_path.write_text("| Timestamp | Note |\n|-----------|------|\n")

    timestamp = datetime.now(timezone.utc).isoformat(timespec="seconds")
    row = f"| {timestamp} | {note} |\n"
    with log_path.open("a") as fh:
        fh.write(row)

def get_version():
    version_file = Path(__file__).resolve().parent.parent / "VERSION.md"
    if version_file.exists():
        for line in version_file.read_text().splitlines()[::-1]:
            line = line.strip()
            if line.startswith("vc"):
                return line.split()[0]
    return "0.0.0"

def main(argv=None):
    """entry point for bw cli 

    NOTE: this is the main cli entry point for the cli. 

    """
    parser = argparse.ArgumentParser(
        prog="breathing-willow",
        description="Breathing Willow CLI"
    )
    parser.add_argument(
        "--version",
        action="store_true",
        help="show version and exit"
    )
    subparsers = parser.add_subparsers(dest="command")

    ccraft_parser = subparsers.add_parser("ccraft", help="tools for context crafting ccraft. ")
    ccraft_parser.add_argument(
        "-f",
        "--file",
        required=True,
        help="input file",
    )

    sense_parser = subparsers.add_parser("sense", help="sense for pulse (diff)")
    sense_parser.add_argument(
        "--diff",
        action="store_true",
        help="export conceptual diff report",
    )

    dev_man_parser = subparsers.add_parser("module-prompt133", help="setup prompt stubs for 133 process")
    dev_man_parser.add_argument("--filepath-module", "-f", required=False, help="path to module to be developed.", default="")
    dev_man_parser.add_argument("--dir-out", "-o", required=False, help="output dir path. will write codex/prompts. ", default="")

    log_parser = subparsers.add_parser("log-prompt", help="log a codex prompt")
    log_parser.add_argument("--title", required=True, help="prompt title")
    log_parser.add_argument("--link", required=True, help="codex task link")
    log_parser.add_argument("--commit", help="commit or PR link")

    hist_parser = subparsers.add_parser(
        "history", help="parse chatgpt history"
    )
    hist_parser.add_argument(
        "-f",
        "--file",
        required=True,
        help="input file",
    )
    step_parser = subparsers.add_parser(
        "vc-step", help="record a quick vc loop step"
    )
    step_parser.add_argument("note", help="short note for the step")

    docs_parser = subparsers.add_parser(
        "docs", help="build and preview the documentation"
    )
    docs_parser.add_argument(
        "--host", default="127.0.0.1", help="host to bind (default: 127.0.0.1)"
    )
    docs_parser.add_argument(
        "--port", default="8000", help="port to serve on (default: 8000)"
    )

    update_parser = subparsers.add_parser(
        "update-net", help="add a document to the graph and render"
    )
    update_parser.add_argument(
        "-f",
        "--file",
        required=True,
        help="path to the document to ingest",
    )
    update_parser.add_argument(
        "--visual-archive",
        required=True,
        help="path to write the visualization HTML",
    )
    update_parser.add_argument(
        "--graph",
        default="willow_growth_v5.json",
        help="path to persistent graph (default: willow_growth_v5.json)",
    )
    update_parser.add_argument(
        "--snapshot-dir",
        help="directory to save version snapshots (default: alongside file)",
    )

    snip_parser = subparsers.add_parser(
        "snip-file", help="truncate file to last practical tokens"
    )
    snip_parser.add_argument(
        "-f",
        "--file",
        default='/field/prompt.md',
        required=True,
        help="path to any text file to snip to last n tokens. ",
    )

    args = parser.parse_args(argv)
    version = get_version()

    if args.version or args.command is None:
        print(f"Breathing Willow version {version} - CLI is alive!")
        return

    if args.command == "history":
        from breathing_willow.export_kernel import ChatExportArchiver
        from datetime import datetime
        from os.path import join
        fp = '/l/gds/chatgpt-exports'
        print(f"will write to '{fp}'")
        now = datetime.now()
        fpo = Path(join(fp, now.strftime('%Y-%m-%d')))
        fpi = Path(args.file)
        archiver = ChatExportArchiver(fpi, fpo)
        archiver.run()
        print(f"\nwould have written to '{fp}' ")

    if args.command == "ccraft":
        from breathing_willow import context_slicer_openoption as cs
        fp = args.file
        print(f"for input file '{fp}'")
        fpo = cs.endpoint(fp)
        print('done. 是道')
    elif args.command == "sense":
        if args.diff:
            report = diff.export_diff("/field")
            Path("/field/field-update.md").write_text(report)
        else:
            print("sense")
    elif args.command == "module-prompt133":
        from breathing_willow import module_prompt_setup as mps
        fp_module = args.filepath_module
        fp_output = args.dir_out
        if not fp_module and fp_module:
            print('must have both -f and -o. ')
        print(fp_module, fp_output)
        mps.generate_codex_prompts(fp_module, fp_output)
    elif args.command == "log-prompt":
        log_prompt(args.title, args.link, args.commit)
    elif args.command == "vc-step":
        mark_vc_step(args.note)
    elif args.command == "docs":
        host = args.host
        port = args.port
        url = f"http://{host}:{port}"
        print(f"Serving docs at {url} (live reload). Press Ctrl+C to stop.")
        cmd = ["mkdocs", "serve", "--dev-addr", f"{host}:{port}"]
        subprocess.run(cmd, check=True)
    elif args.command == "update-net":
        src = Path(args.file)
        snap_dir = Path(args.snapshot_dir) if args.snapshot_dir else None
        save_snapshot(src, snap_dir)
        wg = WillowGrowth(graph_path=args.graph)
        wg.submit_document(args.file)
        wg.visualize(args.visual_archive)
        clusters = wg.cluster_terms()
        append_shaping_log(src, clusters)
    elif args.command == "snip-file":
        from breathing_willow import snip_file as sf
        import tiktoken

        fp = Path(args.file)
        if not fp.exists():
            raise SystemExit(f"file not found: {fp}")

        enc = tiktoken.encoding_for_model("gpt-4")
        before_text = fp.read_text(encoding="utf-8")
        before_tokens = len(enc.encode(before_text))
        print(f"file '{fp}' has {before_tokens} tokens before snipping.")

        print("snipping file to last practical context...")
        sf.snip_file_to_last_tokens(str(fp), context_scope="practical", aggressive=True)

        after_text = fp.read_text(encoding="utf-8")
        after_tokens = len(enc.encode(after_text))
        print(f"file '{fp}' now has {after_tokens} tokens after snipping.")

if __name__ == "__main__":
    main()



