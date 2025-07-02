import json
import zipfile
from pathlib import Path

import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from breathing_willow.export_kernel import ChatExportArchiver


def make_conversation():
    return {
        "mapping": {
            "client-created-root": {
                "id": "client-created-root",
                "parent": None,
                "children": ["abc"],
            },
            "abc": {
                "id": "abc",
                "parent": "client-created-root",
                "children": ["def"],
                "message": {
                    "author": {"role": "user"},
                    "content": {"parts": ["hello"]},
                    "create_time": 1700000000,
                },
            },
            "def": {
                "id": "def",
                "parent": "abc",
                "children": [],
                "message": {
                    "author": {"role": "assistant"},
                    "content": {"parts": ["hi"]},
                },
            },
        }
    }


def test_archiver_conversations_json(tmp_path: Path):
    convo = [make_conversation()]
    convo_path = tmp_path / "conversations.json"
    convo_path.write_text(json.dumps(convo))

    zip_path = tmp_path / "exp.zip"
    with zipfile.ZipFile(zip_path, "w") as zf:
        zf.write(convo_path, arcname="conversations.json")

    out_dir = tmp_path / "out"
    arch = ChatExportArchiver(zip_path, out_dir)
    arch.run()

    files = list(out_dir.glob("*.md"))
    assert len(files) == 1
    text = files[0].read_text()
    assert "## zero:" in text
    assert "## tide:" in text

