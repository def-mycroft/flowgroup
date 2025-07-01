
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
        """Return markdown text with inserted folding markers and memory cues."""
        print("Inserting memory echoes...")

        segments = self._segment_text()
        if len(segments) <= 1:
            return self.md_text

        output_parts = [segments[0]]
        prev_segment = segments[0]
        for idx, segment in enumerate(segments[1:], 1):
            cue = self._make_cue(prev_segment)
            print(f"Cue for segment {idx}: {cue}")
            fold = f"<!-- fold:start -->\n\N{CLOCKWISE OPEN CIRCLE ARROW} memory: {cue}\n<!-- fold:end -->\n"
            print("Inserting fold marker")
            output_parts.append(fold)
            output_parts.append(segment)
            prev_segment = segment
        return "".join(output_parts)

    # simple english stop words
    _STOP_WORDS = {
        "the",
        "a",
        "an",
        "and",
        "or",
        "but",
        "if",
        "while",
        "of",
        "at",
        "by",
        "for",
        "with",
        "about",
        "against",
        "between",
        "into",
        "through",
        "during",
        "before",
        "after",
        "to",
        "from",
        "in",
        "out",
        "on",
        "off",
        "over",
        "under",
        "again",
        "further",
        "then",
        "once",
        "here",
        "there",
        "all",
        "any",
        "both",
        "each",
        "few",
        "more",
        "most",
        "other",
        "some",
        "such",
        "no",
        "nor",
        "not",
        "only",
        "own",
        "same",
        "so",
        "than",
        "too",
        "very",
        "can",
        "will",
        "just",
        "is",
        "are",
        "was",
        "were",
        "be",
        "been",
        "being",
        "have",
        "has",
        "had",
        "do",
        "does",
        "did",
    }

    def _segment_text(self) -> list[str]:
        """Split markdown text into ~token_limit chunks preserving order."""
        print("Segmenting text...")
        token_re = re.compile(r"\b\w+\b")
        segments: list[str] = []
        start = 0
        count = 0
        for match in token_re.finditer(self.md_text):
            count += 1
            if count >= self.token_limit:
                end = self.md_text.find("\n", match.end())
                if end == -1:
                    end = match.end()
                else:
                    end += 1
                segment = self.md_text[start:end]
                print(f"Created segment with {count} tokens")
                segments.append(segment)
                start = end
                count = 0
        segments.append(self.md_text[start:])
        return segments

    def _make_cue(self, text: str) -> str:
        """Generate a short memory cue from ``text``."""
        words = re.findall(r"[A-Za-z]+", text.lower())
        filtered = [w for w in words if w not in self._STOP_WORDS]
        if not filtered:
            filtered = words
        counts = Counter(filtered)
        top_words = [w for w, _ in counts.most_common(7)]
        if len(top_words) < 3:
            extras = [w for w in words if w not in top_words]
            top_words.extend(extras[: 3 - len(top_words)])
        cue = " ".join(top_words[:7])
        return cue


class TokenStatsEmbedder:
    """Embed token-level stats near the top of a markdown document."""

    def __init__(self, md_text: str) -> None:
        self.md_text = md_text

    def _token_count(self) -> int:
        tokens = re.findall(r"\b\w+\b", self.md_text)
        count = len(tokens)
        print(f"Token count: {count}")
        return count

    def _segment_count(self) -> int:
        count = len(re.findall(r"<!--\s*fold:start\s*-->", self.md_text))
        print(f"Segment count: {count}")
        return count

    def _turn_counts(self) -> Counter:
        turns = Counter()
        for role in re.findall(r"^##\s*(zero|tide):", self.md_text, re.MULTILINE):
            turns[role] += 1
        print(f"Turn counts: {dict(turns)}")
        return turns

    def _insert_block(self, block: str) -> str:
        lines = self.md_text.splitlines(keepends=True)
        insert_at = 0
        if lines and lines[0].strip() == "---":
            close = None
            for i in range(1, len(lines)):
                if lines[i].strip() == "---":
                    close = i
                    break
            if close is not None:
                insert_at = close + 1
                while insert_at < len(lines) and lines[insert_at].strip() == "":
                    insert_at += 1
            else:
                print("Warning: frontmatter start found without closing '---'")
                insert_at = 1
        prefix = "".join(lines[:insert_at])
        suffix = "".join(lines[insert_at:])
        return prefix + block + suffix

    def embed_stats(self) -> str:
        token_count = self._token_count()
        segment_count = self._segment_count()
        turns = self._turn_counts()
        total_turns = turns.get("zero", 0) + turns.get("tide", 0)
        stats_block = (
            "<!-- stats:start -->\n"
            f"tokens: {token_count}  \n"
            f"segments: {segment_count}  \n"
            f"turns: {total_turns} (zero: {turns.get('zero', 0)}, tide: {turns.get('tide', 0)})\n"
            "<!-- stats:end -->\n"
        )
        return self._insert_block(stats_block)


class MarkdownExporter:
    """Handles writing the final .md output to disk, ensuring scroll-readability and UTF-8."""

    def __init__(self, parsed_text: str, output_path: Path) -> None:
        self.parsed_text = parsed_text
        self.output_path = Path(output_path)
        try:
            self.output_path.parent.mkdir(parents=True, exist_ok=True)
        except Exception as exc:
            raise RuntimeError(
                f"Unable to create directory {self.output_path.parent}: {exc}"
            ) from exc
        print(f"MarkdownExporter initialized for {self.output_path}")

    def write(self) -> None:
        """Write the shaped markdown to disk."""
        print(f"Opening {self.output_path} for writing")
        try:
            with self.output_path.open(mode="w", encoding="utf-8") as fh:
                print("Writing shaped markdown...")
                fh.write(self.parsed_text)
                fh.write("\n\n<!-- export_kernel:v0 -->")
        except Exception:
            print(f"Failed to write {self.output_path}")
            traceback.print_exc()
            return
        print(f"✅ Exported to {self.output_path}")


def main() -> None:
    """Entry point for running the export kernel."""
    zip_path = Path("chatgpt_export.zip")
    output_dir = Path("field/chatgpt-export/2025-07-01")
    archiver = ChatExportArchiver(zip_path, output_dir)
    archiver.run()


if __name__ == "__main__":
    main()
