import sys
from pathlib import Path
import types

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow_cli.breathing_willow import main as cli_main


def test_snip_file_cli(monkeypatch, tmp_path, capsys):
    test_file = tmp_path / "t.md"
    test_file.write_text("a b c d e")
    out_file = tmp_path / "o.md"

    class FakeEnc:
        def encode(self, text):
            return text.split()
        def decode(self, tokens):
            return " ".join(tokens)

    fake_tiktoken = types.SimpleNamespace(encoding_for_model=lambda m: FakeEnc())
    monkeypatch.setitem(sys.modules, "tiktoken", fake_tiktoken)

    from breathing_willow import snip_file as sf

    calls = {}

    def fake_snip(fp, context_scope="practical", aggressive=False, n_tokens="0"):
        calls["aggressive"] = aggressive
        p = Path(fp)
        tokens = FakeEnc().encode(p.read_text())
        return FakeEnc().decode(tokens[-2:])

    monkeypatch.setattr(sf, "snip_file_to_last_tokens", fake_snip)

    cli_main(["snip-file", "-f", str(test_file), "-o", str(out_file)])

    out = capsys.readouterr().out
    assert "has 5 tokens" in out
    assert "now has 2 tokens" in out
    assert calls["aggressive"] is False
    assert test_file.read_text() == "a b c d e"
    assert out_file.read_text() == "d e"
