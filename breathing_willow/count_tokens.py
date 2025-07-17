import tiktoken


def get_token_count_model(text, model='gpt-4o'):
    """Return token count for the given text using the specified OpenAI model."""
    enc = tiktoken.encoding_for_model(model)
    return len(enc.encode(text))

endpoint = get_token_count_model
