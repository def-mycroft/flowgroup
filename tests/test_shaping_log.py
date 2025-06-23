import re
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow_cli.breathing_willow import main as cli_main


def test_update_net_shaping_log(tmp_path, monkeypatch):
    doc = tmp_path / "shape.txt"
    doc.write_text("alpha beta 123e4567-e89b-12d3-a456-426614174000 gamma")
    out_html = tmp_path / "out.html"

    log_file = tmp_path / "willow-shaping.md"
    monkeypatch.setenv("WILLOW_SHAPING_LOG", str(log_file))

    argv = [
        "update-net",
        "--visual-archive",
        str(out_html),
        "-f",
        str(doc),
    ]
    cli_main(argv)
    first = log_file.read_text()
    assert str(doc) in first
    assert "shape.txt" in first
    assert "123e4567-e89b-12d3-a456-426614174000" in first
    assert "Tag Cloud:" in first
    header = first.splitlines()[0]
    assert re.search(r"## Shaping Log \u2014 .* \u2014 \d+ pts", header)
    assert "**Points:" in first
    assert "Top Concepts:" in first
    assert first.strip().endswith("---")

    cli_main(argv)
    second = log_file.read_text()
    assert second.count("## Shaping Log") == 2
    assert second.endswith(first)
    pts_first = int(re.search(r"(\d+) pts", first).group(1))
    pts_second = int(re.search(r"(\d+) pts", second).group(1))
    assert pts_second == pts_first
