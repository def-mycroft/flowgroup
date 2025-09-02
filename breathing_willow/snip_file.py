from pathlib import Path
import shutil
import tiktoken


def snip_file_to_last_tokens(
    fp,
    context_scope: str = "practical",
    aggressive: bool = False,
    n_tokens: str = "0",
    output_path: str | Path | None = None,
) -> str:
    """Snip ``fp`` to retain only the last ``n_tokens`` tokens of text.

    Parameters
    ----------
    fp : str or Path
        Path to the input file to be snipped.
    context_scope : {"max", "practical"}, optional
        Token retention policy. If ``"max"``, retains 128,000 tokens. If
        ``"practical"``, retains 3,000. Defaults to ``"practical"``.
    aggressive : bool, optional
        If True, write the snipped output to ``output_path`` if provided,
        otherwise overwrite ``fp`` after creating a ``.bak`` backup. If False,
        only return the snipped text. Defaults to False.
    n_tokens : int, optional
        If passed, then last ``n`` tokens will be taken. ``context_scope`` will
        be ignored. Defaults to "0".
    output_path : str or Path, optional
        Destination for snipped content when ``aggressive`` is True. If omitted
        the original file is overwritten after backing it up.

    Returns
    -------
    str
        The truncated text content.

    Raises
    ------
    ValueError
        If ``context_scope`` is not one of {"max", "practical"}.
    FileNotFoundError
        If ``fp`` does not exist.
    OSError
        If an I/O error occurs while reading or writing files.
    """
    n_tokens = int(n_tokens)
    if not n_tokens > 0:
        if context_scope == "max":
            n_tokens = 128_000  # GPT-4o max context length
        elif context_scope == "practical":
            n_tokens = 3_000
        else:
            raise ValueError("context_scope must be 'max' or 'practical'")
    else:
        n_tokens = int(n_tokens)

    src_path = Path(fp)
    try:
        text = src_path.read_text(encoding="utf-8")
    except FileNotFoundError as e:
        raise FileNotFoundError(f"file not found: {src_path}") from e
    except OSError as e:
        raise OSError(f"error reading {src_path}: {e}") from e

    enc = tiktoken.encoding_for_model("gpt-4")
    tokens = enc.encode(text)
    snipped_tokens = tokens[-n_tokens:]
    o = enc.decode(snipped_tokens)

    if aggressive:
        target = Path(output_path) if output_path else src_path
        if target.resolve() == src_path.resolve():
            backup_path = src_path.with_suffix(src_path.suffix + ".bak")
            try:
                shutil.copy2(src_path, backup_path)
            except OSError as e:
                raise OSError(f"error creating backup {backup_path}: {e}") from e
        try:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(o, encoding="utf-8")
        except OSError as e:
            raise OSError(f"error writing {target}: {e}") from e

    return o

endpoint = snip_file_to_last_tokens
