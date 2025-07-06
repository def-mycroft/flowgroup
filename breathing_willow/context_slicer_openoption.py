from pathlib import Path
from collections import Counter
from string import punctuation
from uuid import uuid4 as uuid


import spacy
from jinja2 import Template


################################################################################
# TODO - remove this 

# TODO - undo dev imports 
#import tiktoken

def generate_chunks(alist, n):
    """Returns a generator object which can be turned into a list"""
    # for item i in range of alist
    for i in range(0, len(alist), n):
        # create an index range for l of n items:
        yield alist[i:i + n]


def chunk_list(alist, n):
    """Returns a list of list items from alist in n-sized chunks"""
    return list(generate_chunks(alist, n))


################################################################################

nlp = spacy.load("en_core_web_sm")
TEMPLATE = Template(
    "## {{ chunk_title }}\n"
    "BEGIN {{ chunk_title }}\n\n"
    "{{ chunk_abstract }}\n\n"
    "{{ raw_chunk_text }}\n\n"
    "END {{ chunk_title }}\n"
)


def count_tokens(text: str, model: str = "gpt-4") -> int:
    # TODO - fix this to use tiktoken count
    #enc = tiktoken.encoding_for_model(model)
    #return len(enc.encode(text))
    return len(text.split(' '))


def make_abstract(text: str, n_words: int) -> str:
    doc = nlp(text.lower())
    tokens = [t.text for t in doc if not t.is_stop and t.text not in punctuation and t.is_alpha]
    freq = Counter(tokens).most_common(n_words)
    return " ".join(word for word, _ in freq)


def slice_context_to_chunks(
    fp_input, 
    fp_output: str = "/field/parsed-context.md",
    chunk_n_tokens: int = 3000,
) -> Path:
    """Splits a text into token-based chunks and writes a structured markdown file.

    # TODO - this docstring isn't accurate naymore

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
    with open(fp_input, 'r') as f:
        text = f.read()

    if not text.strip():
        raise ValueError("Input text is empty or whitespace.")

    #enc = tiktoken.encoding_for_model("gpt-4")
    #tokens = enc.encode(text)
    # TODO - re-integrate tiktoken after getting back online
    #tokens = text.split(' ')

    ############################################################################
    ## Convert token sequence back into chunked strings
    #chunks = []
    #for i in range(0, len(tokens), chunk_n_tokens):
    #    chunk_ids = tokens[i:i + chunk_n_tokens]
    #    chunk_text = enc.decode(chunk_ids)
    #    if not chunk_text.strip():
    #        continue
    #    chunks.append(chunk_text)
    # TODO - get rid of this 
    chunks = [' '.join(x) for x in chunk_list(text.split(' '), 1500)]

    # TODO - idk why chunk title isn'th ere? 
    chunk_title = ''
    chunk_abstract = ''

    ############################################################################

    rendered_chunks = []
    for idx,chunk_text in enumerate(chunks):
        if not chunk_title:
            chunk_title = "untitled"
        if not chunk_abstract:
            chunk_abstract = "no summary available"


        ########################################################################
        # TODO - idk 
        ########################################################################
        chunk_abstract = make_abstract(chunk_text, 100)
        chunk_title = make_abstract(chunk_text, 10)
        cn = str(uuid()).split('-')[0]
        chunk_title = f"{idx+1:.0f}-{cn.replace(' ', '-')}-{chunk_title.replace(' ', '-')}"


        print('ABSTRACT')
        print(chunk_abstract)
        print('TITLE')
        print(chunk_title)
        print()
        ########################################################################

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
