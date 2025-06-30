from pathlib import Path

_ASSET_DIR = Path(__file__).resolve().parent / "assets"


def load_asset(name: str) -> str:
    """Return text content of asset ``name``.

    Parameters
    ----------
    name : str
        Base filename within the assets directory (without extension).

    Returns
    -------
    str
        The file's text content.
    """
    path = _ASSET_DIR / f"{name}.txt"
    return path.read_text()


__all__ = ["load_asset"]
