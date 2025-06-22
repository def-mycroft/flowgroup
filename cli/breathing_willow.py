import argparse
from pathlib import Path
from datetime import datetime, timezone


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

    args = parser.parse_args(argv)
    version = get_version()

    if args.version or args.command is None:
        print(f"Breathing Willow version {version} - CLI is alive!")
        return

    if args.command == "log-prompt":
        log_prompt(args.title, args.link, args.commit)


if __name__ == "__main__":
    main()
