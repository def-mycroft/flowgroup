import pathlib
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow_cli.breathing_willow import main as cli_main
from w_cli import diff


def test_sense_diff_writes_update(monkeypatch, tmp_path):
    calls = {}
    def fake_export(root: str, window: str = '24h', back: str | None = None) -> str:
        calls['root'] = root
        return 'log data'
    monkeypatch.setattr(diff, 'export_diff', fake_export)

    out_file = tmp_path / 'field-update.md'
    orig_write = Path.write_text
    def fake_write(self, text, *a, **kw):
        if str(self) == '/field/field-update.md':
            out_file.write_text(text)
        else:
            orig_write(self, text, *a, **kw)
    monkeypatch.setattr(Path, 'write_text', fake_write)

    cli_main(['sense', '--diff'])

    assert calls['root'] == '/field'
    assert out_file.read_text() == 'log data'
