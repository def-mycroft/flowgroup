from pathlib import Path
import re

_ASSET_DIR = Path(__file__).resolve().parent / "assets"


def strip_markdown_formatting(fp: str) -> None:
    """Aggressively strip markdown formatting from a file and overwrite it."""
    path = Path(fp)
    text = path.read_text()

    # Remove code blocks and inline code
    text = re.sub(r"```.*?```", "", text, flags=re.DOTALL)
    text = re.sub(r"`[^`]+`", "", text)

    # Remove images and links, keep alt or link text
    text = re.sub(r"!\[.*?\]\(.*?\)", "", text)
    text = re.sub(r"\[(.*?)\]\(.*?\)", r"\1", text)

    # Remove markdown headers
    text = re.sub(r"^#+\s*", "", text, flags=re.MULTILINE)

    # Remove emphasis and bold
    text = re.sub(r"[*_]{1,3}", "", text)

    # Remove blockquotes and horizontal rules
    text = re.sub(r"^>\s*", "", text, flags=re.MULTILINE)
    text = re.sub(r"^[-*_]{3,}$", "", text, flags=re.MULTILINE)

    # Remove list markers
    text = re.sub(r"^(\s*[-*+]\s+|\s*\d+\.\s+)", "", text, flags=re.MULTILINE)

    # Strip remaining markdown table artifacts
    text = re.sub(r"\|", " ", text)
    text = re.sub(r":?-+:?", "", text)

    path.write_text(text)


def load_asset(name, ext='txt'):
    """Return text content of asset ``name``.

    Parameters
    ----------
    name : str
        Base filename within the assets directory (without extension).
    ext : str
        filename extension. 

    Returns
    -------
    str
        The file's text content.
    """
    path = _ASSET_DIR / f"{name}.{ext}"
    return path.read_text()


__all__ = ["load_asset"]
