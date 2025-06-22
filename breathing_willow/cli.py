import argparse
from . import __version__

def cmd_version(_args: argparse.Namespace) -> None:
    """Print the current version."""
    print(__version__)


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="breathing-willow")
    subparsers = parser.add_subparsers(dest="command", required=True)

    version_parser = subparsers.add_parser("version", help="Show project version")
    version_parser.set_defaults(func=cmd_version)

    return parser


def main(argv=None) -> None:
    parser = build_parser()
    args = parser.parse_args(argv)
    args.func(args)


if __name__ == "__main__":
    main()
