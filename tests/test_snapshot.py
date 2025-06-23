import re
from pathlib import Path
import uuid

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow_cli.breathing_willow import main as cli_main


def test_update_net_snapshot(tmp_path: Path):
    doc = tmp_path / "test-shape.md"
    doc.write_text("hello world")
    out_html = tmp_path / "out.html"

    argv = [
        "update-net",
        "--visual-archive",
        str(out_html),
        "-f",
        str(doc),
    ]
    cli_main(argv)

    snaps = list(tmp_path.glob("test-shape-*.md"))
    assert len(snaps) == 1
    first = snaps[0]
    text = first.read_text().splitlines()
    uid_line = text[0].strip()
    dt_line = text[1].strip()
    assert uuid.UUID(uid_line)
    assert re.match(r"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} [-+]\d{4} America/Denver", dt_line)
    assert "***" in text[3]
    assert "hello world" in first.read_text()

    # run again, ensure new snapshot created
    cli_main(argv)
    snaps2 = list(tmp_path.glob("test-shape-*.md"))
    assert len(snaps2) == 2
    names = {p.name for p in snaps2}
    assert len(names) == 2
