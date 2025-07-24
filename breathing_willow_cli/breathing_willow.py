"""Breathing Willow command line interface.

The :mod:`breathing_willow_cli` package exposes a small command suite under the
``willow`` entry point.  Each subcommand performs a focused task, from running a
conceptual diff to bootstrapping prompt development files.  The CLI is intended
to be lightweight and composable so you can weave these utilities into your own
flows.
"""

from __future__ import annotations

import argparse
from typing import Sequence

from .utils import get_version
from .subcommands import add_subcommands


def build_parser() -> argparse.ArgumentParser:
    """Create the top-level argument parser."""
    parser = argparse.ArgumentParser(
        prog="breathing-willow", description="Breathing Willow CLI"
    )
    parser.add_argument("--version", action="store_true", help="show version and exit")
    subparsers = parser.add_subparsers(dest="command")
    add_subcommands(subparsers)
    return parser


def main(argv: Sequence[str] | None = None) -> None:
    """Entry point for the Breathing Willow CLI."""
    parser = build_parser()
    args = parser.parse_args(argv)
    version = get_version()

    if args.version or args.command is None:
        print(f"Breathing Willow version {version} - CLI is alive!")
        return

    if not hasattr(args, "func"):
        parser.error("No command specified")

    args.func(args)


if __name__ == "__main__":
    main()
