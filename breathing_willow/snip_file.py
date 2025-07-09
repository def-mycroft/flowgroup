from pathlib import Path
import tiktoken


def snip_file_to_last_tokens(fp, context_scope='practical', aggressive=False,
                             n_tokens='0') -> str:
    """Snips a file to retain only the last N tokens of text content.

    Reads a file, tokenizes its contents using the GPT tokenizer, and returns
    (or optionally overwrites) only the final N tokens. The value of N is chosen
    based on the `context_scope` setting, which controls whether to preserve a
    maximum or practical context window for downstream processing.

    ## Parameters
    fp : str or Path
        Path to the input file to be snipped.
    context_scope : {'max', 'practical'}, optional
        Token retention policy. If 'max', retains 128,000 tokens. If 'practical',
        retains 3,000. Defaults to 'practical'.
    aggressive : bool, optional
        If True, overwrites the original file with the snipped output. If False,
        returns the truncated text as a string. Defaults to False.
    n_tokens : int, optional
        if passed, then last n tokens will be taken. arg context_scope will be
        simply overwritten 

    ## Returns
    str
        The truncated text content, unless `aggressive` is True.

    ## Raises
    ValueError
        If `context_scope` is not one of {'max', 'practical'}.
    """
    n_tokens = int(n_tokens)
    if not n_tokens > 0:
        if context_scope == 'max':
            n_tokens = 128_000  # GPT-4o max context length
        elif context_scope == 'practical':
            n_tokens = 3_000
        else:
            raise ValueError("context_scope must be 'max' or 'practical'")
    else:
        n_tokens = int(n_tokens)

    enc = tiktoken.encoding_for_model("gpt-4")
    text = Path(fp).read_text(encoding='utf-8')
    tokens = enc.encode(text)
    snipped_tokens = tokens[-n_tokens:]

    o = enc.decode(snipped_tokens)
    if not aggressive:
        return o
    else:
        with open(fp, 'w') as f:
            f.write(o)

endpoint = snip_file_to_last_tokens
