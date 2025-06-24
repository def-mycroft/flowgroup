import argparse
import re
from pathlib import Path
from datetime import datetime, timedelta, timezone
from typing import Sequence, Dict

STOP_WORDS = {
    'a','an','the','and','or','but','if','while','of','at','by','for','with','about','against','between','into','through','during','before','after','to','from','in','out','on','off','over','under','again','further','then','once','here','there','all','any','both','each','few','more','most','other','some','such','no','nor','not','only','own','same','so','than','too','very','can','will','just'
}



def format_word_cloud(cloud: Dict[str, float]) -> str:
    """Format word cloud as markdown table."""
    if not cloud:
        return "_(no words found)_"
    lines = ["| Word | Weight |", "| --- | --- |"]
    for w, v in cloud.items():
        lines.append(f"| {w} | {v:.2f} |")
    return "\n".join(lines)



def find_modified_texts(root: str, start: datetime, end: datetime) -> list[Path]:
    """Return list of basic text files modified in given UTC time range.

    Parameters
    ----------
    root : str
        Root directory to search (e.g. '/field').
    start : datetime
        Start of time range (timezone-aware, UTC).
    end : datetime
        End of time range (timezone-aware, UTC).

    Returns
    -------
    list of Path
        List of Path objects for text-like files modified in range.

    Raises
    ------
    ValueError
        If start or end is not timezone-aware.
    """
    if start.tzinfo is None or end.tzinfo is None:
        raise ValueError("start and end must be timezone-aware (UTC) datetimes")

    exts = {'.txt', '.md', '.rst', '.log', '.text'}
    paths: list[Path] = []
    for p in Path(root).rglob('*'):
        if p.suffix.lower() not in exts:
            continue
        mtime = datetime.fromtimestamp(p.stat().st_mtime, timezone.utc)
        if start <= mtime < end:
            paths.append(p)
    return paths


def word_cloud(doc: str, size_n: int = 50) -> Dict[str, float]:
    """Generate a weighted word cloud from markdown text.

    This function extracts words from markdown-formatted text,
    filters out common stop words, counts word frequencies,
    and returns a dictionary mapping words to normalized weights.
    Weights are scaled so that the most frequent word has weight 1.0.

    The result is a simple word cloud representation suitable for
    comparing document content, computing diffs, or visualization.
    This implementation favors stability and clarity over visual
    layout — it returns word weights, not graphical positions.

    Parameters
    ----------
    doc : str
        The markdown text to analyze.
    size_n : int, optional
        The maximum number of words to return (default is 50).

    Returns
    -------
    Dict[str, float]
        Dictionary mapping words to normalized weights in [0.0, 1.0],
        sorted by decreasing weight (most salient words first).

    Notes
    -----
    - Words shorter than 2 characters are excluded.
    - The result is case-insensitive.
    - The STOP_WORDS set controls which words are filtered.
    """
    tokens = re.findall(r"\b[a-zA-Z]{2,}\b", doc.lower())
    freq: Dict[str, int] = {}
    for t in tokens:
        if t in STOP_WORDS:
            continue
        freq[t] = freq.get(t, 0) + 1
    if not freq:
        return {}
    max_f = max(freq.values())
    weights = {w: freq[w]/max_f for w in freq}
    items = sorted(weights.items(), key=lambda x: (-x[1], x[0]))[:size_n]
    return dict(items)


def _jaccard(a: Dict[str, float], b: Dict[str, float]) -> float:
    if not a and not b:
        return 0.0
    set_a = set(a)
    set_b = set(b)
    inter = len(set_a & set_b)
    union = len(set_a | set_b)
    return 1 - (inter / union)


def parse_duration(text: str) -> timedelta:
    m = re.match(r"(\d+)([dhm])", text)
    if not m:
        raise ValueError(f"bad duration: {text}")
    value = int(m.group(1))
    unit = m.group(2)
    if unit == 'd':
        return timedelta(days=value)
    if unit == 'h':
        return timedelta(hours=value)
    return timedelta(minutes=value)


def _collect_docs(root: Path, start: datetime, end: datetime) -> Sequence[str]:
    texts: list[str] = []
    for p in root.rglob('*.md'):
        mtime = datetime.fromtimestamp(p.stat().st_mtime, timezone.utc)
        if start <= mtime < end:
            texts.append(p.read_text())
    return texts


def compute_diff(root: Path, window: timedelta, back: timedelta) -> float:
    now = datetime.now(timezone.utc)
    current_start = now - window
    prev_start = current_start - back
    prev_end = current_start
    current_texts = _collect_docs(root, current_start, now)
    prev_texts = _collect_docs(root, prev_start, prev_end)
    cloud_now: Dict[str, float] = {}
    for t in current_texts:
        for k,v in word_cloud(t).items():
            cloud_now[k] = max(cloud_now.get(k,0), v)
    cloud_prev: Dict[str, float] = {}
    for t in prev_texts:
        for k,v in word_cloud(t).items():
            cloud_prev[k] = max(cloud_prev.get(k,0), v)
    return _jaccard(cloud_now, cloud_prev)


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog='w', description='w diff tool')
    sub = p.add_subparsers(dest='command', required=True)
    diff_p = sub.add_parser('diff', help='conceptual diff')
    diff_p.add_argument('--window', default='24h', help='window size')
    diff_p.add_argument('--back', help='how far back to compare')
    diff_p.add_argument('--dir', default='/l/obs-chaotic/', help='root directory')
    diff_p.add_argument('--out', help='output file for markdown log')
    diff_p.add_argument('--verbose', action='store_true')
    diff_p.add_argument('--live', action='store_true')
    diff_p.add_argument('--graph', action='store_true')
    diff_p.set_defaults(func=cmd_diff)
    return p


def _markdown_log(score: float, root: Path, start: datetime, end: datetime, cloud_md: str) -> str:
    ts1 = start.isoformat(timespec='seconds')
    ts2 = end.isoformat(timespec='seconds')
    return (
        f"# Shaping Progress — Conceptual Diff  \n"
        f"_Window:_ {ts1} → {ts2}  \n"
        f"_Directory:_ {root}  \n"
        f"_Score:_ **{score:.2f}** (scale: 0 = stable, 1+ = strong shift)\n\n---\n"
        f"{cloud_md}\n"
        f"\n***\n"
    )


def cmd_diff(args: argparse.Namespace) -> None:
    window = parse_duration(args.window)
    back = parse_duration(args.back) if args.back else window
    root = Path(args.dir)
    score = compute_diff(root, window, back)
    start = datetime.now(timezone.utc) - window
    log = _markdown_log(score, root, start, datetime.now(timezone.utc))
    if args.out:
        Path(args.out).write_text(log)
    else:
        print(log)


def export_diff(root: str, window: str = '24h', back: str | None = None) -> str:
    """Compute conceptual diff and return formatted markdown log.

    Parameters
    ----------
    root : str
        Path to the root directory of documents.
    window : str, optional
        Size of the current analysis window (e.g. '24h', '7d').
    back : str, optional
        How far back to compare against (e.g. '48h', '14d').
        If omitted, uses same size as `window`.

    Returns
    -------
    str
        Markdown-formatted conceptual diff log.
    """
    w = parse_duration(window)
    b = parse_duration(back) if back else w
    p_root = Path(root)
    score = compute_diff(p_root, w, b)
    now = datetime.now(timezone.utc)
    start = now - w

    paths = find_modified_texts(root, start, now)
    cloud: Dict[str, float] = {}
    for p in paths:
        for k, v in word_cloud(p.read_text()).items():
            cloud[k] = max(cloud.get(k, 0), v)

    cloud_md = format_word_cloud(cloud)
    return _markdown_log(score, p_root, start, now, cloud_md)


def main(argv: Sequence[str] | None = None) -> None:
    parser = build_parser()
    args = parser.parse_args(argv)
    args.func(args)


if __name__ == '__main__':
    main()
