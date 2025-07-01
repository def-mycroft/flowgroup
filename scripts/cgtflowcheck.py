import argparse
import re
from datetime import datetime
from pathlib import Path


def parse_args():
    parser = argparse.ArgumentParser(
        description="Inspect markdown export structure for flow correctness"
    )
    parser.add_argument(
        "--date",
        help="Filter inspection to a specific YYYY-MM-DD export directory",
    )
    return parser.parse_args()


def gather_directories(base: Path, date_filter: str):
    if date_filter:
        try:
            datetime.strptime(date_filter, "%Y-%m-%d")
        except ValueError:
            raise SystemExit(f"Invalid date format: {date_filter}")
        dir_path = base / date_filter
        if dir_path.is_dir():
            return [dir_path]
        else:
            raise SystemExit(f"No export directory found for date: {date_filter}")
    if not base.is_dir():
        raise SystemExit(f"Export directory not found: {base}")
    dirs = [d for d in base.iterdir() if d.is_dir()]
    return dirs if dirs else [base]


def inspect_file(path: Path):
    text = path.read_text(encoding="utf-8", errors="ignore")
    fold_start = len(re.findall(r"<!--\s*fold:start\s*-->", text))
    fold_end = len(re.findall(r"<!--\s*fold:end\s*-->", text))
    memory_lines = re.findall(r"^\s*🔁\s*memory:\s*(.*)$", text, re.MULTILINE)
    token_count = len(re.findall(r"\w+", text))
    zero_count = len(re.findall(r"^##\s*zero:", text, re.MULTILINE))
    tide_count = len(re.findall(r"^##\s*tide:", text, re.MULTILINE))
    return {
        "fold_start": fold_start,
        "fold_end": fold_end,
        "memory": [m.strip()[:40] for m in memory_lines],
        "token_count": token_count,
        "zero": zero_count,
        "tide": tide_count,
    }


def format_summary(path: Path, info: dict) -> str:
    lines = [f"📄 {path.name}"]
    if info["fold_start"] == info["fold_end"]:
        lines.append(f"    ✅ folds: {info['fold_start']} matched")
    else:
        lines.append(
            f"    ⚠️ folds: start={info['fold_start']} end={info['fold_end']}"
        )
    if info["memory"]:
        for m in info["memory"]:
            lines.append(f"    🔁 memory: {m}")
    else:
        lines.append("    ⚠️ no memory cues")
    lines.append(f"    🧠 tokens: {info['token_count']}")
    lines.append(f"    🗣️ turns: zero={info['zero']}, tide={info['tide']}")
    if info["token_count"] > 10000:
        lines.append("    ⚠️ token count > 10k")
    return "\n".join(lines)


def main():
    args = parse_args()
    base = Path("/field/chatgpt-export")
    directories = gather_directories(base, args.date)
    files = []
    for d in directories:
        files.extend(sorted(d.rglob("*.md")))
    if not files:
        print("No markdown files found")
        return
    for path in files:
        try:
            info = inspect_file(path)
        except Exception as e:
            print(f"Error processing {path}: {e}")
            continue
        print(format_summary(path, info))
        print()


if __name__ == "__main__":
    main()
