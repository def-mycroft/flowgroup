import argparse
from pathlib import Path


def get_version():
    version_file = Path(__file__).resolve().parent.parent / "VERSION.md"
    if version_file.exists():
        for line in version_file.read_text().splitlines()[::-1]:
            line = line.strip()
            if line.startswith("vc"):
                return line.split()[0]
    return "0.0.0"


def main(argv=None):
    parser = argparse.ArgumentParser(
        prog="breathing-willow",
        description="Breathing Willow CLI"
    )
    parser.add_argument(
        "--version",
        action="store_true",
        help="show version and exit"
    )
    subparsers = parser.add_subparsers(dest="command")

    args = parser.parse_args(argv)
    version = get_version()

    if args.version or args.command is None:
        print(f"Breathing Willow version {version} - CLI is alive!")
        return


if __name__ == "__main__":
    main()
