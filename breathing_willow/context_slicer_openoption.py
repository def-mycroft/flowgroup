from pathlib import Path
from collections import Counter
from string import punctuation

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
    text: str,
    fp_output: str = "/field/parsed-context.md",
    chunk_n_tokens: int = 3000,
) -> Path:
    """Splits a text into token-based chunks and writes a structured markdown file.

    This function divides the input `text` into segments of approximately
    `chunk_n_tokens` tokens using the GPT tokenizer. Each chunk is annotated with a
    title and an abstract derived from keyword frequency. The final output is a
    markdown file containing all chunks in a standardized format, written to
    `fp_output`.

    ## Parameters
    text : str
        The full input text to be segmented and annotated.
    fp_output : str, optional
        Path to the output markdown file. Defaults to
        "/field/parsed-context.md".
    chunk_n_tokens : int, optional
        Approximate number of tokens per chunk. Defaults to 3000.

    ## Returns
    Path
        The path to the generated markdown file containing all annotated chunks.
    """
    if not text.strip():
        raise ValueError("Input text is empty or whitespace.")

    enc = tiktoken.encoding_for_model("gpt-4")
    tokens = enc.encode(text)

    # Convert token sequence back into chunked strings
    chunks = []
    for i in range(0, len(tokens), chunk_n_tokens):
        chunk_ids = tokens[i:i + chunk_n_tokens]
        chunk_text = enc.decode(chunk_ids)
        if not chunk_text.strip():
            continue
        chunks.append(chunk_text)

    rendered_chunks = []
    for chunk_text in chunks:
        if not chunk_title:
            chunk_title = "untitled"
        if not chunk_abstract:
            chunk_abstract = "no summary available"
        chunk_title = make_abstract(chunk_text, 3)
        chunk_abstract = make_abstract(chunk_text, 500)
        rendered = TEMPLATE.render(
            chunk_title=chunk_title,
            chunk_abstract=chunk_abstract,
            raw_chunk_text=chunk_text
        )
        rendered_chunks.append(rendered.strip())

    output_path = Path(fp_output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    with output_path.open("w", encoding="utf-8") as f:
        f.write("\n\n".join(rendered_chunks))

    print(f"wrote '{output_path}'")

    return output_path

endpoint = slice_context_to_chunks
