from pathlib import Path
from collections import Counter
from string import punctuation
from uuid import uuid4 as uuid

import spacy
import tiktoken
from jinja2 import Template

nlp = spacy.load("en_core_web_sm")
TEMPLATE = Template(
    "## {{ chunk_title }}\n"
    "BEGIN {{ chunk_title }}\n\n"
    "{{ chunk_abstract }}\n\n"
    "{{ raw_chunk_text }}\n\n"
    "END {{ chunk_title }}\n"
)


def count_tokens(text: str, model: str = "gpt-4") -> int:
    enc = tiktoken.encoding_for_model(model)
    return len(enc.encode(text))


def make_abstract(text: str, n_words: int) -> str:
    doc = nlp(text.lower())
    tokens = [t.text for t in doc if not t.is_stop and t.text not in punctuation and t.is_alpha]
    freq = Counter(tokens).most_common(n_words)
    return " ".join(word for word, _ in freq)


def slice_context_to_chunks(
    fp_input: str,
    fp_output: str = "/field/parsed-context.md",
    chunk_n_tokens: int = 3000,
) -> Path:
    """Slices input text into token-based chunks and outputs markdown with summary blocks."""
    with open(fp_input, "r", encoding="utf-8") as f:
        text = f.read()

    if not text.strip():
        raise ValueError("Input text is empty or whitespace.")

    enc = tiktoken.encoding_for_model("gpt-4")
    tokens = enc.encode(text)

    chunks = []
    for i in range(0, len(tokens), chunk_n_tokens):
        chunk_ids = tokens[i:i + chunk_n_tokens]
        chunk_text = enc.decode(chunk_ids).strip()
        if chunk_text:
            chunks.append(chunk_text)

    rendered_chunks = []
    for idx, chunk_text in enumerate(chunks):
        chunk_abstract = make_abstract(chunk_text, 100)
        title_tokens = make_abstract(chunk_text, 10)
        uid = str(uuid()).split("-")[0]
        chunk_title = f"{idx+1}-{uid}-{title_tokens.replace(' ', '-')}"
        rendered = TEMPLATE.render(
            chunk_title=chunk_title,
            chunk_abstract=chunk_abstract,
            raw_chunk_text=chunk_text
        )
        rendered_chunks.append(rendered.strip())

    output_path = Path(fp_output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text("\n\n".join(rendered_chunks), encoding="utf-8")

    print(f"wrote '{output_path}'")
    return output_path

endpoint = slice_context_to_chunks
