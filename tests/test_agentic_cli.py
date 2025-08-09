import json
import sys
from pathlib import Path

import json
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow_cli.breathing_willow import main as cli_main


def _setup_registry(tmp_path, monkeypatch, data):
    home = tmp_path / "home"
    registry_dir = home / ".willow"
    registry_dir.mkdir(parents=True)
    registry_file = registry_dir / "agents.json"
    registry_file.write_text(json.dumps(data))
    monkeypatch.setattr(Path, "home", lambda: home)
    return registry_file


def test_load_clipboard_agent_success(monkeypatch, tmp_path, capsys):
    src = tmp_path / "source.md"
    src.write_text("hello")
    agent_name = "clipboard agent test cresting-cloud 85165e39"
    _setup_registry(tmp_path, monkeypatch, {agent_name: str(src)})

    out_file = tmp_path / "out.md"
    cli_main(["agentic", "--load-clipboard-agent", "85165e39", "-o", str(out_file)])

    assert out_file.read_text() == "hello"
    out = capsys.readouterr().out
    assert f"Loaded Clipboard Agent '{agent_name}'" in out
    assert f"Context written to {out_file}" in out


def test_load_clipboard_agent_no_match(monkeypatch, tmp_path):
    _setup_registry(tmp_path, monkeypatch, {"some agent 123": str(tmp_path / "a.md")})
    out_file = tmp_path / "o.md"
    with pytest.raises(SystemExit) as e:
        cli_main(["agentic", "--load-clipboard-agent", "zzz", "-o", str(out_file)])
    assert "no agent id matching" in str(e.value)


def test_load_clipboard_agent_multiple_matches(monkeypatch, tmp_path):
    src1 = tmp_path / "a.md"; src1.write_text("a")
    src2 = tmp_path / "b.md"; src2.write_text("b")
    _setup_registry(tmp_path, monkeypatch, {"agent 123": str(src1), "other 123": str(src2)})
    out_file = tmp_path / "o.md"
    with pytest.raises(SystemExit) as e:
        cli_main(["agentic", "--load-clipboard-agent", "123", "-o", str(out_file)])
    assert "multiple agents match" in str(e.value)
