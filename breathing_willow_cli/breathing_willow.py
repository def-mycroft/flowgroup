import argparse
from pathlib import Path
from datetime import datetime, timezone
import subprocess
from typing import Sequence

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

    log_parser = subparsers.add_parser("log-prompt", help="log a codex prompt")
    log_parser.add_argument("--title", required=True, help="prompt title")
    log_parser.add_argument("--link", required=True, help="codex task link")
    log_parser.add_argument("--commit", help="commit or PR link")

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

    args = parser.parse_args(argv)
    version = get_version()

    if args.version or args.command is None:
        print(f"Breathing Willow version {version} - CLI is alive!")
        return

    if args.command == "log-prompt":
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
        wg = WillowGrowth(graph_path=args.graph)
        wg.submit_document(args.file)
        wg.visualize(args.visual_archive)


if __name__ == "__main__":
    main()

