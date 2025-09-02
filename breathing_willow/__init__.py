"""Breathing Willow core package."""

from .clipboard_agent import ClipboardAgent
from .helpers import setup_nltk

__version__ = "0.1.0"

__all__ = ["__version__", "ClipboardAgent", "setup_nltk"]
