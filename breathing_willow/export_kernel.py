
"""
BEGINPROMPT
 |🌊 [flow agent] @ ctime 2025-07-03T12:25:51-0600 | 🌊📚 tide:: INSTRUCTIONS
 FOR CODEX historical-money f88acfa7. 

 follow the below prompt. remove text between BEGINPROMPT and ENDPROMPT

Rewrite the following two classes — `ThreadParser` and `ChatExportArchiver` — to output HTML
instead of Markdown. The goal is to convert a ChatGPT `.zip` export into scroll-friendly,
structure-preserving HTML files.

You must:
- Replace all Markdown formatting logic with equivalent HTML structure.
  - User turns should be wrapped in `<div class="user-turn">` with a `<h2>zero:</h2>` header.
  - Assistant turns should be in `<div class="assistant-turn">` with a `<h2>tide:</h2>` header.
  - Line breaks and paragraph spacing should be preserved.
- At the top of each HTML file, prepend a `<div class="meta">` block with:
  - The export identifier (derived from the `.zip` filename, e.g., its base name without extension)
  - The full conversation start and end datetimes in local Denver time (tz-aware)
  - The conversation date in `YYYY-MM-DD` format
  - Participants (always zero and tide)
  - If the `arc` field exists, include it as a `<div class="arc">` element

Implementation notes:
- In `ChatExportArchiver`, pass the export basename as a new field `export_id`, extracted from
  the zip file name without extension. This value must be passed to `ThreadParser`.
- In `ThreadParser`, detect the earliest and latest timestamps from the messages list to compute
  start and end times. Use the `pytz` library to convert UTC timestamps to Denver local time.
- The final HTML output should be returned as a single string and written to `.html` files.
- Output files must use `.html` extension, and filenames should still follow the zero-padded index
  pattern (`001-conversation.html`, etc.).
ENDPROMPT
"""


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
import traceback
import re
from collections import Counter
from glob import glob
from os.path import join


class ChatExportArchiver:
    """Orchestrates the conversion of a ChatGPT `.zip` export into markdown files.

    This class defines the top-level control flow for transforming a ChatGPT
    conversation archive into a series of structured markdown documents. It accepts
    paths to the source `.zip` archive and the destination output directory.
    Internally, it unpacks the archive, parses the `conversations.json` file, and
    delegates parsing of each conversation thread to `ThreadParser`. Each resulting
    markdown document is written to the output directory with sequential filenames.

    ## Parameters
    zip_path : Path
        Path to the exported `.zip` archive containing the conversations.
    output_dir : Path
        Path to the directory where markdown files will be saved.

    ## Attributes
    zip_path : Path
        The input path to the archive file.
    output_dir : Path
        The output directory for saving parsed markdown files.

    ## Methods
    run()
        Executes the unpacking, parsing, and markdown export pipeline.
    """
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

            conv_dir = Path(tmpdir)
            convo_file = conv_dir / "conversations.json"
            if not convo_file.is_file():
                raise Exception("conversations.json not found")

            try:
                with convo_file.open("r", encoding="utf-8") as fh:
                    conversations = json.load(fh)
            except Exception as exc:
                print(f"Failed to load {convo_file}: {exc}")
                return

            if not isinstance(conversations, list):
                print(f"{convo_file} did not contain a list")
                return

            print(f"Found {len(conversations)} conversations...")

            for idx, thread in enumerate(conversations, 1):
                print(f"Parsing conversation {idx}...")
                try:
                    md_text = ThreadParser(thread).parse()
                except Exception as exc:
                    print(f"Failed to parse conversation {idx}: {exc}")
                    continue

                dest_name = f"{idx:03d}-conversations.md"
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
        """Return ordered list of messages by walking the conversation tree."""
        if isinstance(thread, list):
            messages = thread
        elif isinstance(thread, dict):
            if "mapping" in thread:
                mapping = thread["mapping"]
                root_id = "client-created-root"
                ordered: list[dict] = []

                def walk(node_id: str) -> None:
                    node = mapping.get(node_id)
                    if not node:
                        return
                    msg = node.get("message")
                    if msg and isinstance(msg, dict):
                        author = msg.get("author")
                        if isinstance(author, dict):
                            author = author.get("role")
                        content = msg.get("content")
                        if isinstance(content, dict):
                            parts = content.get("parts", [])
                            if isinstance(parts, list) and parts:
                                content_text = "\n".join(str(p).strip() for p in parts if p)
                            else:
                                content_text = content.get("text", "").strip()
                        else:
                            content_text = content or ""
                        if content_text.strip():
                            ordered.append({"author": author, "content": content_text.strip()})
                    for child_id in node.get("children", []):
                        walk(child_id)

                walk(root_id)
                return ordered
            elif "messages" in thread and isinstance(thread["messages"], list):
                messages = thread["messages"]
            else:
                print("Unsupported or missing mapping structure.")
                return []
        else:
            print("Unsupported or missing mapping structure.")
            return []

        ordered = []
        for msg in messages:
            if not isinstance(msg, dict):
                continue
            author = msg.get("author")
            if isinstance(author, dict):
                author = author.get("role")
            content = msg.get("content")
            if isinstance(content, dict):
                parts = content.get("parts", [])
                if isinstance(parts, list) and parts:
                    content_text = "\n".join(str(p).strip() for p in parts if p)
                else:
                    content_text = content.get("text", "").strip()
            else:
                content_text = content or ""
            if content_text.strip():
                ordered.append({"author": author, "content": content_text.strip()})
        return ordered

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


if __name__ == "__main__":
    main()

