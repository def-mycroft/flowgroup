import os
import re
import uuid
from datetime import datetime, timezone
from pathlib import Path
from zoneinfo import ZoneInfo

UUID_RE = re.compile(
    r"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
)


def extract_uuids(text: str) -> list[str]:
    """Return list of UUID strings found in ``text``."""
    return UUID_RE.findall(text)


def _tokens(text: str) -> list[str]:
    return re.findall(r"\b\w+\b", text.lower())


def tag_cloud(text: str, max_len: int = 500) -> str:
    """Return a simple frequency-based tag cloud truncated to ``max_len``."""
    tokens = _tokens(text)
    freq: dict[str, int] = {}
    for t in tokens:
        freq[t] = freq.get(t, 0) + 1
    words: list[str] = []
    for word, count in sorted(freq.items(), key=lambda x: (-x[1], x[0])):
        words.extend([word] * count)
    cloud = " ".join(words)
    return cloud[:max_len]


def all_tokens_in_log(log_text: str) -> set[str]:
    tokens: set[str] = set()
    for match in re.findall(r"\*\*Tag Cloud:\*\*\s*\n([\s\S]+?)\n---", log_text):
        tokens.update(_tokens(match))
    return tokens


def last_points(log_text: str) -> int:
    m = re.search(r"## Shaping Log \u2014 .*? \u2014 (\d+) pts", log_text)
    return int(m.group(1)) if m else 0


def append_shaping_log(file_path: Path, clusters: list[list[str]] | None = None) -> None:
    """Append an entry about ``file_path`` to the shaping log."""
    log_file = Path(os.environ.get("WILLOW_SHAPING_LOG", "/l/obs-chaotic/willow-shaping.md"))
    log_file.parent.mkdir(parents=True, exist_ok=True)
    now = datetime.now(timezone.utc).isoformat(timespec="seconds")
    text = file_path.read_text()
    prev_text = log_file.read_text() if log_file.exists() else ""

    previous_tokens = all_tokens_in_log(prev_text)
    current_tokens = set(_tokens(text))
    new_terms = len(current_tokens - previous_tokens)
    volume = len(current_tokens)
    run_points = volume + new_terms if new_terms else 0
    total_points = last_points(prev_text) + run_points

    uuids = set(extract_uuids(file_path.name) + extract_uuids(text))
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
        f"{tag_cloud(text)}\n\n---\n"
    )

    log_file.write_text(entry + prev_text)


def save_snapshot(src: Path, snapshot_dir: Path | None = None) -> Path:
    """Save a snapshot copy of ``src`` with UUID header."""
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


def get_version() -> str:
    """Return the current project version."""
    version_file = Path(__file__).resolve().parent.parent / "VERSION.md"
    if version_file.exists():
        for line in version_file.read_text().splitlines()[::-1]:
            line = line.strip()
            if line.startswith("vc"):
                return line.split()[0]
    return "0.0.0"

__all__ = [
    "extract_uuids",
    "tag_cloud",
    "all_tokens_in_log",
    "last_points",
    "append_shaping_log",
    "save_snapshot",
    "log_prompt",
    "mark_vc_step",
    "get_version",
]

