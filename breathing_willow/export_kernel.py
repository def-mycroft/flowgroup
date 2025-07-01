from __future__ import annotations

"""Export kernel for converting ChatGPT exports to Markdown.

This module defines the scaffold for parsing zipped ChatGPT exports into
Markdown files optimized for rhythm-aware shaping workflows. All
functionality here is placeholder-only. Future prompts will gradually
replace the print statements with real logic.
"""

from pathlib import Path
import json
import tempfile
import zipfile


class ChatExportArchiver:
    """Handles overall process: unpacking .zip, iterating files, coordinating pipeline."""

    def __init__(self, zip_path: Path, output_dir: Path) -> None:
        self.zip_path = Path(zip_path)
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        print(f"ChatExportArchiver initialized with {self.zip_path} -> {self.output_dir}")

    def run(self) -> None:
        """Extract the archive and convert each conversation to markdown."""
        print("Extracting archive...")
        with tempfile.TemporaryDirectory() as tmpdir:
            try:
                with zipfile.ZipFile(self.zip_path) as zf:
                    zf.extractall(tmpdir)
            except Exception as exc:
                print(f"Failed to extract {self.zip_path}: {exc}")
                return

            conv_dir = Path(tmpdir) / "conversations"
            json_files = sorted(conv_dir.glob("*.json"))
            print(f"Found {len(json_files)} conversations...")

            for idx, json_file in enumerate(json_files, 1):
                print(f"Parsing {json_file.name}...")
                try:
                    with json_file.open("r", encoding="utf-8") as fh:
                        raw_thread = json.load(fh)
                except Exception as exc:
                    print(f"Skipping {json_file.name}: {exc}")
                    continue

                try:
                    md_text = ThreadParser(raw_thread).parse()
                except Exception as exc:
                    print(f"Failed to parse {json_file.name}: {exc}")
                    continue

                dest_name = f"{idx:03d}-{json_file.stem}.md"
                dest_path = self.output_dir / dest_name
                try:
                    dest_path.write_text(md_text, encoding="utf-8")
                    print(f"Writing to {dest_path}")
                except Exception as exc:
                    print(f"Failed to write {dest_path}: {exc}")


class ThreadParser:
    """Parses individual conversation files into structured Markdown with user/agent turns."""

    def __init__(self, raw_thread: dict | str) -> None:
        """Store raw thread data for later parsing."""
        self.raw_thread = raw_thread
        print("ThreadParser initialized")

    def _load_thread(self) -> dict | list:
        """Return thread as Python object, loading JSON if needed."""
        if isinstance(self.raw_thread, (dict, list)):
            return self.raw_thread
        try:
            return json.loads(self.raw_thread)
        except Exception as exc:  # pragma: no cover - safety net
            print(f"Failed to load thread JSON: {exc}")
            return {}

    def _extract_date(self, thread: dict | list, messages: list[dict]) -> str:
        """Extract conversation date from thread or fallback to today."""
        ts = None
        if isinstance(thread, dict):
            for key in ("create_time", "createTime", "timestamp", "date"):
                if key in thread:
                    ts = thread.get(key)
                    break
        if ts is None:
            for msg in messages:
                ts = msg.get("create_time") or msg.get("timestamp")
                if ts:
                    break
        if isinstance(ts, (int, float)):
            try:
                from datetime import datetime

                return datetime.fromtimestamp(ts).date().isoformat()
            except Exception:
                pass
        from datetime import date

        return date.today().isoformat()

    def _normalize_messages(self, thread: dict | list) -> list[dict]:
        """Return ordered list of simple messages with author/content."""
        if isinstance(thread, list):
            msgs = thread
        elif isinstance(thread, dict):
            if "messages" in thread and isinstance(thread["messages"], list):
                msgs = thread["messages"]
            elif "mapping" in thread and isinstance(thread["mapping"], dict):
                temp: list[tuple[float | int | None, dict]] = []
                for node in thread["mapping"].values():
                    m = node.get("message")
                    if not m:
                        continue
                    ts = m.get("create_time") or m.get("timestamp")
                    temp.append((ts, m))
                temp.sort(key=lambda x: (x[0] if x[0] is not None else 0))
                msgs = [m for _, m in temp]
            else:
                print("Unknown thread structure; no messages found")
                msgs = []
        else:
            print("Unsupported thread type; expected dict or list")
            msgs = []

        normalized: list[dict] = []
        for m in msgs:
            if not isinstance(m, dict):
                print("Skipping malformed message entry")
                continue
            author = m.get("author")
            if isinstance(author, dict):
                author = author.get("role")
            content = m.get("content")
            if isinstance(content, dict):
                if "parts" in content and isinstance(content["parts"], list):
                    content_text = "\n".join(p for p in content["parts"] if p)
                else:
                    content_text = content.get("text", "")
            else:
                content_text = content or ""
            if not content_text.strip():
                print("Skipping empty message")
                continue
            normalized.append({"author": author, "content": content_text.strip(), **m})
        return normalized

    def parse(self) -> str:
        """Convert raw thread into markdown text."""
        print("Parsing thread...")
        thread_obj = self._load_thread()
        messages = self._normalize_messages(thread_obj)

        lines: list[str] = []
        for msg in messages:
            role = msg.get("author")
            if role == "user":
                lines.append("## zero:")
            elif role == "assistant":
                lines.append("## tide:")
            else:
                print(f"Skipping unknown author: {role}")
                continue
            lines.append(msg.get("content", ""))
            lines.append("")

        date_str = self._extract_date(thread_obj, messages)
        arc = thread_obj.get("arc", "") if isinstance(thread_obj, dict) else ""

        header = [
            "---",
            f"date: {date_str}",
            "participants:",
            "  - zero",
            "  - tide",
            f"arc: {arc}",
            "---",
            "",
        ]

        markdown = "\n".join(header + lines).rstrip() + "\n"
        return markdown


class MemoryEchoInserter:
    """Inserts semantic breaks + memory echoes into raw markdown every ~3k tokens."""

    def __init__(self, md_text: str, token_limit: int = 3000) -> None:
        self.md_text = md_text
        self.token_limit = token_limit
        print(f"MemoryEchoInserter initialized with token_limit={token_limit}")

    def insert_memory_echoes(self) -> str:
        """Return markdown text with placeholder memory echoes."""
        print("Inserting memory echoes...")
        return self.md_text


class MarkdownExporter:
    """Handles writing the final .md output to disk, ensuring scroll-readability and UTF-8."""

    def __init__(self, parsed_text: str, output_path: Path) -> None:
        self.parsed_text = parsed_text
        self.output_path = output_path
        print(f"MarkdownExporter initialized for {output_path}")

    def write(self) -> None:
        """Simulate writing markdown to disk."""
        print(f"Writing markdown to {self.output_path}")


def main() -> None:
    """Entry point for running the export kernel."""
    zip_path = Path("chatgpt_export.zip")
    output_dir = Path("field/chatgpt-export/2025-07-01")
    archiver = ChatExportArchiver(zip_path, output_dir)
    archiver.run()


if __name__ == "__main__":
    main()
