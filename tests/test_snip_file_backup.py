import sys
from pathlib import Path
import types

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


def test_backup_and_missing_file(monkeypatch, tmp_path):
    class FakeEnc:
        def encode(self, text):
            return text.split()
        def decode(self, tokens):
            return " ".join(tokens)

    fake_tiktoken = types.SimpleNamespace(encoding_for_model=lambda m: FakeEnc())
    monkeypatch.setitem(sys.modules, "tiktoken", fake_tiktoken)

    from breathing_willow import snip_file as sf

    src = tmp_path / "f.txt"
    src.write_text("a b c d e", encoding="utf-8")

    out_text = sf.snip_file_to_last_tokens(src, aggressive=True, n_tokens="2")
    assert out_text == "d e"
    assert src.read_text(encoding="utf-8") == "d e"
    backup = src.with_suffix(src.suffix + ".bak")
    assert backup.exists()
    assert backup.read_text(encoding="utf-8") == "a b c d e"

    missing = tmp_path / "missing.txt"
    try:
        sf.snip_file_to_last_tokens(missing, aggressive=True)
    except FileNotFoundError as e:
        assert "file not found" in str(e)
    else:
        assert False, "expected FileNotFoundError"
