"""Utilities for converting ChatGPT exports into HTML conversations."""

from __future__ import annotations

"""Export kernel for converting ChatGPT exports to HTML.

This module defines the scaffold for parsing zipped ChatGPT exports into HTML files optimized for rhythm-aware shaping workflows. All functionality here is placeholder-only. Future prompts will gradually replace the print statements with real logic."""

import traceback
import re
from pathlib import Path
import json
import tempfile
import zipfile
from collections import Counter
from glob import glob
from os.path import join

import html
from datetime import datetime
import pytz

class ChatExportArchiver:
    """Orchestrates the conversion of a ChatGPT `.zip` export into HTML files.

    This class defines the top-level control flow for transforming a ChatGPT
    conversation archive into a series of structured HTML documents. It accepts
    paths to the source `.zip` archive and the destination output directory.
    Internally, it unpacks the archive, parses the `conversations.json` file, and
    delegates parsing of each conversation thread to `ThreadParser`. Each resulting
    HTML document is written to the output directory with sequential filenames.

    ## Parameters
    zip_path : Path
        Path to the exported `.zip` archive containing the conversations.
    output_dir : Path
        Path to the directory where HTML files will be saved.

    ## Attributes
    zip_path : Path
        The input path to the archive file.
    output_dir : Path
        The output directory for saving parsed HTML files.

    ## Methods
    run()
        Executes the unpacking, parsing, and HTML export pipeline.
    """
    def __init__(self, zip_path: Path, output_dir: Path) -> None:
        self.zip_path = Path(zip_path)
        self.export_id = self.zip_path.stem
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        print(f"ChatExportArchiver initialized with {self.zip_path} -> {self.output_dir}")

    def run(self) -> None:
        """Extract the archive and convert each conversation to HTML."""
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
                    html_text = ThreadParser(thread, export_id=self.export_id).parse()
                except Exception as exc:
                    print(f"Failed to parse conversation {idx}: {exc}")
                    continue

                dest_name = f"{idx:03d}-conversation.html"
                dest_path = self.output_dir / dest_name
                try:
                    dest_path.write_text(html_text, encoding="utf-8")
                    print(f"Writing to {dest_path}")
                except Exception as exc:
                    print(f"Failed to write {dest_path}: {exc}")


class ThreadParser:
    """Parses individual conversation files into structured HTML with user/agent turns."""

    def __init__(self, raw_thread: dict | str, export_id: str) -> None:
        """Store raw thread data for later parsing."""
        self.raw_thread = raw_thread
        self.export_id = export_id
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

    def _compute_times(self, thread: dict | list, messages: list[dict]) -> tuple[str, str, str]:
        """Return start datetime, end datetime, and date string."""
        ts_list: list[float] = []
        if isinstance(thread, dict):
            for key in ("create_time", "createTime", "timestamp"):
                val = thread.get(key)
                if isinstance(val, (int, float)):
                    ts_list.append(float(val))
        for msg in messages:
            for key in ("create_time", "timestamp"):
                val = msg.get(key)
                if isinstance(val, (int, float)):
                    ts_list.append(float(val))
        if not ts_list:
            from datetime import date
            today = date.today().isoformat()
            return "", "", today
        start_ts = min(ts_list)
        end_ts = max(ts_list)
        denver = pytz.timezone("America/Denver")
        start_dt = datetime.fromtimestamp(start_ts, tz=pytz.utc).astimezone(denver)
        end_dt = datetime.fromtimestamp(end_ts, tz=pytz.utc).astimezone(denver)
        return start_dt.isoformat(), end_dt.isoformat(), start_dt.date().isoformat()

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
        """Convert raw thread into HTML text."""
        print("Parsing thread...")
        thread_obj = self._load_thread()
        messages = self._normalize_messages(thread_obj)

        start, end, date_str = self._compute_times(thread_obj, messages)
        arc = thread_obj.get("arc", "") if isinstance(thread_obj, dict) else ""

        lines: list[str] = []
        lines.append('<div class="meta">')
        lines.append(f'<div class="export-id">{html.escape(self.export_id)}</div>')
        if start:
            lines.append(f'<div class="start">{start}</div>')
        if end:
            lines.append(f'<div class="end">{end}</div>')
        lines.append(f'<div class="date">{date_str}</div>')
        lines.append('<div class="participants"><span>zero</span><span>tide</span></div>')
        if arc:
            lines.append(f'<div class="arc">{html.escape(str(arc))}</div>')
        lines.append('</div>')

        for msg in messages:
            role = msg.get("author")
            content = html.escape(msg.get("content", "")).replace("\n", "<br>\n")
            if role == "user":
                lines.append(f'<div class="user-turn"><h2>zero:</h2>{content}</div>')
            elif role == "assistant":
                lines.append(f'<div class="assistant-turn"><h2>tide:</h2>{content}</div>')
            else:
                print(f"Skipping unknown author: {role}")

        html_text = "\n".join(lines).rstrip() + "\n"
        return html_text


class KernelIndexPage:
    """Generate a root-level index.html listing all exported scrolls.

    This method scans a directory of HTML scrolls generated from ChatGPT exports
    and writes an index file named `index.html` into that directory. The index
    provides a chronological, grouped-by-date overview of the archive’s scrolls,
    with each entry linking to the scroll file, displaying its filename, the
    extracted date string (e.g. "2025-07-01"), and a preview snippet drawn from
    the first user prompt in the file. Filenames are assumed to follow the
    pattern `NNN-conversation.html`.

    Requirements:
    - Use only standard Python libraries (e.g., os, re, html, datetime).
    - Sort scrolls chronologically based on filename prefix (e.g. "003").
    - Extract the date string and first user prompt by reading from each file.
    - Group scroll links by date (if possible), and maintain clean visual rhythm.
    - Output must be valid HTML viewable in any basic browser or Markdown preview.
    - Avoid JavaScript or external CSS. Minimal inline CSS is permitted.

    The goal is to create a stable, walkable entrypoint into the archive that 
    immediately orients the user. The visual layout should echo clarity: a quiet 
    trailhead where each scroll announces its tone with a glance. Return nothing;
    simply write the index file to disk.
    """


class ScrollTableOfContents:
    """Inject a table of contents into each scroll HTML file.

    Write a method that reads a parsed conversation scroll (as a list of 
    user/assistant message dicts with content and author keys), and outputs 
    a single HTML string representing a scroll-specific table of contents. 
    Each TOC entry should be a jump link (<a href="#...">) pointing to an 
    anchor above the corresponding turn in the scroll, using sequential IDs 
    like `turn-001`, `turn-002`, etc. The label for each TOC entry should 
    be the first 8–12 words of the user/assistant message (after stripping 
    newlines and trimming whitespace), prefixed with either “zero:” or “tide:”.

    Requirements:
    - TOC should be a single styled <div> element suitable for injecting at 
      the top of the scroll HTML.
    - If there are more than 20 turns, break the TOC into sections of 20, 
      each with a subheading (e.g., “Turns 1–20”, “Turns 21–40”).
    - Do not use JavaScript or external CSS. Use inline styles if needed.
    - Assume each turn has already been assigned a sequential ID.
    - The method should return the generated TOC as a string, not write to disk.
    - Use only standard Python libraries such as `html` and `textwrap`.

    The goal is to let users quickly navigate long scrolls by skimming 
    meaningful fragments of each turn. It should feel lightweight, embedded, 
    and skimmable — a rhythm map, not a wall of text.
    """


class TurnSummaryAnnotator:
    """Generate end-of-turn summaries with keyphrase and token count annotations.

    Write a method that processes a list of messages (each a dict with 'author'
    and 'content' keys) and returns an updated list of HTML turn blocks, where
    each user/assistant message is followed by a lightweight annotation block.
    This block includes two things: (1) the number of characters in the message,
    and (2) a five-word summary based on most frequent, meaningful tokens.

    For each message:
    - Tokenize the content, strip punctuation, lowercase, and remove common 
      stopwords (using `nltk` or `spacy`, no model training required).
    - Count token frequencies and select the top 5 most common as a summary.
    - Include a small styled block at the end of the turn that reads:
      “🔍 Summary: [token1 token2 token3 token4 token5] — [123 chars]”
    - Ensure all HTML remains well-formed and valid.

    Requirements:
    - Works only on `user` and `assistant` messages; others are skipped.
    - Tokenization should be fast and robust; fallback if external libraries fail.
    - Returns HTML strings with summary blocks embedded, but does not write to disk.
    - No JavaScript or external styling; inline styling allowed but minimal.
    - Make sure special characters in tokens or content are safely escaped.

    This summary annotation is meant to act as a foothold for the reader. 
    It helps the user reenter the flow of a scroll without rereading — each 
    turn speaks its own orientation. When skimming a long thread, the user 
    can catch the rhythm, content, and structure at a glance.
    """

