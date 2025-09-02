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

        # after all scrolls are written, generate the root index
        # this index is the archive trailhead for browsing
        KernelIndexPage().run(self.output_dir)


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

    def run(self, directory: str | Path) -> None:
        """Scan ``directory`` and write an ``index.html`` summarizing its scrolls."""
        from os import listdir
        from pathlib import Path
        import html
        import re

        dir_path = Path(directory)
        files = []
        for name in listdir(dir_path):
            m = re.match(r"(\d+)-conversation\.html$", name)
            if not m:
                continue
            try:
                num = int(m.group(1))
            except ValueError:
                num = 0
            files.append((num, dir_path / name))
        files.sort(key=lambda x: x[0])

        entries: list[tuple[str, str, str]] = []
        for _, fpath in files:
            try:
                text = fpath.read_text(encoding="utf-8")
            except Exception:
                continue
            date_match = re.search(r'<div class="date">([^<]+)</div>', text)
            date_str = date_match.group(1) if date_match else ""
            user_match = re.search(
                r'<div class="user-turn">.*?<h2>zero:</h2>(.*?)</div>',
                text,
                re.DOTALL,
            )
            snippet_raw = user_match.group(1) if user_match else ""
            snippet_text = re.sub(r"<[^>]+>", " ", snippet_raw)
            snippet_words = snippet_text.split()
            snippet = " ".join(snippet_words[:20])
            entries.append((date_str, fpath.name, html.escape(snippet)))

        index_lines: list[str] = []
        index_lines.append("<html><head><meta charset='utf-8'>")
        index_lines.append(
            "<style>body{font-family:sans-serif;} h2{margin-top:1em;} ul{list-style:none;padding:0;} li{margin:0.2em 0;}</style>"
        )
        index_lines.append("</head><body>")
        index_lines.append("<h1>Archive Index</h1>")

        current_date = None
        for date_str, fname, snippet in entries:
            if date_str != current_date:
                if current_date is not None:
                    index_lines.append("</ul>")
                current_date = date_str
                index_lines.append(f"<h2>{html.escape(date_str)}</h2>")
                index_lines.append("<ul>")
            line = f'<li><a href="{html.escape(fname)}">{html.escape(fname)}</a> – {snippet}</li>'
            index_lines.append(line)
        if current_date is not None:
            index_lines.append("</ul>")

        index_lines.append("</body></html>")

        (dir_path / "index.html").write_text("\n".join(index_lines), encoding="utf-8")


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

    def render(self, turns: list[dict]) -> str:
        """Return an HTML table of contents generated from ``turns``."""
        from html import escape

        entries: list[str] = []
        for idx, msg in enumerate(turns, 1):
            role = msg.get("author")
            if role not in {"user", "assistant"}:
                continue
            prefix = "zero:" if role == "user" else "tide:"
            content = msg.get("content", "")
            text = " ".join(content.replace("\n", " ").split())
            words = text.split()[:12]
            label = f"{prefix} {' '.join(words)}"
            entries.append(
                f'<li><a href="#turn-{idx:03d}">{escape(label)}</a></li>'
            )

        groups: list[list[str]] = []
        for i in range(0, len(entries), 20):
            groups.append(entries[i : i + 20])

        lines: list[str] = []
        lines.append('<div class="toc" style="font-size:0.9em;margin-bottom:1em">')
        for g_idx, group in enumerate(groups, 1):
            start = (g_idx - 1) * 20 + 1
            end = start + len(group) - 1
            lines.append(f'<h3>Turns {start}\u2013{end}</h3>')
            lines.append("<ul>")
            lines.extend(group)
            lines.append("</ul>")
        lines.append("</div>")
        return "\n".join(lines)


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

    def run(self, messages: list[dict]) -> list[str]:
        """Return HTML blocks for ``messages`` with summary annotations."""
        import html
        import re
        from collections import Counter

        try:  # attempt to load nltk stopwords
            import nltk
            stop_words = set(nltk.corpus.stopwords.words("english"))
        except Exception:
            # call ``breathing_willow.setup_nltk()`` to download these corpora
            stop_words = {
                "the",
                "and",
                "to",
                "of",
                "a",
                "in",
                "that",
                "it",
                "is",
                "for",
                "on",
                "with",
                "as",
                "this",
                "by",
                "an",
                "be",
            }

        blocks: list[str] = []
        for idx, msg in enumerate(messages, 1):
            role = msg.get("author")
            if role not in {"user", "assistant"}:
                continue
            prefix = "zero" if role == "user" else "tide"
            content = msg.get("content", "")
            esc_content = html.escape(content).replace("\n", "<br>\n")
            blocks.append(f'<div class="{role}-turn" id="turn-{idx:03d}"><h2>{prefix}:</h2>{esc_content}</div>')

            words = [w.lower() for w in re.findall(r"[A-Za-z']+", content)]
            filtered = [w for w in words if w not in stop_words]
            common = [w for w, _ in Counter(filtered).most_common(5)]
            summary = " ".join(common)
            magnifier = "\U0001f50d"  # 🔍
            annot = (
                f'<div class="turn-summary" style="font-size:smaller;color:#666;">'
                f'{magnifier} Summary: {html.escape(summary)} — [{len(content)} chars]'
                "</div>"
            )
            blocks.append(annot)

        return blocks


def annotate_scrolls_in_dir(output_dir: Path) -> None:
    """Annotate each conversation scroll in ``output_dir`` with turn summaries.

    This function searches ``output_dir`` for HTML files matching
    ``*-conversation.html``. For each file it extracts the ordered list of user
    and assistant turns, runs :class:`TurnSummaryAnnotator` to generate
    annotated HTML, then replaces the original turn blocks with the annotated
    ones. All other content in the file is left untouched.

    Parameters
    ----------
    output_dir : Path
        Directory containing exported scroll HTML files.
    """

    dir_path = Path(output_dir)
    annotator = TurnSummaryAnnotator()

    html_files = sorted(dir_path.glob("*-conversation.html"))
    for html_path in html_files:
        print(f"Processing {html_path.name}...")
        try:
            html_text = html_path.read_text(encoding="utf-8")
        except Exception as exc:
            print(f"  Failed to read {html_path}: {exc}")
            continue

        pattern = re.compile(
            r'<div class="(?P<role>user|assistant)-turn">\s*<h2>[^<]+</h2>(?P<content>.*?)</div>',
            re.DOTALL,
        )
        matches = list(pattern.finditer(html_text))
        if not matches:
            print(f"  No turns found, skipping")
            continue

        messages: list[dict] = []
        for m in matches:
            role = m.group("role")
            content_html = m.group("content")
            text = re.sub(r"<br\s*/?>", "\n", content_html)
            text = re.sub(r"<[^>]+>", "", text)
            text = html.unescape(text)
            messages.append({"author": role, "content": text})

        annotated_blocks = annotator.run(messages)
        new_turns = "\n".join(annotated_blocks)

        start = matches[0].start()
        end = matches[-1].end()
        new_html = html_text[:start] + new_turns + html_text[end:]
        try:
            html_path.write_text(new_html, encoding="utf-8")
            print(f"  Annotated {len(messages)} turns")
        except Exception as exc:
            print(f"  Failed to write {html_path}: {exc}")

