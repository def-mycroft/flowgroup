from pathlib import Path
import tiktoken

def snip_file_to_last_tokens(fp, context_scope='max') -> str:
    """Snips a file's content to the last N tokens based on context_scope.

    This function reads the content of the file at `fp`, encodes it using the GPT-4o
    tokenizer, and returns only the last N tokens of the text, where N depends on the
    selected `context_scope`.

    If `context_scope='max'`, the function uses the full context window of GPT-4o, 
    approximately 128,000 tokens. This setting ensures no relevant context is lost, 
    suitable for full-document ingestion, memory construction, or longform synthesis.

    If `context_scope='practical'`, the function uses 3,000 tokens. This value is chosen
    to reflect the rough upper bound of recent, immediately relevant content in most 
    conversations, prompts, or extractive tasks. It avoids overhead while preserving
    useful trailing context.

    Valid values for `context_scope`: 'max', 'practical'.
    """
    if context_scope == 'max':
        n_tokens = 128_000  # GPT-4o max context length
    elif context_scope == 'practical':
        n_tokens = 3_000
    else:
        raise ValueError("context_scope must be 'max' or 'practical'")

    enc = tiktoken.encoding_for_model("gpt-4")
    text = Path(fp).read_text(encoding='utf-8')
    tokens = enc.encode(text)
    snipped_tokens = tokens[-n_tokens:]
    return enc.decode(snipped_tokens)


endpoint = snip_file_to_last_tokens
