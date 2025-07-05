from pathlib import Path
from bs4 import BeautifulSoup
import tiktoken

def count_tokens(text, model="gpt-4"):
    """Count tokens in a text string for a given OpenAI model."""
    enc = tiktoken.encoding_for_model(model)
    return len(enc.encode(text))


def parse_conversation(fp_html):
    """Parse an HTML transcript into structured conversation turns.

    Parameters
    ----------
    fp_html : str or Path
        Filepath to the HTML file containing the conversation.

    Returns
    -------
    list of dict
        Each dict has 'role' (either 'user' or 'chatgpt') and 'text' fields.
    """
    with open(fp_html, "r", encoding="utf-8") as f:
        soup = BeautifulSoup(f, "html.parser")

    results = []
    turns = soup.find_all("div", class_="user-turn") + soup.find_all("div", class_="assistant-turn")
    for turn in sorted(turns, key=lambda x: x.sourceline or 0):
        if "user-turn" in turn.get("class", []):
            role = "user"
        elif "assistant-turn" in turn.get("class", []):
            role = "chatgpt"
        else:
            continue
        header = turn.find("h2")
        if header:
            header.extract()
        text = turn.get_text(strip=True, separator="\n")
        results.append({"role": role, "text": text})

    return results


def export_snippets_markdown(fp_html, out_path="/field/conversation_snippets.md",
                             model="gpt-4", max_tokens=3000):
    """Parse and chunk a ChatGPT conversation, then write to markdown.

    Each snippet is separated under a H1 heading and grouped under H2 meta-chunks.

    Parameters
    ----------
    fp_html : str or Path
        Filepath to the input HTML export from ChatGPT.
    out_path : str or Path
        Path where the markdown file will be written.
    model : str
        Model name for token counting.
    max_tokens : int
        Maximum tokens per meta-chunk.
    """
    thread = parse_conversation(fp_html)

    # Chunk conversation into token-constrained blocks
    n = 0
    text = ''
    snippets = []
    for x in thread:
        t = '\n***\n' + f"role: {x['role']}\n" + x['text']
        nt = count_tokens(t, model)
        if n + nt < max_tokens:
            text += t
            n = count_tokens(text, model)
        else:
            snippets.append(text.strip())
            text = t
            n = count_tokens(t, model)
    if text.strip():
        snippets.append(text.strip())

    # Write to markdown with meta-chunk grouping
    out_path = Path(out_path)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    with open(out_path, "w", encoding="utf-8") as f:
        meta_idx = 1
        meta_tokens = 0
        f.write(f"## Meta-Chunk {meta_idx}\n\n")

        for i, chunk in enumerate(snippets, 1):
            t = f"# Chunk {i}\n\n{chunk}\n\n"
            t_tokens = count_tokens(t, model)

            if meta_tokens + t_tokens > max_tokens:
                meta_idx += 1
                f.write(f"## Meta-Chunk {meta_idx}\n\n")
                meta_tokens = 0

            print(f"tokens: {t_tokens}")
            f.write(t)
            meta_tokens += t_tokens

    return out_path

