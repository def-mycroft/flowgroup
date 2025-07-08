"""
# relevant_files.py

This module finds files most similar to a reference file within the vault,
based on string similarity and file modification time (mtime). It outputs a
markdown list of paths to `/field/relevant-files.md`, starting with the input.

Vault keys are file paths, and values are their full text content.
"""

import subprocess
from pathlib import Path
from difflib import SequenceMatcher
from datetime import datetime
from zero_obsidian import helpers as oh


DEFAULT_FP_INPUT = "/field/prompt.md"
FP_OUTPUT = Path("/field/relevant-files.md")


import spacy
from numpy import dot
from numpy.linalg import norm

# load once at module level
nlp = spacy.load("en_core_web_md")  # or "en_core_web_lg" if available

def text_similarity(a: str, b: str) -> float:
    """Use spaCy vector cosine similarity between documents."""
    doc_a = nlp(a)
    doc_b = nlp(b)
    if not doc_a.vector_norm or not doc_b.vector_norm:
        return 0.0
    return dot(doc_a.vector, doc_b.vector) / (norm(doc_a.vector) * norm(doc_b.vector))


def get_mtime(filepath: str) -> datetime | None:
    """Return last modified time of file as datetime."""
    try:
        result = subprocess.run(
            ["stat", "-c", "%Y", filepath],
            capture_output=True, text=True, check=True
        )
        timestamp = int(result.stdout.strip())
        return datetime.fromtimestamp(timestamp)
    except Exception as e:
        print(f"[warn] couldn't get mtime for {filepath}: {e}")
        return None



def get_recent_vault(days: int = 365, tags_exclude: list[str] = ["#project"]) -> dict[str, str]:
    """Return vault entries with mtime within the last `days`, excluding tags."""
    now = datetime.now()
    cutoff = now.timestamp() - days * 86400
    vault = oh.load_vault()
    recent = {}
    for path, content in vault.items():
        if any(tag in content for tag in tags_exclude):
            continue
        try:
            mtime = Path(path).stat().st_mtime
            if mtime > cutoff:
                recent[path] = content
        except FileNotFoundError:
            continue
    print(f"[vault] {len(recent)} recent files retained")
    return recent


def find_relevant_files(fp_input: str = DEFAULT_FP_INPUT, n_similar: int = 30, days: int = 365) -> Path:
    """Generate a list of files most similar to fp_input and write to markdown list."""
    ref_path = Path(fp_input)
    if not ref_path.exists():
        raise FileNotFoundError(f"Missing input file: {fp_input}")
    ref_text = ref_path.read_text(encoding="utf-8")

    vault = get_recent_vault(days=days)
    if not vault:
        raise RuntimeError("[error] no recent files found in vault")

    sims = [(fp, text_similarity(ref_text, content)) for fp, content in vault.items()]
    ranked = sorted(sims, key=lambda x: x[1], reverse=True)[:n_similar]
    filepaths = [fp_input] + [fp for fp, _ in ranked]

    FP_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    FP_OUTPUT.write_text("\n".join(f"- {fp}" for fp in filepaths), encoding="utf-8")
    print(f"[done] wrote {len(filepaths)} paths to {FP_OUTPUT}")
    return FP_OUTPUT


endpoint = find_relevant_files
